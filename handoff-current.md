# Current Handoff

## Project

- Path: `D:\codex\gameproject`
- App: Spring Boot building management game
- Server URL: `http://localhost:8080`
- Run: `.\gradlew.bat bootRun`
- Test: `.\gradlew.bat test --console=plain`
- Account rule: username 2-20 chars, password at least 4 chars.
- Start new work by reading this file first.
- Read/write Markdown handoff files as UTF-8.
- Do not use `handoff-summary.txt` by default; it is large and partially mojibake.

## Current State

- Current branch: `codex/refactor-building-story`
- Latest pushed commit: `64456f4 add stock market systems and events`
- Remote: `origin/codex/refactor-building-story`
- Working tree was clean immediately after push.
- Server is stopped. Port 8080 has no listener.
- Last verification before push: `.\gradlew.bat test --console=plain` passed.

## Recent GitHub Push

- Pushed to GitHub:
  - Branch: `codex/refactor-building-story`
  - Commit: `64456f4`
  - Message: `add stock market systems and events`
- `gh` CLI is not installed in this environment, so no PR was created.

## Recent Stock Work

- Stock content unlocks after Seoul unlock schedule.
- Stock screen exists as `/main?view=stocks`.
- Stock screen shares main game time flow.
- Stock screen sends `/tick?view=stocks`.
- City event/auction reload signals are deferred while viewing stocks.
- City events can be created in background and shown after returning to city view.
- Stock market initializes 15 companies:
  - IT 3, food 3, retail 3, manufacturing 3, telecom 3.
  - Each industry has one safe, one normal, one aggressive stock internally.
  - Safe/normal/aggressive labels are not shown in the stock list.
- Stock prices update every 5 elapsed days.
- Stock chart is server-rendered SVG candlestick.
- Stock prices use coin values.
- Coin exchange rate: `1 coin = 100 won`.
- Buy/sell fee: 0.5%, rounded up.
- Buy/sell are immediate market-price actions.
- Loss is limited to principal; no margin/debt stock trading.
- Stock trade history exists.
- Trade history currently shows records from the last 90 days.
- Trade history filters:
  - all
  - buy
  - sell
  - by stock
- Cash/coin exchange has quick buttons:
  - all available
  - 100k
  - 1m
  - 10m coin
- Stock right-side panel shows holding summary above recent city records.
- Holding summary shows:
  - held stock count
  - total shares
  - total cost
  - total valuation
  - profit/loss

## Stock Events

- Stock industry boom/recession events are implemented.
- Occur only after stock content is unlocked.
- Monthly chance: 15%.
- Random industry: IT, food, retail, manufacturing, telecom.
- Random trend: boom or recession.
- Duration: next 2 stock price updates.
- Boom effect: random +3% to +8% industry effect per affected stock update.
- Recession effect: random -3% to -8% industry effect per affected stock update.
- Event modal images are stored under `src/main/resources/static/assets/stock-news/`.
- Stock industry news records use `RecordType.STOCK_EVENT`.

## Real Estate News Events

- City real-estate boom/fall news events exist.
- Applies to current city only.
- Monthly chance: 15%.
- Effect duration: next 2 market refreshes.
- Real-estate news images are under `src/main/resources/static/assets/news/`.

## Important Files

- Stock service: `src/main/java/com/game/buildingstory/service/StockService.java`
- Stock entities:
  - `src/main/java/com/game/buildingstory/domain/OwnedStock.java`
  - `src/main/java/com/game/buildingstory/domain/StockPriceHistory.java`
  - `src/main/java/com/game/buildingstory/domain/StockTradeHistory.java`
- Stock view fragment: `src/main/resources/templates/fragments/main-stock-panel.html`
- Main view: `src/main/resources/templates/main.html`
- Frontend logic: `src/main/resources/static/app.js`
- Styles: `src/main/resources/static/styles.css`
- Controller: `src/main/java/com/game/buildingstory/web/GameController.java`
- Model assembler: `src/main/java/com/game/buildingstory/web/MainPageModelAssembler.java`
- Tests: `src/test/java/com/game/buildingstory/BuildingStoryApplicationTests.java`

## Validation Notes

- Regression tests cover:
  - stock unlock after Seoul schedule
  - 5-day stock price updates
  - stock coin exchange and immediate buy/sell with fee
  - stock-view deferred city events
  - stock industry news activation and 2-update duration
  - real-estate news 2-refresh duration
- Last full test before push passed.

## Known Tooling Notes

- `gh` command is unavailable.
- Use normal `git` for commit/push.
- If PR creation is needed, install/configure GitHub CLI or create PR manually on GitHub.

## Suggested Next Work

- Decide whether stock industry boom/recession should be visible as an active badge/status on the stock screen.
- Add QA button for stock industry event activation if manual testing is needed.
- Consider splitting stock UI JS from `app.js` if stock UI grows further.
- Consider adding portfolio detail per held stock if holding summary becomes too compact.
