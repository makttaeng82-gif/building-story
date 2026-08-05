# PlayerCompanyService 코드 줄단위 해설

원본 파일: `src/main/java/com/game/buildingstory/service/PlayerCompanyService.java`

기업 설립 시 개인 현금, 법인 자금, 지분과 비서 배치가 어떻게 한 번에 변경되는지 설명한다. 단순 상수와 getter는 반복 설명하지 않는다.

## 클래스와 의존성

```java
@Service
public class PlayerCompanyService {
```

`@Service`로 등록된 기업 전용 서비스다. 컨트롤러는 입력만 전달하고 설립 규칙과 저장 순서는 이 클래스가 책임진다.

```java
private final PlayerRepository playerRepository;
private final PlayerCompanyRepository playerCompanyRepository;
private final OwnedSecretaryRepository ownedSecretaryRepository;
private final MonthlyRecordRepository monthlyRecordRepository;
private final StockCatalog stockCatalog;
```

플레이어 잠금, 법인 저장, 비서 배치 해제, 최근 기록 저장과 기존 상장기업명 중복 검사를 각각 전용 저장소와 카탈로그에 맡긴다.

## 법인 조회

```java
@Transactional(readOnly = true)
public Optional<PlayerCompany> company(Player player) {
    return playerCompanyRepository.findByPlayer(player);
}
```

플레이어는 법인을 최대 하나만 소유한다. 설립 전에는 빈 `Optional`, 설립 후에는 저장된 법인을 반환한다.

## 설립 트랜잭션

```java
@Transactional
public String establish(long playerId, String requestedCompanyName, String requestedServiceName, long investment) {
    Player player = playerRepository.findByIdForUpdate(playerId).orElseThrow();
```

`@Transactional`은 이후 현금·법인·비서 변경을 한 작업으로 묶는다. `findByIdForUpdate`는 같은 플레이어의 동시 설립 요청을 직렬화해 두 요청이 같은 현금을 동시에 읽지 못하게 한다.

```java
Optional<PlayerCompany> existingCompany = playerCompanyRepository.findByPlayer(player);
if (existingCompany.isPresent()) {
    return "이미 설립된 기업이 있음";
}
```

플레이어 행 잠금 뒤 기존 법인을 다시 확인한다. 첫 요청이 설립을 끝냈다면 뒤 요청은 아무 자금도 차감하지 않고 종료한다.

```java
if (player.isPaused()) {
    return "일시정지 중에는 기업을 설립할 수 없음";
}
```

게임 일시정지 중 경제행동을 금지하는 공통 규칙을 적용한다.

```java
String companyName = normalizeName(requestedCompanyName);
String serviceName = normalizeName(requestedServiceName);
String validationError = validateNames(companyName, serviceName);
```

앞뒤 공백을 제거한 뒤 길이, 제어문자와 기존 상장기업명 중복을 검사한다. 브라우저 검증을 우회한 직접 요청도 서버에서 거부된다.

```java
if (investment < MINIMUM_INVESTMENT) {
    return "최소 출자금은 1,500억원";
}
if (investment > player.getCash()) {
    return "출자금이 개인 현금을 초과함";
}
if (!player.spendCash(investment)) {
    return "출자금 부족";
}
```

최소 출자금과 실제 개인 현금을 확인한 후에만 차감한다. 화면의 예상 계산은 안내일 뿐이며 최종 기준은 항상 이 서버 검사다.

```java
long initialCorporateCash = Math.subtractExact(investment, SECRETARY_TRAINING_COST);
PlayerCompany company = new PlayerCompany(
        player, companyName, serviceName, investment, initialCorporateCash,
        INITIAL_ISSUED_SHARES, player.getElapsedDays()
);
playerCompanyRepository.save(company);
```

법인현금은 출자금에서 일회성 비서교육비 3억원을 뺀 값으로 시작한다. 최초 발행주식 1,000만주는 모두 플레이어에게 배정되며 설립일도 함께 저장한다.

```java
ownedSecretaryRepository.findByPlayerOrderById(player)
        .forEach(secretary -> secretary.assignTo(null));
```

보유 비서의 도시 배치를 모두 해제한다. 숙련도와 호감도는 변경하지 않으므로 이후 기업 역할에 그대로 활용할 수 있다.

```java
monthlyRecordRepository.save(new MonthlyRecord(...));
```

개인 현금이 줄어든 이유와 교육비·지분 결과를 최근 기록에 남긴다. 이 저장까지 실패하면 트랜잭션 전체가 취소되어 앞선 현금과 비서 상태도 원상복구된다.

## 개발 중 우회 범위

비서 성장과 부동산 관리직원 배치는 현재 설립 차단 조건에서 제외되어 있다. 최소 출자금, 현금 부족, 이름 검증과 중복 설립은 데이터 정합성 규칙이므로 개발 중에도 유지한다.

설립 후 비서 월급은 `SecretaryOperationsService`가 법인 존재 여부를 확인해 법인현금에서 전원분을 한 번에 지급한다. 잔액이 부족하면 지급 순서에 따라 일부 비서만 월급을 받지 않도록 전원 미지급 처리한다.
