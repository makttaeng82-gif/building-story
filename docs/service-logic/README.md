# 서비스 로직 설명 파일 목록

이 폴더는 프로젝트의 주요 서비스 로직을 초급 개발자가 코드와 함께 공부할 수 있게 정리한 문서다.

## 읽는 순서

1. [GameService](./GameService.md)
2. [DailyGameOrchestrator](./DailyGameOrchestrator.md)
3. [SettlementService](./SettlementService.md)
4. [BuildingTradeService](./BuildingTradeService.md)
5. [StockService](./StockService.md)
6. [SecretaryTenantEventService](./SecretaryTenantEventService.md)
7. [SecretaryOperationsService](./SecretaryOperationsService.md)
8. [AuctionService](./AuctionService.md)
9. [EventFlowService](./EventFlowService.md)
10. [ShopService](./ShopService.md)
11. [LoanService](./LoanService.md)

## 공통 구조

```text
Controller
  -> GameService
  -> 전용 Service
  -> Repository
  -> Entity
```

- Controller: HTTP 요청을 받는다.
- GameService: 게임 전체 기능의 입구 역할을 한다.
- 전용 Service: 주식, 부동산, 비서, 경매처럼 특정 기능의 규칙을 처리한다.
- Repository: DB 조회/저장을 담당한다.
- Entity: DB에 저장되는 실제 게임 상태다.
