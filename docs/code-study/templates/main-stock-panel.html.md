# main-stock-panel.html 코드 주석형 해설

원본 파일: `src/main/resources/templates/fragments/main-stock-panel.html`

형식:
- 원본 코드를 그대로 배치한다.
- 필요한 코드 바로 아래에 해설 주석을 붙인다.
- import/package/단순 선언은 과하게 설명하지 않는다.

```html
<!doctype html>
<html lang="ko" xmlns:th="http://www.thymeleaf.org">
<body>
<section th:fragment="stockPanel" class="panel city-panel stock-panel"
         th:attr="data-player-cash=${player.cash},data-player-coin=${player.coin}">
         <!-- 해설: data-* 속성은 JavaScript가 읽을 수 있는 화면 상태나 식별자를 HTML에 심어 둔다. -->
    <!--
    주식 화면 fragment다.
    서버가 stockQuotes, stockMarketStatus, stockTradeHistories를 미리 계산해 넘기고,
    app.js는 선택 탭, 수량 버튼, 교환 미리보기 같은 상호작용만 담당한다.
    -->
    <div class="section-head">
        <div>
            <h2>주식</h2>
            <p>회사 선택 후 주가 확인 · 주가는 5일마다 갱신</p>
            <span th:if="${stockMarketStatus.hasActiveNews()}"
            <!-- 해설: 서버 모델 값 조건에 따라 이 요소를 보여주거나 숨긴다. -->
                  class="stock-news-status"
                  th:classappend="${stockMarketStatus.activeNewsDirection()}"
                  th:text="${stockMarketStatus.activeNewsText()}">업종 이벤트 적용중</span>
                  <!-- 해설: 서버 모델 값을 HTML 텍스트로 출력한다. 기본적으로 이스케이프되어 안전하다. -->
        </div>
        <div class="refresh-progress-card" aria-label="다음 날 진행률">
            <span>주가 갱신</span>
            <strong th:text="${stockMarketStatus.nextUpdateDateText()} + ' · D-' + ${stockMarketStatus.daysUntilNextUpdate()}">다음 갱신</strong>
            <!-- 해설: 서버 모델 값을 HTML 텍스트로 출력한다. 기본적으로 이스케이프되어 안전하다. -->
            <div class="refresh-progress">
                <span id="stockDayProgress" th:style="'width:' + ${stockMarketStatus.progressPercent()} + '%'"></span>
            </div>
            <small>5일마다 갱신</small>
        </div>
    </div>

    <div class="stock-app">
        <!-- 왼쪽 목록은 모든 종목 버튼을 렌더링하고, JS가 전체/내종목 필터에 따라 hidden을 토글한다. -->
        <aside class="stock-sidebar">
            <div class="stock-filter-tabs" role="tablist" aria-label="종목 필터">
                <button type="button" class="active" data-stock-filter="all">전체종목</button>
                <!-- 해설: 사용자가 클릭해 폼 제출이나 프론트 동작을 실행하는 버튼이다. -->
                <button type="button" data-stock-filter="owned">내종목</button>
                <!-- 해설: 사용자가 클릭해 폼 제출이나 프론트 동작을 실행하는 버튼이다. -->
            </div>
            <p class="stock-empty-message" hidden>보유중인 종목 없음</p>
            <div class="stock-company-list">
            <button th:each="quote : ${stockQuotes}" th:with="stock=${quote.stock()}" type="button" class="stock-company-button"
            <!-- 해설: 서버에서 받은 목록을 반복 렌더링한다. 목록 원소마다 이 HTML 블록이 한 번씩 생성된다. -->
                    th:classappend="${quoteStat.index == 0} ? ' selected'"
                    th:attr="data-stock-key=${stock.key()},data-owned-quantity=${quote.quantity()}">
                    <!-- 해설: data-* 속성은 JavaScript가 읽을 수 있는 화면 상태나 식별자를 HTML에 심어 둔다. -->
                <span th:text="${stock.industry()}">업계</span>
                <!-- 해설: 서버 모델 값을 HTML 텍스트로 출력한다. 기본적으로 이스케이프되어 안전하다. -->
                <em th:classappend="${quote.changeDirection()}"
                    th:text="${quote.changePercentText()}">0%</em>
                    <!-- 해설: 서버 모델 값을 HTML 텍스트로 출력한다. 기본적으로 이스케이프되어 안전하다. -->
                <strong th:text="${stock.name()}">회사</strong>
                <!-- 해설: 서버 모델 값을 HTML 텍스트로 출력한다. 기본적으로 이스케이프되어 안전하다. -->
                <small>현재 <b th:text="${quote.currentPriceText()}">0원</b></small>
                <!-- 해설: 서버 모델 값을 HTML 텍스트로 출력한다. 기본적으로 이스케이프되어 안전하다. -->
                <small>전일 <b th:text="${quote.previousPriceText()}">0원</b></small>
                <!-- 해설: 서버 모델 값을 HTML 텍스트로 출력한다. 기본적으로 이스케이프되어 안전하다. -->
            </button>
            </div>
        </aside>
        <section class="stock-detail-list">
            <!-- 각 종목 상세는 모두 HTML에 존재한다. 선택된 종목 하나만 .active 클래스로 보이게 한다. -->
            <article th:each="quote : ${stockQuotes}" th:with="stock=${quote.stock()}" class="stock-detail"
            <!-- 해설: 서버에서 받은 목록을 반복 렌더링한다. 목록 원소마다 이 HTML 블록이 한 번씩 생성된다. -->
                     th:classappend="${quoteStat.index == 0} ? ' active'"
                     th:attr="data-stock-detail=${stock.key()},data-stock-price=${quote.currentPrice()},data-owned-quantity=${quote.quantity()}">
                     <!-- 해설: data-* 속성은 JavaScript가 읽을 수 있는 화면 상태나 식별자를 HTML에 심어 둔다. -->
                <div class="stock-detail-head">
                    <div>
                        <span class="badge" th:text="${stock.industry()}">업계</span>
                        <!-- 해설: 서버 모델 값을 HTML 텍스트로 출력한다. 기본적으로 이스케이프되어 안전하다. -->
                        <h3 th:text="${stock.name()}">회사명</h3>
                        <!-- 해설: 서버 모델 값을 HTML 텍스트로 출력한다. 기본적으로 이스케이프되어 안전하다. -->
                        <strong class="stock-price" th:text="${quote.currentPriceText()}">현재가</strong>
                        <!-- 해설: 서버 모델 값을 HTML 텍스트로 출력한다. 기본적으로 이스케이프되어 안전하다. -->
                    </div>
                </div>
                <div class="stock-chart-shell">
                    <!-- 캔들 차트는 StockService가 계산한 SVG 좌표를 사용한다. 브라우저 차트 라이브러리가 필요 없다. -->
                    <svg class="stock-candle-chart" viewBox="0 0 860 300" preserveAspectRatio="none" aria-label="주가 차트">
                        <line class="chart-grid-line" x1="20" y1="24" x2="700" y2="24"></line>
                        <line class="chart-grid-line" x1="20" y1="91" x2="700" y2="91"></line>
                        <line class="chart-grid-line" x1="20" y1="158" x2="700" y2="158"></line>
                        <line class="chart-grid-line" x1="20" y1="226" x2="700" y2="226"></line>
                        <g th:each="candle : ${quote.candles()}" th:classappend="${candle.rising()} ? ' candle-up' : ' candle-down'">
                        <!-- 해설: 서버에서 받은 목록을 반복 렌더링한다. 목록 원소마다 이 HTML 블록이 한 번씩 생성된다. -->
                            <line class="candle-wick"
                                  th:attr="x1=${candle.x()},x2=${candle.x()},y1=${candle.highY()},y2=${candle.lowY()}"></line>
                            <rect class="candle-body" width="7"
                                  th:attr="x=${candle.x() - 3},y=${candle.bodyY()},height=${candle.bodyHeight()}"></rect>
                        </g>
                        <line class="current-price-line" x1="20" x2="700"
                              th:attr="y1=${quote.currentPriceY()},y2=${quote.currentPriceY()}"></line>
                        <text class="chart-price-label" x="710" th:attr="y=${quote.currentPriceY()}" th:text="${quote.currentChartPriceText()}">0원</text>
                        <!-- 해설: 서버 모델 값을 HTML 텍스트로 출력한다. 기본적으로 이스케이프되어 안전하다. -->
                        <text class="chart-axis-label" x="710" y="28" th:text="${quote.maxPriceText()}">최고가</text>
                        <!-- 해설: 서버 모델 값을 HTML 텍스트로 출력한다. 기본적으로 이스케이프되어 안전하다. -->
                        <text class="chart-axis-label" x="710" y="230" th:text="${quote.minPriceText()}">최저가</text>
                        <!-- 해설: 서버 모델 값을 HTML 텍스트로 출력한다. 기본적으로 이스케이프되어 안전하다. -->
                    </svg>
                </div>
                <dl class="stock-summary-grid">
                    <div>
                        <dt>현재가</dt>
                        <dd th:text="${quote.currentPriceText()}">가격</dd>
                        <!-- 해설: 서버 모델 값을 HTML 텍스트로 출력한다. 기본적으로 이스케이프되어 안전하다. -->
                    </div>
                    <div>
                        <dt>직전가</dt>
                        <dd th:text="${quote.previousPriceText()}">가격</dd>
                        <!-- 해설: 서버 모델 값을 HTML 텍스트로 출력한다. 기본적으로 이스케이프되어 안전하다. -->
                    </div>
                    <div>
                        <dt>5일 변동</dt>
                        <dd th:class="${quote.changeDirection()}" th:text="${quote.changePercentText()} + ' · ' + ${quote.changeAmountText()}">0%</dd>
                        <!-- 해설: 서버 모델 값을 HTML 텍스트로 출력한다. 기본적으로 이스케이프되어 안전하다. -->
                    </div>
                    <div>
                        <dt>보유수량</dt>
                        <dd th:text="${quote.quantity()} + '주'">0주</dd>
                        <!-- 해설: 서버 모델 값을 HTML 텍스트로 출력한다. 기본적으로 이스케이프되어 안전하다. -->
                    </div>
                    <div>
                        <dt>평균단가</dt>
                        <dd th:text="${quote.averagePriceText()}">0원</dd>
                        <!-- 해설: 서버 모델 값을 HTML 텍스트로 출력한다. 기본적으로 이스케이프되어 안전하다. -->
                    </div>
                    <div>
                        <dt>평가손익</dt>
                        <dd th:text="${quote.valuationProfitText()}">0원</dd>
                        <!-- 해설: 서버 모델 값을 HTML 텍스트로 출력한다. 기본적으로 이스케이프되어 안전하다. -->
                    </div>
                </dl>
                <div class="stock-trade-panel">
                    <!-- 매수/매도 form은 서버 POST가 최종 검증을 한다. JS 미리보기는 사용자 편의를 위한 보조 계산이다. -->
                    <form class="stock-order-form" data-stock-buy-form method="post" th:action="@{/stocks/{key}/buy(key=${stock.key()})}">
                    <!-- 해설: 서버 모델이나 URL 표현식으로 링크/이미지/폼 액션 경로를 만든다. -->
                        <label>
                            매수 수량
                            <input type="number" name="quantity" min="1" value="1">
                        </label>
                        <div class="stock-quick-buttons" aria-label="매수 수량 빠른 선택">
                            <button class="secondary" type="button" data-quantity-action="plus" data-quantity-value="1">+1</button>
                            <!-- 해설: 사용자가 클릭해 폼 제출이나 프론트 동작을 실행하는 버튼이다. -->
                            <button class="secondary" type="button" data-quantity-action="plus" data-quantity-value="10">+10</button>
                            <!-- 해설: 사용자가 클릭해 폼 제출이나 프론트 동작을 실행하는 버튼이다. -->
                            <button class="secondary" type="button" data-quantity-action="plus" data-quantity-value="100">+100</button>
                            <!-- 해설: 사용자가 클릭해 폼 제출이나 프론트 동작을 실행하는 버튼이다. -->
                            <button class="secondary" type="button" data-quantity-action="half">50%</button>
                            <!-- 해설: 사용자가 클릭해 폼 제출이나 프론트 동작을 실행하는 버튼이다. -->
                            <button class="secondary" type="button" data-quantity-action="max">최대</button>
                            <!-- 해설: 사용자가 클릭해 폼 제출이나 프론트 동작을 실행하는 버튼이다. -->
                        </div>
                        <div class="stock-order-preview" data-stock-buy-preview>예상 비용 계산중</div>
                        <!-- 해설: data-* 속성은 JavaScript가 읽을 수 있는 화면 상태나 식별자를 HTML에 심어 둔다. -->
                        <button type="submit">매수</button>
                        <!-- 해설: 사용자가 클릭해 폼 제출이나 프론트 동작을 실행하는 버튼이다. -->
                    </form>
                    <form class="stock-order-form" data-stock-sell-form method="post" th:action="@{/stocks/{key}/sell(key=${stock.key()})}">
                    <!-- 해설: 서버 모델이나 URL 표현식으로 링크/이미지/폼 액션 경로를 만든다. -->
                        <label>
                            매도 수량
                            <input type="number" name="quantity" min="1" value="1">
                        </label>
                        <div class="stock-quick-buttons" aria-label="매도 수량 빠른 선택">
                            <button class="secondary" type="button" data-quantity-action="plus" data-quantity-value="1">+1</button>
                            <!-- 해설: 사용자가 클릭해 폼 제출이나 프론트 동작을 실행하는 버튼이다. -->
                            <button class="secondary" type="button" data-quantity-action="plus" data-quantity-value="10">+10</button>
                            <!-- 해설: 사용자가 클릭해 폼 제출이나 프론트 동작을 실행하는 버튼이다. -->
                            <button class="secondary" type="button" data-quantity-action="plus" data-quantity-value="100">+100</button>
                            <!-- 해설: 사용자가 클릭해 폼 제출이나 프론트 동작을 실행하는 버튼이다. -->
                            <button class="secondary" type="button" data-quantity-action="half">50%</button>
                            <!-- 해설: 사용자가 클릭해 폼 제출이나 프론트 동작을 실행하는 버튼이다. -->
                            <button class="secondary" type="button" data-quantity-action="max">최대</button>
                            <!-- 해설: 사용자가 클릭해 폼 제출이나 프론트 동작을 실행하는 버튼이다. -->
                        </div>
                        <div class="stock-order-preview" data-stock-sell-preview>예상 수령 계산중</div>
                        <!-- 해설: data-* 속성은 JavaScript가 읽을 수 있는 화면 상태나 식별자를 HTML에 심어 둔다. -->
                        <button class="secondary" type="submit" th:disabled="${quote.quantity() <= 0}">매도</button>
                        <!-- 해설: 사용자가 클릭해 폼 제출이나 프론트 동작을 실행하는 버튼이다. -->
                    </form>
                </div>
            </article>
        </section>
    </div>
    <div class="stock-bottom-grid">
        <article class="stock-exchange-panel">
            <h3>현금 · 코인 교환</h3>
            <p>1코인 = 현금 100원</p>
            <form data-stock-exchange-form data-exchange-type="cash-to-coin" method="post" action="/stocks/exchange/cash-to-coin">
            <!-- 해설: POST 폼이다. 버튼 클릭 시 서버 상태를 변경하는 요청을 보낸다. -->
                <label>
                    구매 코인
                    <input type="number" name="coinAmount" min="1" value="10000">
                </label>
                <div class="stock-exchange-quick-buttons" aria-label="구매 코인 빠른 선택">
                    <button class="secondary" type="button" data-exchange-action="max">전액</button>
                    <!-- 해설: 사용자가 클릭해 폼 제출이나 프론트 동작을 실행하는 버튼이다. -->
                    <button class="secondary" type="button" data-exchange-action="set" data-exchange-value="100000">10만</button>
                    <!-- 해설: 사용자가 클릭해 폼 제출이나 프론트 동작을 실행하는 버튼이다. -->
                    <button class="secondary" type="button" data-exchange-action="set" data-exchange-value="1000000">100만</button>
                    <!-- 해설: 사용자가 클릭해 폼 제출이나 프론트 동작을 실행하는 버튼이다. -->
                    <button class="secondary" type="button" data-exchange-action="set" data-exchange-value="10000000">1000만</button>
                    <!-- 해설: 사용자가 클릭해 폼 제출이나 프론트 동작을 실행하는 버튼이다. -->
                </div>
                <div class="stock-order-preview" data-exchange-preview>예상 교환액 계산중</div>
                <!-- 해설: data-* 속성은 JavaScript가 읽을 수 있는 화면 상태나 식별자를 HTML에 심어 둔다. -->
                <button type="submit">현금 → 코인</button>
                <!-- 해설: 사용자가 클릭해 폼 제출이나 프론트 동작을 실행하는 버튼이다. -->
            </form>
            <form data-stock-exchange-form data-exchange-type="coin-to-cash" method="post" action="/stocks/exchange/coin-to-cash">
            <!-- 해설: POST 폼이다. 버튼 클릭 시 서버 상태를 변경하는 요청을 보낸다. -->
                <label>
                    환전 코인
                    <input type="number" name="coinAmount" min="1" value="10000">
                </label>
                <div class="stock-exchange-quick-buttons" aria-label="환전 코인 빠른 선택">
                    <button class="secondary" type="button" data-exchange-action="max">전액</button>
                    <!-- 해설: 사용자가 클릭해 폼 제출이나 프론트 동작을 실행하는 버튼이다. -->
                    <button class="secondary" type="button" data-exchange-action="set" data-exchange-value="100000">10만</button>
                    <!-- 해설: 사용자가 클릭해 폼 제출이나 프론트 동작을 실행하는 버튼이다. -->
                    <button class="secondary" type="button" data-exchange-action="set" data-exchange-value="1000000">100만</button>
                    <!-- 해설: 사용자가 클릭해 폼 제출이나 프론트 동작을 실행하는 버튼이다. -->
                    <button class="secondary" type="button" data-exchange-action="set" data-exchange-value="10000000">1000만</button>
                    <!-- 해설: 사용자가 클릭해 폼 제출이나 프론트 동작을 실행하는 버튼이다. -->
                </div>
                <div class="stock-order-preview" data-exchange-preview>예상 교환액 계산중</div>
                <!-- 해설: data-* 속성은 JavaScript가 읽을 수 있는 화면 상태나 식별자를 HTML에 심어 둔다. -->
                <button class="secondary" type="submit">코인 → 현금</button>
                <!-- 해설: 사용자가 클릭해 폼 제출이나 프론트 동작을 실행하는 버튼이다. -->
            </form>
        </article>
        <article class="stock-history-panel" data-stock-history-panel>
        <!-- 해설: data-* 속성은 JavaScript가 읽을 수 있는 화면 상태나 식별자를 HTML에 심어 둔다. -->
            <div class="stock-history-head">
                <h3>거래내역</h3>
                <div class="stock-history-filter-tabs" role="tablist" aria-label="거래 유형 필터">
                    <button type="button" class="active" data-trade-filter="all">전체</button>
                    <!-- 해설: 사용자가 클릭해 폼 제출이나 프론트 동작을 실행하는 버튼이다. -->
                    <button type="button" data-trade-filter="매수">매수</button>
                    <!-- 해설: 사용자가 클릭해 폼 제출이나 프론트 동작을 실행하는 버튼이다. -->
                    <button type="button" data-trade-filter="매도">매도</button>
                    <!-- 해설: 사용자가 클릭해 폼 제출이나 프론트 동작을 실행하는 버튼이다. -->
                </div>
                <label>
                    종목별
                    <select data-trade-stock-filter>
                    <!-- 해설: data-* 속성은 JavaScript가 읽을 수 있는 화면 상태나 식별자를 HTML에 심어 둔다. -->
                        <option value="all">전체 종목</option>
                        <option th:each="stock : ${stockSpecs}" th:value="${stock.key()}" th:text="${stock.name()}">종목</option>
                        <!-- 해설: 서버에서 받은 목록을 반복 렌더링한다. 목록 원소마다 이 HTML 블록이 한 번씩 생성된다. -->
                    </select>
                </label>
            </div>
            <ul class="stock-trade-history" th:if="${!stockTradeHistories.isEmpty()}">
            <!-- 해설: 서버 모델 값 조건에 따라 이 요소를 보여주거나 숨긴다. -->
                <li th:each="trade : ${stockTradeHistories}"
                <!-- 해설: 서버에서 받은 목록을 반복 렌더링한다. 목록 원소마다 이 HTML 블록이 한 번씩 생성된다. -->
                    th:attr="data-trade-type=${trade.tradeType},data-trade-stock-key=${trade.stockKey}">
                    <!-- 해설: data-* 속성은 JavaScript가 읽을 수 있는 화면 상태나 식별자를 HTML에 심어 둔다. -->
                    <b th:text="${trade.month} + '월 ' + ${trade.day} + '일'">날짜</b>
                    <!-- 해설: 서버 모델 값을 HTML 텍스트로 출력한다. 기본적으로 이스케이프되어 안전하다. -->
                    <span th:text="${trade.tradeType} + ' · ' + ${trade.stockName} + ' ' + ${trade.quantity} + '주'">거래</span>
                    <!-- 해설: 서버 모델 값을 HTML 텍스트로 출력한다. 기본적으로 이스케이프되어 안전하다. -->
                    <small th:text="'단가 ' + ${@gameService.stockCoinText(trade.price)} + ' · 수수료 ' + ${@gameService.stockCoinText(trade.fee)} + ' · ' + ${@gameService.stockCoinText(trade.netAmount)}">금액</small>
                    <!-- 해설: 서버 모델 값을 HTML 텍스트로 출력한다. 기본적으로 이스케이프되어 안전하다. -->
                </li>
            </ul>
            <p class="stock-trade-empty-message" hidden>조건에 맞는 거래내역 없음</p>
            <p th:if="${stockTradeHistories.isEmpty()}">거래내역 없음</p>
            <!-- 해설: 서버 모델 값 조건에 따라 이 요소를 보여주거나 숨긴다. -->
        </article>
    </div>
</section>
</body>
</html>
```