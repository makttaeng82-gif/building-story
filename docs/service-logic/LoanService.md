# LoanService 설명

파일: `src/main/java/com/game/buildingstory/service/LoanService.java`

## 역할

대출 관련 계산과 상환을 담당한다.

담당 기능:

- 대출 목록 조회
- 남은 상환액 계산
- 남은 원금 계산
- 대출 한도 계산
- 직접 상환
- 월초 만기 상환 처리

## 주요 메서드

### `loans(Player player)`

플레이어의 대출 목록을 가져온다.

### `remainingRepayment(Player player)`

앞으로 갚아야 할 총 상환액을 계산한다.

### `remainingPrincipal(Player player)`

남은 원금 합계를 계산한다.

### `availableLoanLimit(Player player)`

현재 더 빌릴 수 있는 금액을 계산한다.

```java
loanLimit(player) - remainingPrincipal(player)
```

### `loanLimit(Player player)`

플레이어의 대출 한도를 계산한다.

현재 구조에서는 평판/자산 기준 계산의 중심 역할을 한다.

### `repayLoan(long playerId, long loanId)`

대출을 직접 상환한다.

흐름:

1. 플레이어 조회.
2. 일시정지 확인.
3. 대출 조회.
4. 소유자 확인.
5. 상환액만큼 현금 차감.
6. 대출 삭제 또는 원금 감소.
7. 기록 저장.

### `processMaturity(Player player)`

월초 정산에서 대출 월 상환을 처리한다.

## 주의점

- 건물 구매 서비스는 Loan을 생성하지만, 한도와 상환 계산은 LoanService가 담당한다.
- 직접 상환과 월초 자동 상환 모두 기록을 남겨야 한다.

