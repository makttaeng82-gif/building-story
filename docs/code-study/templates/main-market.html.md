# main-market.html 코드 주석형 해설

원본 파일: `src/main/resources/templates/fragments/main-market.html`

형식:
- 원본 코드를 그대로 배치한다.
- 필요한 코드 바로 아래에 해설 주석을 붙인다.
- import/package/단순 선언은 과하게 설명하지 않는다.

```html
<!doctype html>
<html lang="ko" xmlns:th="http://www.thymeleaf.org">
<body>
<section th:fragment="market" class="panel market-panel">
    <!--
    현재 도시의 부동산 매물 목록이다.
    offers는 아직 구매하지 않은 BuildingOffer이고, 구매 버튼은 현금/대출 모드를 서버로 POST한다.
    -->
    <div class="section-head">
        <div>
            <div class="market-title-row">
                <h2 th:text="${player.currentCity} + ' 매물'">매물</h2>
                <!-- 해설: 서버 모델 값을 HTML 텍스트로 출력한다. 기본적으로 이스케이프되어 안전하다. -->
                <span class="badge market-news-badge"
                      th:if="${!@gameService.marketNewsStatusText(player, player.currentCity).isBlank()}"
                      <!-- 해설: 서버 모델 값 조건에 따라 이 요소를 보여주거나 숨긴다. -->
                      th:classappend="${player.activeMarketNewsTrend == 'RISE'} ? ' danger' : ' good'"
                      th:text="${@gameService.marketNewsStatusText(player, player.currentCity)}">뉴스</span>
                      <!-- 해설: 서버 모델 값을 HTML 텍스트로 출력한다. 기본적으로 이스케이프되어 안전하다. -->
            </div>
            <p th:text="'건물 종류별 매물 · 5일마다 가격 갱신 · 다음 갱신 ' + ${player.ddayText(player.offerRefreshDday())}">매물 갱신</p>
            <!-- 해설: 서버 모델 값을 HTML 텍스트로 출력한다. 기본적으로 이스케이프되어 안전하다. -->
        </div>
        <div class="refresh-progress-card" aria-label="매물 갱신 진행률">
            <span>매물 갱신</span>
            <strong th:text="${player.ddayText(player.offerRefreshDday())}">D-5</strong>
            <!-- 해설: 서버 모델 값을 HTML 텍스트로 출력한다. 기본적으로 이스케이프되어 안전하다. -->
            <div class="refresh-progress">
                <span th:style="'width: ' + ${player.offerRefreshProgressPercent()} + '%'"></span>
            </div>
            <small th:text="${player.offerRefreshProgressPercent()} + '%'">0%</small>
            <!-- 해설: 서버 모델 값을 HTML 텍스트로 출력한다. 기본적으로 이스케이프되어 안전하다. -->
        </div>
    </div>
    <div class="market-list">
        <article class="market-card" th:each="offer : ${offers}"
        <!-- 해설: 서버에서 받은 목록을 반복 렌더링한다. 목록 원소마다 이 HTML 블록이 한 번씩 생성된다. -->
                 th:with="offerUnlocked=${@gameService.isOfferUnlocked(player, offer)}, purchaseCooldown=${@gameService.purchaseCooldownDaysLeft(player, offer)}, purchaseWaiting=${purchaseCooldown > 0}"
                 th:classappend="${!offerUnlocked} ? ' locked-offer' : (${purchaseWaiting} ? ' waiting-offer' : (${offer.valuationStatus.name() == 'UNDER'} ? ' low' : (${offer.valuationStatus.name() == 'OVER'} ? ' high' : ' normal')))">
            <img class="market-image" th:src="${@gameService.buildingImagePath(offer)}" th:alt="${offer.name} + ' 이미지'">
            <!-- 해설: 서버 모델이나 URL 표현식으로 링크/이미지/폼 액션 경로를 만든다. -->
            <span class="badge" th:classappend="${offer.valuationStatus.name() == 'UNDER'} ? ' good' : (${offer.valuationStatus.name() == 'OVER'} ? ' danger' : '')" th:text="${offer.valuationStatus.label()} + ' ' + ${offer.valuationStatus.rate()} + '%'">상태</span>
            <!-- 해설: 서버 모델 값을 HTML 텍스트로 출력한다. 기본적으로 이스케이프되어 안전하다. -->
            <h3 th:text="${offer.name}">매물명</h3>
            <!-- 해설: 서버 모델 값을 HTML 텍스트로 출력한다. 기본적으로 이스케이프되어 안전하다. -->
            <p th:text="'시장가 ' + ${@moneyText.format(offer.marketPrice)}">시장가</p>
            <!-- 해설: 서버 모델 값을 HTML 텍스트로 출력한다. 기본적으로 이스케이프되어 안전하다. -->
            <strong class="money" th:text="${@moneyText.format(offer.offerPrice)}">가격</strong>
            <!-- 해설: 서버 모델 값을 HTML 텍스트로 출력한다. 기본적으로 이스케이프되어 안전하다. -->
            <small th:text="'월세 ' + ${@moneyText.format(offer.monthlyRent)} + ' · 판매쿨타임 ' + ${offer.tradeCooldownDays} + '일'">월세</small>
            <!-- 해설: 서버 모델 값을 HTML 텍스트로 출력한다. 기본적으로 이스케이프되어 안전하다. -->
            <small th:if="${!offerUnlocked}">해금 전 · 구매 불가</small>
            <!-- 해설: 서버 모델 값 조건에 따라 이 요소를 보여주거나 숨긴다. -->
            <small th:if="${purchaseCooldown > 0}" th:text="'구매쿨타임 ' + ${player.ddayText(purchaseCooldown)}">구매쿨타임</small>
            <!-- 해설: 서버 모델 값 조건에 따라 이 요소를 보여주거나 숨긴다. -->
            <div class="market-lock-overlay" th:if="${!offerUnlocked}">
            <!-- 해설: 서버 모델 값 조건에 따라 이 요소를 보여주거나 숨긴다. -->
                <span class="lock-icon" aria-hidden="true"></span>
                <strong>해금 필요</strong>
            </div>
            <div class="market-lock-overlay waiting" th:if="${offerUnlocked && purchaseWaiting}">
            <!-- 해설: 서버 모델 값 조건에 따라 이 요소를 보여주거나 숨긴다. -->
                <span class="lock-icon" aria-hidden="true"></span>
                <strong>다음 거래</strong>
                <span th:text="${player.ddayText(purchaseCooldown)}">대기시간</span>
                <!-- 해설: 서버 모델 값을 HTML 텍스트로 출력한다. 기본적으로 이스케이프되어 안전하다. -->
            </div>
            <div class="buy-actions">
                <form method="post" th:action="@{/offers/{id}/buy(id=${offer.id})}">
                <!-- 해설: 서버 모델이나 URL 표현식으로 링크/이미지/폼 액션 경로를 만든다. -->
                    <input type="hidden" name="mode" value="cash">
                    <button type="submit" th:disabled="${player.paused || !offerUnlocked || purchaseWaiting || player.cash < offer.offerPrice}" th:text="${player.paused ? '일시정지' : (!offerUnlocked ? '해금 전' : (purchaseWaiting ? '쿨타임' : (player.cash < offer.offerPrice ? '현금부족' : '현금구매')))}">현금구매</button>
                    <!-- 해설: 서버 모델 값을 HTML 텍스트로 출력한다. 기본적으로 이스케이프되어 안전하다. -->
                </form>
                <form method="post" th:action="@{/offers/{id}/buy(id=${offer.id})}">
                <!-- 해설: 서버 모델이나 URL 표현식으로 링크/이미지/폼 액션 경로를 만든다. -->
                    <input type="hidden" name="mode" value="loan">
                    <button class="secondary" type="submit" th:disabled="${player.paused || !offerUnlocked || purchaseWaiting || player.cash < offer.cashForLoanPurchase() || availableLoanLimit < offer.loanAmount()}" th:text="${player.paused ? '일시정지' : (!offerUnlocked ? '해금 전' : (purchaseWaiting ? '쿨타임' : (player.cash < offer.cashForLoanPurchase() ? '현금부족' : (availableLoanLimit < offer.loanAmount() ? '한도초과' : '대출구매'))))}">대출구매</button>
                    <!-- 해설: 서버 모델 값을 HTML 텍스트로 출력한다. 기본적으로 이스케이프되어 안전하다. -->
                </form>
            </div>
            <small th:text="'대출구매: 대출 ' + ${@moneyText.format(offer.loanAmount())} + ' · 현금 ' + ${@moneyText.format(offer.cashForLoanPurchase())}">대출구매</small>
            <!-- 해설: 서버 모델 값을 HTML 텍스트로 출력한다. 기본적으로 이스케이프되어 안전하다. -->
        </article>
    </div>
</section>
</body>
</html>
```