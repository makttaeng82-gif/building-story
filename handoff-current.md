# Current Handoff

## Project

- Path: `D:\gameproject`
- App: Spring Boot building management game
- Server: `http://localhost:8080`
- Run: `.\gradlew.bat bootRun`
- Test: `.\gradlew.bat test --console=plain`
- Legacy handoff: `handoff-summary.txt` is large and partially mojibake. Do not read it by default.

## Handoff File Policy

- At the start of a new chat or task, read `handoff-current.md` first.
- Encoding rule is mandatory: all Markdown handoff files must be read and written as UTF-8.
- Do not rely on PowerShell default encoding or console code page. Windows may use CP949 and Korean text can appear as mojibake even when the file is valid UTF-8.
- If Korean text looks broken, first re-read the file with explicit UTF-8 before assuming the file content is damaged.
  - PowerShell read: `Get-Content -Raw -Encoding UTF8 -LiteralPath "handoff-current.md"`
  - PowerShell write: `Set-Content -Encoding UTF8 -LiteralPath "handoff-current.md" -Value $text`
- At the end of work, keep `handoff-current.md` short and update only the current state.
- Append detailed work history to `handoff-history.md`.
- Check `handoff-history.md` or `git log` only when older reasoning or past details are needed.
- Do not use `handoff-summary.txt` as the default handoff source.

## Current Rule Notes

- Do not overwrite unrelated dirty worktree changes.
- During development, consider extensibility and maintainability before implementation.
  - Avoid growing already-large files when a responsibility can be cleanly separated.
  - Prefer existing project patterns over one-off solutions.
  - Keep behavior changes and refactoring changes clearly separated when possible.
- Main screen time advances through `/tick`.
- Info screen does not advance time.
- Event and auction modals pause normal play.
- Active test server was restarted on port 8080 after city/secretary panel fragment extraction. PID: `9928`.
- Next planned work: code refactoring to reduce lookup time.

## Recent Implemented Systems

- Auction chance is 3%, auction auto-cancel timer is 20 seconds.
- Gift shop exists with 5 gift items.
- Gifts give affinity experience +1 and are gated by affinity range.
- Secretary event chain exists:
  - tenant intro
  - request
  - request accepted or waiting
  - hire event
  - completed
- Secretary event images are stored under `src/main/resources/static/assets/events/`.
- Building images are used in owned building and market cards.
- Secretary special effects are reflected in final city chance text.
- QA tools exist on the main test panel for secretary event setup/stage.

## Latest Changes

- Fixed QA/test helper persistence after refactor.
  - Root cause: `QaService` was missing transactional boundaries after `/test/*` endpoints were moved from `GameService`.
  - Added `@Transactional` to `QaService`.
  - Added regression test `qaCashChangePersistsWithoutCallerTransaction`.
- Started code refactoring to reduce `GameService` size.
- Extracted secretary tenant scenario data into:
  - `SecretaryTenantScenario`
  - `SecretaryTenantScenarioCatalog`
- Extracted secretary tenant event flow into:
  - `SecretaryTenantEventService`
- Extracted display formatting into:
  - `GameTextFormatter`
- Extracted shop behavior into:
  - `ShopService`
- Extracted auction behavior into:
  - `AuctionService`
- Extracted QA/test helper behavior into:
  - `QaService`
- Extracted building trade/market behavior into:
  - `BuildingTradeService`
- Extracted loan behavior into:
  - `LoanService`
- Extracted secretary operations behavior into:
  - `SecretaryOperationsService`
- Extracted monthly settlement and random building event behavior into:
  - `SettlementService`
- Extracted event completion/cancel and resignation behavior into:
  - `EventFlowService`
- `GameService.java` was reduced from 2019 lines to 585 lines.
- Split main page modal markup into:
  - `fragments/main-modals.html`
- Split main page market markup into:
  - `fragments/main-market.html`
- Split main page QA/test panel markup into:
  - `fragments/main-test-panel.html`
- Split main page info-grid markup into:
  - `fragments/main-info-grid.html`
- Split main page city panel markup into:
  - `fragments/main-city-panel.html`
- Split main page secretary panel markup into:
  - `fragments/main-secretary-panel.html`
- `main.html` was reduced from 693 lines to 105 lines.
- `fragments/main-info-grid.html` is 152 lines.
- `fragments/main-city-panel.html` is 48 lines.
- `fragments/main-secretary-panel.html` is 65 lines.
- Fixed market refresh behavior:
  - Buying a building no longer deletes its offer.
  - `ensureOffers` no longer refreshes just because the city has fewer offers than the catalog.
  - Purchased offers remain visible with purchase cooldown until the scheduled 5-day refresh.
- Fixed purchase confirmation behavior:
  - Donation forms now use the shared confirm modal.
  - Donation, luxury, and gift purchase confirm modal title is `구매 하시겠습니까?`.
  - Confirm modal display now pauses day progression through `game-paused`.
- Behavior intent: no feature changes, only responsibility split.
- Cleaned 33 root-level `tmp-*` files. `server-8080.log` and `server-8080.err.log` were recreated by the active server.

## Secretary Event Conditions For Display

- 설아름
  - 입주조건: 1월 3일 청주 첫 건물 입주 이벤트
  - 고용조건: 현금 1억 최초 달성 후 월세 2달 감면 수락, 2달 경과
- 설하은
  - 입주조건: 세종 도담동 24평형 아파트 첫 구매
  - 고용조건: 평판 1만5500 이상, 현금 3억 이상, 부탁 수락 후 1억원 지급
- 이다은
  - 입주조건: 대전 봉명동 상가주택 첫 구매
  - 고용조건: 평판 5만500 이상, 그렌져 보유, 부탁 수락 후 30일 경과
- 한아리
  - 입주조건: 부산 센텀시티 첫 구매
  - 고용조건: 평판 45만5000 이상, 현금 150억 이상, 부탁 수락 후 90억 지급
- 김채린
  - 입주조건: 인천 송도 센트럴파크 첫 구매
  - 고용조건: 평판 131만 이상, 현금 2000억 이상, 대출 없음, 부탁 수락 후 1000억 지급
- 신수아
  - 입주조건: 서울 트리플렛 타워 첫 구매
  - 고용조건: 모든 사치품 보유, 현금 10조 이상, 반포 자이맹 리 구매

## Files Changed In Latest Turn

- `src/main/java/com/game/buildingstory/service/GameService.java`
- `src/main/java/com/game/buildingstory/service/GameTextFormatter.java`
- `src/main/java/com/game/buildingstory/service/SecretaryTenantEventService.java`
- `src/main/java/com/game/buildingstory/service/SecretaryTenantScenario.java`
- `src/main/java/com/game/buildingstory/service/SecretaryTenantScenarioCatalog.java`
- `src/main/java/com/game/buildingstory/service/ShopService.java`
- `src/main/java/com/game/buildingstory/service/AuctionService.java`
- `src/main/java/com/game/buildingstory/service/QaService.java`
- `src/main/java/com/game/buildingstory/service/BuildingTradeService.java`
- `src/main/java/com/game/buildingstory/service/LoanService.java`
- `src/main/java/com/game/buildingstory/service/SecretaryOperationsService.java`
- `src/main/java/com/game/buildingstory/service/SettlementService.java`
- `src/main/java/com/game/buildingstory/service/EventFlowService.java`
- `src/main/java/com/game/buildingstory/repo/PurchaseCooldownRepository.java`
- `src/main/resources/templates/fragments/main-modals.html`
- `src/main/resources/templates/fragments/main-market.html`
- `src/main/resources/templates/fragments/main-test-panel.html`
- `src/main/resources/templates/fragments/main-info-grid.html`
- `src/main/resources/templates/fragments/main-city-panel.html`
- `src/main/resources/templates/fragments/main-secretary-panel.html`
- `src/main/resources/static/app.js`
- `src/main/resources/templates/main.html`
- `src/main/java/com/game/buildingstory/web/GameController.java`
- `src/test/java/com/game/buildingstory/BuildingStoryApplicationTests.java`
- `handoff-current.md`
- `handoff-history.md`

## Verification Status

- Baseline test before refactor: `.\gradlew.bat test --console=plain` passed.
- Test after scenario extraction: `.\gradlew.bat test --console=plain` passed.
- Test after display formatter extraction: `.\gradlew.bat test --console=plain` passed.
- Test after secretary tenant event service extraction: `.\gradlew.bat test --console=plain` passed.
- Test after shop service extraction: `.\gradlew.bat test --console=plain` passed.
- Test after auction service extraction: `.\gradlew.bat test --console=plain` passed.
- Test after QA service extraction: `.\gradlew.bat test --console=plain` passed.
- Test after QA transaction fix: `.\gradlew.bat test --console=plain` passed.
- Test after building trade service extraction: `.\gradlew.bat test --console=plain` passed.
- Test after loan service extraction: `.\gradlew.bat test --console=plain` passed.
- Test after secretary operations service extraction: `.\gradlew.bat test --console=plain` passed.
- Test after settlement service extraction: `.\gradlew.bat test --console=plain` passed.
- Test after event flow service extraction: `.\gradlew.bat test --console=plain` passed.
- Test after main modal fragment extraction: `.\gradlew.bat test --console=plain` passed.
- Test after main market fragment extraction: `.\gradlew.bat test --console=plain` passed.
- Test after main test panel fragment extraction: `.\gradlew.bat test --console=plain` passed.
- Test after market refresh and confirm modal fixes: `.\gradlew.bat test --console=plain` passed.
- Test after main info-grid fragment extraction: `.\gradlew.bat test --console=plain` passed.
- Test after main city/secretary panel fragment extraction: `.\gradlew.bat test --console=plain` passed.
- Added regression test `buyingOfferDoesNotRefreshMarketBeforeRefreshDay`.
- Browser render check after fragment extraction passed:
  - `/login` loaded.
  - Test user registered.
  - `/story` completed.
  - `/main` rendered with `#secretaryModal`, `#confirmModal`, and market content present.
- HTTP render check after market fragment extraction passed:
  - `/main` rendered with `.market-panel`, `.market-card`, `#secretaryModal`, and `#confirmModal`.
- HTTP render check after test panel fragment extraction passed:
  - `/main` rendered with `.test-panel`, `/test/cash`, `/test/secretary-event/stage`, and no template error.
- HTTP flow check after market refresh fix passed:
  - Buying one offer kept market card count at 4.
  - Purchased offer showed cooldown text.
  - Donation confirm attributes rendered.
  - `data-player-paused` rendered.
- HTTP render check after info-grid fragment extraction passed:
  - `/main` rendered with `.info-grid`, `.building-detail`, `.loan-box`, donation confirm attributes, and `.test-panel`.
  - No template error.
- HTTP render check after city/secretary panel fragment extraction passed:
  - `/main` rendered with `.layout`, `.city-panel`, `.skyline`, `.secretary-panel`, `.secretary-focus`, `.market-panel`, and `.info-grid`.
  - No template error.
- Manual QA cash POST verified: cash changed from `0원` to `3000만원`.
- Server restarted successfully on `http://localhost:8080`. PID: `9928`.
- Mojibake pattern check against service files and handoff files found no matches.

## Next Planned Work

- Continue refactoring before adding features.
- Recommended next targets:
  - Split remaining `main.html` header/navigation/toast shell only if useful.
  - Reduce `GameService` remaining read/display delegation methods if useful.
