# SecretaryTenantEventService 설명

파일: `src/main/java/com/game/buildingstory/service/SecretaryTenantEventService.java`

## 역할

비서가 임차인으로 등장하고, 요청을 해결한 뒤 고용 가능해지는 흐름을 담당한다.

전체 흐름:

```text
특정 건물 구매
  -> 비서 임차인 등장 가능 확인
  -> intro 이벤트
  -> tenant 상태
  -> 요청 조건 만족
  -> request 이벤트
  -> hire available 상태
  -> 고용 조건 만족
  -> hire 이벤트
  -> OwnedSecretary 생성
  -> 이벤트 완료
```

## 주요 상수

```java
static final String INTRO_EFFECT_PREFIX = "SECRETARY_TENANT_INTRO:";
```

이벤트 effect 문자열 prefix다.

예:

```text
SECRETARY_TENANT_HIRE:secretary-6
```

이 문자열을 보고 이벤트 완료 시 어떤 비서를 처리할지 판단한다.

## 주요 메서드

### `evaluate(Player player, boolean hasActiveAuction)`

비서 임차인 이벤트 전체 흐름을 평가한다.

보통 하루 진행이나 화면 렌더링 전에 호출된다.

### `tryActivateIntro(Player player, OwnedBuilding building)`

새로 산 건물이 특정 비서 등장 조건에 맞으면 intro 이벤트를 만든다.

조건:

- 시나리오의 도시/건물 슬롯과 일치.
- 아직 같은 비서 이벤트가 없음.
- 활성 이벤트가 없음.

### `applyFirstTenantMoveIn(Player player)`

첫 임차인 이벤트를 실제 건물 입주 상태로 반영한다.

### `applyIntroEvent(Player player, String secretaryKey)`

intro 이벤트 완료 후 비서를 임차인 상태로 만든다.

### `applyRequestEvent(Player player, String secretaryKey)`

비서 요청 이벤트 완료 처리다.

흐름:

1. 이벤트 조회.
2. 시나리오 조회.
3. 필요 비용/조건 검증.
4. 비용 차감.
5. 이벤트를 고용 가능 상태로 변경.
6. 기록 저장.

### `applyHireEvent(Player player, String secretaryKey)`

비서 고용 이벤트 완료 처리다.

핵심:

```java
if (ownedSecretaryRepository.findByPlayerAndSecretaryKey(player, secretaryKey).isEmpty()) {
    ownedSecretaryRepository.save(new OwnedSecretary(player, secretaryKey, spec.baseProficiency()));
}
```

이미 보유하지 않은 비서만 새로 저장한다.

```java
if ("secretary-6".equals(secretaryKey)) {
```

6번 비서는 서울 4번 건물로 거주지를 옮기는 특수 처리다.

### `createTenantEvent(...)`

비서 임차인 이벤트 엔티티를 처음 만든다.

중복 생성 방지:

```java
if (secretaryTenantEventRepository.findByPlayerAndSecretaryKey(...).isPresent()) {
    return;
}
```

### `activateNextEvent(Player player)`

현재 조건에서 띄울 다음 비서 이벤트를 찾는다.

중요 원칙:

- 이미 활성 이벤트가 있으면 아무 것도 하지 않는다.
- 한 번에 이벤트 하나만 띄운다.
- 상태가 `TENANT`이고 요청 조건을 만족하면 `REQUEST_AVAILABLE`.
- 상태가 `HIRE_AVAILABLE`이고 고용 조건을 만족하면 고용 이벤트 생성.

### `statusText(OwnedBuilding building)`

건물에 연결된 비서 임차인 이벤트 상태를 화면 문구로 변환한다.

### `isRentWaived(Player player, OwnedBuilding building)`

비서 임차인 요청 처리 결과로 월세 감면 상태인지 확인한다.

## 주의점

- 이벤트 모달은 한 번에 하나만 활성화해야 한다.
- `SecretaryTenantScenarioCatalog`는 원본 조건 데이터다.
- 실제 진행 상태는 `SecretaryTenantEvent` 엔티티에 저장된다.
- effect 문자열 prefix와 처리 로직이 서로 맞아야 한다.

