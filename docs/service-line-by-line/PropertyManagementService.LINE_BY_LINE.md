# PropertyManagementService 코드 줄단위 해설

원본 파일: `src/main/java/com/game/buildingstory/service/PropertyManagementService.java`

아래 내용은 import 문을 제외한 서비스 코드를 실행 흐름에 따라 설명한다. 단순 상수와 getter는 반복 설명하지 않는다.

## 클래스와 의존성

```java
@Service
@Transactional
public class PropertyManagementService {
```

`@Service`는 이 클래스를 Spring이 관리하는 서비스 객체로 등록한다. `@Transactional`은 공개 메서드가 실행되는 동안 조회와 변경을 하나의 DB 작업으로 묶는다. 중간에 예외가 발생하면 현금만 빠지고 수리 상태는 남는 식의 부분 저장을 막는다.

```java
    private final PlayerRepository playerRepository;
    private final OwnedPropertyManagerRepository propertyManagerRepository;
    private final OwnedBuildingRepository ownedBuildingRepository;
    private final MonthlyRecordRepository monthlyRecordRepository;
    private final BuildingCatalog buildingCatalog;
    private final ReputationCatalog reputationCatalog;
```

서비스가 직접 SQL을 작성하지 않고 Repository를 통해 플레이어, 관리직원, 건물과 최근 기록을 읽고 쓴다. Catalog는 유효한 도시 검사와 평판 칭호 갱신에 사용한다.

```java
    public PropertyManagementService(
            PlayerRepository playerRepository,
            OwnedPropertyManagerRepository propertyManagerRepository,
            OwnedBuildingRepository ownedBuildingRepository,
            MonthlyRecordRepository monthlyRecordRepository,
            BuildingCatalog buildingCatalog,
            ReputationCatalog reputationCatalog
    ) {
```

생성자 주입이다. Spring이 필요한 구현 객체를 전달하며, 필드가 `final`이므로 생성 이후 다른 객체로 바뀌지 않는다.

## 채용

```java
    public String hire(long playerId, String city) {
        Player player = playerRepository.findById(playerId).orElseThrow();
```

브라우저가 보낸 ID만 믿고 처리하지 않고 DB에서 실제 플레이어를 조회한다. 없는 ID면 예외를 발생시켜 잘못된 요청이 저장되지 않게 한다.

```java
        if (player.isPaused()) {
            return "일시정지 중에는 관리직원을 채용할 수 없음";
        }
```

일시정지 중 경제행동 금지 규칙을 관리직원 채용에도 동일하게 적용한다.

```java
        if (!buildingCatalog.cities().contains(city)) {
            return "존재하지 않는 도시";
        }
```

요청 문자열이 게임에 실제로 존재하는 도시인지 검사한다. 이 검사가 없으면 임의 문자열로 관리직원 데이터가 만들어질 수 있다.

```java
        if (propertyManagerRepository.findByPlayerAndCity(player, city).isPresent()) {
            return city + " 관리직원은 이미 채용됨";
        }
```

같은 플레이어가 같은 도시에 두 명을 채용하는 것을 서비스 단계에서 먼저 차단한다. 엔티티의 유일성 제약은 동시 요청까지 막는 최종 안전장치다.

```java
        var assignedSecretaries = ownedSecretaryRepository.findByPlayerAndAssignedCityOrderById(player, city);
        int unassignedSecretaryCount = assignedSecretaries.size();
        assignedSecretaries.forEach(secretary -> secretary.assignTo(null));
        propertyManagerRepository.save(new OwnedPropertyManager(player, city));
        String memo = unassignedSecretaryCount == 0
                ? "자동수리 인계 완료"
                : "자동수리 인계 완료 · 비서 배치 해제 " + unassignedSecretaryCount + "명";
        saveRecord(player, "부동산 관리직원 채용", null, 0, city, memo);
        return city + " 부동산 관리직원 채용 완료"
                + (unassignedSecretaryCount == 0 ? "" : " · 비서 배치 해제");
    }
```

먼저 해당 도시에 배치된 비서를 모두 조회해 배치를 해제한다. 그 뒤 관리직원을 `ACTIVE` 상태로 생성하므로 한 도시의 관리 주체가 비서와 직원으로 중복되지 않는다. 해제된 비서 수는 기록과 토스트에 남긴다.

## 조회와 인계 도시

```java
    @Transactional(readOnly = true)
    public Optional<OwnedPropertyManager> manager(Player player, String city) {
        return propertyManagerRepository.findByPlayerAndCity(player, city);
    }
```

도시 화면이 현재 도시의 관리직원 존재 여부를 확인한다. `Optional`은 미채용 상태를 `null` 대신 명시적으로 표현한다.

```java
    @Transactional(readOnly = true)
    public Set<String> managedCities(Player player) {
        Set<String> cities = new LinkedHashSet<>();
        propertyManagerRepository.findByPlayerOrderById(player)
                .forEach(manager -> cities.add(manager.getCity()));
        return Set.copyOf(cities);
    }
```

이미 관리직원에게 인계된 도시 집합을 만든다. `Set`은 포함 여부 검사가 빠르고 중복 도시를 허용하지 않는다. 이 목록은 급여중단 상태도 포함한다. 그렇지 않으면 체불 시 비서가 대신 수리해 급여 패널티가 사라지기 때문이다.

```java
    public long salaryDue(OwnedPropertyManager manager) {
        return Math.multiplyExact(MONTHLY_SALARY, manager.getUnpaidSalaryMonths() + 1L);
    }
```

다음 급여일에 낼 금액은 `기본월급 × (체불개월 + 이번 달)`이다. `multiplyExact`는 금액이 `long` 범위를 넘을 때 조용히 잘못된 음수로 바뀌지 않고 예외를 발생시킨다.

## 급여 정산

```java
    public String processSalaries(Player player) {
        List<OwnedPropertyManager> managers = propertyManagerRepository.findByPlayerOrderById(player);
        if (managers.isEmpty()) {
            return "";
        }
```

매월 15일 호출되는 급여 처리다. 관리직원이 없으면 기록과 알림을 만들지 않는다.

```java
        long totalDue = managers.stream().mapToLong(this::salaryDue).sum();
```

각 직원의 당월 급여와 체불액을 모두 합산한다. 도시별로 순서대로 결제하면 앞 도시만 지급되는 문제가 생기므로 먼저 전체 금액을 계산한다.

```java
        if (player.spendCash(totalDue)) {
            managers.forEach(OwnedPropertyManager::recordSalaryPaid);
```

전체 금액을 한 번에 낼 수 있을 때만 현금을 차감한다. 성공하면 모든 직원의 체불개월을 0으로 만들고 자동수리를 재개한다.

```java
            saveRecord(player, "관리직원 월급", -totalDue, 0, null,
                    managers.size() + "명 · 자동수리 정상");
            return "관리직원 월급 지급 " + managers.size() + "명";
        }
```

지출은 음수 금액으로 기록한다. 직원별 동일 기록을 여러 개 만들지 않고 한 번의 급여 사건으로 합쳐 최근 기록이 불필요하게 길어지는 것을 막는다.

```java
        managers.forEach(OwnedPropertyManager::recordUnpaidSalary);
        saveRecord(player, "관리직원 월급 미지급", null, 0, null,
                managers.size() + "명 · 자동수리 중단");
        return "관리직원 월급 미지급 · 자동수리 중단";
    }
```

전체 금액이 부족하면 아무 도시에도 지급하지 않는다. 모든 직원의 체불개월을 함께 증가시키고 상태를 `SALARY_SUSPENDED`로 바꾼다.

## 자동수리

```java
    public String processAutoRepairs(Player player) {
        String notice = "";
        for (OwnedPropertyManager manager : propertyManagerRepository.findByPlayerOrderById(player)) {
            if (!manager.isActive()) {
                continue;
            }
```

고용된 관리직원을 순서대로 확인한다. 급여가 중단된 직원은 데이터가 삭제되지는 않지만 수리를 실행하지 않는다.

```java
            List<OwnedBuilding> targets = ownedBuildingRepository.findByPlayerAndCityOrderById(player, manager.getCity()).stream()
                    .filter(OwnedBuilding::isRepairRequested)
                    .sorted(Comparator.comparingInt(OwnedBuilding::getRepairNeglectedMonths).reversed()
                            .thenComparing(OwnedBuilding::getId))
                    .toList();
```

담당 도시의 수리요청만 남긴 뒤 방치개월 내림차순, 건물 ID 오름차순으로 정렬한다. 따라서 오래 방치된 건물이 먼저 처리되고 결과가 매번 같은 순서로 재현된다.

```java
            int repairedCount = 0;
            boolean cashShortage = false;
            for (OwnedBuilding building : targets) {
                long repairCost = building.repairCost();
```

한 도시에서 처리한 건수를 세고 현금부족 여부를 별도로 기억한다. 관리직원 할인은 없으므로 건물이 계산한 정상 수리비를 그대로 사용한다.

```java
                if (!player.spendCash(repairCost)) {
                    cashShortage = true;
                    break;
                }
```

현재 현금으로 다음 건물을 수리할 수 없으면 중단한다. 이후 건물도 우선순위가 더 낮으므로 건너뛰어 수리하지 않는다.

```java
                boolean repairedWithinOneMonth = building.repair();
                int reputationChange = repairedWithinOneMonth ? REPAIR_REPUTATION_REWARD : 0;
                if (reputationChange > 0) {
                    player.addReputation(reputationChange);
                }
```

`repair()`는 수리요청을 제거하면서 한 달 안에 처리했는지도 반환한다. 빠른 수리일 때만 기존 기본 평판 3을 지급한다.

```java
                saveRecord(player, "관리직원 수리", -repairCost, reputationChange,
                        building.getName(), "부동산 관리직원");
                repairedCount++;
            }
```

건물마다 비용과 평판 변화를 기록한다. 비서 수리와 구분하기 위해 메모에 처리 주체를 남긴다.

```java
            if (repairedCount > 0) {
                notice = appendNotice(notice, manager.getCity() + " 관리직원 자동수리 " + repairedCount + "건");
            }
            if (cashShortage) {
                notice = appendNotice(notice, manager.getCity() + " 관리직원 수리비 부족");
            }
```

DB 기록은 건물별로 남기지만 토스트는 도시별 처리 건수로 합친다. 현금부족 경고도 같은 일일 처리에서 도시당 한 번만 추가된다.

```java
        if (!notice.isBlank()) {
            player.updateTitle(reputationCatalog.currentTier(player.getReputation(), !player.isEmployed()).title());
        }
        return notice;
    }
```

수리로 평판이 바뀌었을 수 있으므로 최종 칭호를 다시 계산한다. 아무 처리도 없으면 불필요한 계산을 하지 않는다.

## 기록과 알림 결합

```java
    private void saveRecord(Player player, String title, Long amount, int reputationChange, String buildingName, String memo) {
        monthlyRecordRepository.save(new MonthlyRecord(
                player, RecordType.PROPERTY_MANAGER, title, amount, reputationChange, buildingName, memo));
```

채용, 급여와 수리를 전용 기록 유형으로 저장한다. 화면 필터나 향후 통계를 비서 기록과 독립적으로 확장할 수 있다.

```java
        monthlyRecordRepository.deleteByPlayerAndElapsedDaysLessThan(
                player, Math.max(0, player.getElapsedDays() - RECORD_RETENTION_DAYS));
    }
```

최근 기록 보존기간보다 오래된 행을 삭제해 게임 진행이 길어져도 기록 테이블이 계속 커지지 않게 한다.

```java
    private String appendNotice(String base, String addition) {
        if (addition == null || addition.isBlank()) {
            return base;
        }
        return base == null || base.isBlank() ? addition : base + " · " + addition;
    }
```

여러 도시의 결과를 하나의 토스트 문장으로 합친다. 빈 문자열을 먼저 검사해 문장 앞뒤에 불필요한 구분점이 생기지 않게 한다.
