# SecretaryOperationsService 설명

파일: `src/main/java/com/game/buildingstory/service/SecretaryOperationsService.java`

## 역할

고용된 비서의 운영 기능을 담당한다.

담당 기능:

- 첫 비서 고용
- 일반 비서 고용
- 도시 배치/해제
- 자동 수리
- 월간 평판 보너스
- 비서 월급
- 비서 특수 효과 계산
- 퇴거 방어
- 월세 보너스
- 건물 대기시간 감소

## 주요 메서드

### `hireFirstSecretary(long playerId)`

첫 비서를 고용한다.

조건:

- 게임이 일시정지 상태가 아님.
- 아직 첫 비서를 고용하지 않음.
- 평판 1000 이상.
- 청주 보호 임차인이 존재.

성공 시:

- 보호 임차인 퇴거.
- 첫 비서 고용 상태 저장.
- `OwnedSecretary` 생성.

### `canHireSecretary(Player player, SecretarySpec spec)`

비서 고용 가능 여부를 계산한다.

주로 화면에서 버튼 표시/잠금 상태에 사용된다.

### `hireSecretary(long playerId, String secretaryKey)`

카탈로그의 비서 스펙을 보고 실제 보유 비서를 생성한다.

### `assignSecretary(long playerId, long ownedSecretaryId, String city)`

비서를 도시에 배치한다.

중요 규칙:

- 같은 도시에 다른 비서가 이미 있으면 배치 불가.
- 비서 효과는 배치된 도시에서만 적용된다.

### `unassignSecretary(...)`

비서 배치를 해제한다.

### `processAutoRepairs(Player player)`

비서 자동 수리를 처리한다.

흐름:

1. 배치된 비서 조회.
2. 담당 도시의 수리 요청 건물 조회.
3. 비서 숙련도에 따른 관리 가능 건물 수 계산.
4. 자동 수리 쿨다운 확인.
5. 수리비 차감.
6. 수리 완료.
7. 숙련도 경험치 증가.

### `processMonthlyReputation(Player player)`

월초에 비서가 평판 보너스를 준다.

### `processSalaries(Player player)`

고용된 비서들의 월급을 차감한다.

### `defendMoveOut(Player player, OwnedBuilding building)`

퇴거 이벤트가 발생했을 때 비서가 막을 수 있는지 계산한다.

### `rentBonusPercent(Player player, String city)`

배치된 비서가 주는 월세 증가율을 계산한다.

### `buildingWaitReductionPercent(Player player, String city)`

건물 거래 대기시간 감소율을 계산한다.

## 문법 포인트

```java
ownedSecretaryRepository.findByPlayer(player).stream()
```

DB에서 가져온 리스트를 stream으로 처리한다.

```java
.filter(secretary -> city.equals(secretary.getAssignedCity()))
```

특정 도시에 배치된 비서만 필터링한다.

## 주의점

- 비서 효과는 배치 도시 기준이다.
- 숙련도와 호감도는 다른 성장 축이다.
- 자동 수리는 쿨다운과 관리 건물 수 제한을 함께 봐야 한다.

