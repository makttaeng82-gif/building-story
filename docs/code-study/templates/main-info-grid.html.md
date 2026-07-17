# main-info-grid.html 코드 주석형 해설

원본 파일: `src/main/resources/templates/fragments/main-info-grid.html`

형식:
- 원본 코드를 그대로 배치한다.
- 필요한 코드 바로 아래에 해설 주석을 붙인다.
- import/package/단순 선언은 과하게 설명하지 않는다.

```html
<!doctype html>
<html lang="ko" xmlns:th="http://www.thymeleaf.org">
<body>
<section th:fragment="infoGrid" class="info-grid">
    <!--
    메인 화면의 보조 정보 그리드다.
    선택한 건물 상세, 대출 현황, 상점/기부/선물 같은 부가 행동 패널을 배치한다.
    -->
    <article class="panel">
        <h2>선택 건물</h2>
        <div class="building-detail" th:each="building : ${buildings}" th:classappend="${buildingStat.index == 0} ? ' active'" th:attr="data-building-detail-id=${building.id}">
        <!-- 해설: 서버에서 받은 목록을 반복 렌더링한다. 목록 원소마다 이 HTML 블록이 한 번씩 생성된다. -->
            <span class="badge good">보유중</span>
            <h3 class="selected-building-title" th:text="${building.name} + ' #' + ${buildingStat.count}">건물</h3>
            <!-- 해설: 서버 모델 값을 HTML 텍스트로 출력한다. 기본적으로 이스케이프되어 안전하다. -->
            <dl>
                <div class="price-split-row">
                    <dt>구매가 / 시세</dt>
                    <dd class="price-split">
                        <span><b>구매가</b><em th:text="${@moneyText.format(building.purchasePrice)}">0원</em></span>
                        <!-- 해설: 서버 모델 값을 HTML 텍스트로 출력한다. 기본적으로 이스케이프되어 안전하다. -->
                        <span><b>시세</b><em th:text="${@moneyText.format(building.marketPrice)}">0원</em></span>
                        <!-- 해설: 서버 모델 값을 HTML 텍스트로 출력한다. 기본적으로 이스케이프되어 안전하다. -->
                    </dd>
                </div>
                <div><dt>월세</dt><dd th:text="${@gameService.rentWaivedBySecretaryEvent(player, building) ? '월세 감면' : @moneyText.format(@gameService.effectiveMonthlyRent(player, building))}">0원</dd></div>
                <!-- 해설: 서버 모델 값을 HTML 텍스트로 출력한다. 기본적으로 이스케이프되어 안전하다. -->
                <div><dt>상태</dt><dd th:text="${@gameService.secretaryTenantStatusText(building).isBlank() ? (building.occupied ? (building.protectedTenant ? '후배 거주중' : (building.tenantProtectedDaysLeft(player.elapsedDays) > 0 ? '입주중 · 퇴거보호 ' + player.ddayText(building.tenantProtectedDaysLeft(player.elapsedDays)) : '입주중')) : '공실') : @gameService.secretaryTenantStatusText(building)}">상태</dd></div>
                <!-- 해설: 서버 모델 값을 HTML 텍스트로 출력한다. 기본적으로 이스케이프되어 안전하다. -->
                <div><dt>수리</dt><dd th:text="${building.repairRequested ? '요청중 · 비용 ' + @moneyText.format(building.repairCost()) : '요청 없음'}">수리</dd></div>
                <!-- 해설: 서버 모델 값을 HTML 텍스트로 출력한다. 기본적으로 이스케이프되어 안전하다. -->
                <div><dt>판매 가능</dt><dd th:text="${@gameService.sellAvailabilityText(player, building)}">D-0</dd></div>
                <!-- 해설: 서버 모델 값을 HTML 텍스트로 출력한다. 기본적으로 이스케이프되어 안전하다. -->
            </dl>
            <div class="button-row">
                <form method="post" th:action="@{/buildings/{id}/repair(id=${building.id})}">
                <!-- 해설: 서버 모델이나 URL 표현식으로 링크/이미지/폼 액션 경로를 만든다. -->
                    <button type="submit" th:disabled="${player.paused || !building.repairRequested || player.cash < building.repairCost()}" th:text="${player.paused ? '일시정지' : (!building.repairRequested ? '수리요청 없음' : (player.cash < building.repairCost() ? '수리비 부족' : '수리 처리'))}">수리 처리</button>
                    <!-- 해설: 서버 모델 값을 HTML 텍스트로 출력한다. 기본적으로 이스케이프되어 안전하다. -->
                </form>
                <form method="post" th:action="@{/buildings/{id}/sell(id=${building.id})}">
                <!-- 해설: 서버 모델이나 URL 표현식으로 링크/이미지/폼 액션 경로를 만든다. -->
                    <button class="secondary" type="submit" th:disabled="${player.paused || !@gameService.canSell(player, building)}" th:text="${player.paused ? '일시정지' : '판매'}">판매</button>
                    <!-- 해설: 서버 모델 값을 HTML 텍스트로 출력한다. 기본적으로 이스케이프되어 안전하다. -->
                </form>
            </div>
        </div>
        <p th:if="${buildings.isEmpty()}">보유 건물 없음</p>
        <!-- 해설: 서버 모델 값 조건에 따라 이 요소를 보여주거나 숨긴다. -->
    </article>

    <article class="panel recent-record-panel" data-recent-record-panel>
    <!-- 해설: data-* 속성은 JavaScript가 읽을 수 있는 화면 상태나 식별자를 HTML에 심어 둔다. -->
        <div class="record-panel-head">
            <h2>최근 기록</h2>
            <button class="secondary" type="button" data-record-panel-toggle>고정</button>
            <!-- 해설: 사용자가 클릭해 폼 제출이나 프론트 동작을 실행하는 버튼이다. -->
        </div>
        <ul class="event-list record-list" th:if="${!records.isEmpty()}">
        <!-- 해설: 서버 모델 값 조건에 따라 이 요소를 보여주거나 숨긴다. -->
            <li th:each="record : ${records}">
            <!-- 해설: 서버에서 받은 목록을 반복 렌더링한다. 목록 원소마다 이 HTML 블록이 한 번씩 생성된다. -->
                <b th:text="${record.month} + '월 ' + ${record.day} + '일'">1월 1일</b>
                <!-- 해설: 서버 모델 값을 HTML 텍스트로 출력한다. 기본적으로 이스케이프되어 안전하다. -->
                <span th:text="${record.title}">기록</span>
                <!-- 해설: 서버 모델 값을 HTML 텍스트로 출력한다. 기본적으로 이스케이프되어 안전하다. -->
                <span th:if="${record.buildingName != null}" th:text="' · ' + ${record.buildingName}">건물</span>
                <!-- 해설: 서버 모델 값 조건에 따라 이 요소를 보여주거나 숨긴다. -->
                <span th:if="${record.amount != null}"
                <!-- 해설: 서버 모델 값 조건에 따라 이 요소를 보여주거나 숨긴다. -->
                      th:classappend="${record.amount > 0} ? ' money-in' : ' money-out'"
                      th:text="' · ' + ${record.amount > 0 ? '+' : '-'} + ${@moneyText.format(record.amount < 0 ? -record.amount : record.amount)}">금액</span>
                      <!-- 해설: 서버 모델 값을 HTML 텍스트로 출력한다. 기본적으로 이스케이프되어 안전하다. -->
                <span th:if="${record.reputationChange != 0}" th:text="' · 평판 ' + ${record.reputationChange > 0 ? '+' : ''} + ${@gameService.reputationText(record.reputationChange)}">평판</span>
                <!-- 해설: 서버 모델 값 조건에 따라 이 요소를 보여주거나 숨긴다. -->
                <small th:if="${record.memo != null}" th:text="${record.memo}">메모</small>
                <!-- 해설: 서버 모델 값 조건에 따라 이 요소를 보여주거나 숨긴다. -->
            </li>
        </ul>
        <p th:if="${records.isEmpty()}">최근 기록 없음</p>
        <!-- 해설: 서버 모델 값 조건에 따라 이 요소를 보여주거나 숨긴다. -->
    </article>

    <article class="panel">
        <h2>대출현황</h2>
        <p>즉시상환 가능 · 6개월 만기 미상환 시 평판 절반 감소 후 6개월 유예</p>
        <div class="loan-box">
            <div><span>대출 한도</span><strong th:text="${@moneyText.format(loanLimit)}">0원</strong></div>
            <!-- 해설: 서버 모델 값을 HTML 텍스트로 출력한다. 기본적으로 이스케이프되어 안전하다. -->
            <div><span>대출 가능</span><strong th:text="${@moneyText.format(availableLoanLimit)}">0원</strong></div>
            <!-- 해설: 서버 모델 값을 HTML 텍스트로 출력한다. 기본적으로 이스케이프되어 안전하다. -->
            <div><span>대출 건수</span><strong th:text="${loans.size()} + '건'">0건</strong></div>
            <!-- 해설: 서버 모델 값을 HTML 텍스트로 출력한다. 기본적으로 이스케이프되어 안전하다. -->
            <div><span>대출 원금 합계</span><strong th:text="${@moneyText.format(loanPrincipal)}">0원</strong></div>
            <!-- 해설: 서버 모델 값을 HTML 텍스트로 출력한다. 기본적으로 이스케이프되어 안전하다. -->
            <div><span>남은 상환액</span><strong th:text="${@moneyText.format(loanRepaymentTotal)}">0원</strong></div>
            <!-- 해설: 서버 모델 값을 HTML 텍스트로 출력한다. 기본적으로 이스케이프되어 안전하다. -->
        </div>
        <div class="loan-list" th:if="${!loans.isEmpty()}">
        <!-- 해설: 서버 모델 값 조건에 따라 이 요소를 보여주거나 숨긴다. -->
            <div class="loan-row" th:each="loan : ${loans}">
            <!-- 해설: 서버에서 받은 목록을 반복 렌더링한다. 목록 원소마다 이 HTML 블록이 한 번씩 생성된다. -->
                <strong th:text="'대출 #' + ${loan.id}">대출</strong>
                <!-- 해설: 서버 모델 값을 HTML 텍스트로 출력한다. 기본적으로 이스케이프되어 안전하다. -->
                <span th:text="'원금 ' + ${@moneyText.format(loan.principal)}">원금</span>
                <!-- 해설: 서버 모델 값을 HTML 텍스트로 출력한다. 기본적으로 이스케이프되어 안전하다. -->
                <span th:text="'잔여 ' + ${loan.remainingMonths} + '개월'">잔여</span>
                <!-- 해설: 서버 모델 값을 HTML 텍스트로 출력한다. 기본적으로 이스케이프되어 안전하다. -->
                <span th:text="'남은 상환 ' + ${@moneyText.format(loan.remainingRepayment())}">남은 상환</span>
                <!-- 해설: 서버 모델 값을 HTML 텍스트로 출력한다. 기본적으로 이스케이프되어 안전하다. -->
                <form method="post" th:action="@{/loans/{id}/repay(id=${loan.id})}">
                <!-- 해설: 서버 모델이나 URL 표현식으로 링크/이미지/폼 액션 경로를 만든다. -->
                    <button class="secondary" type="submit" th:disabled="${player.paused || player.cash < loan.remainingRepayment()}" th:text="${player.paused ? '일시정지' : '즉시상환'}">즉시상환</button>
                    <!-- 해설: 서버 모델 값을 HTML 텍스트로 출력한다. 기본적으로 이스케이프되어 안전하다. -->
                </form>
            </div>
        </div>
        <p th:if="${loans.isEmpty()}">현재 대출 없음</p>
        <!-- 해설: 서버 모델 값 조건에 따라 이 요소를 보여주거나 숨긴다. -->
    </article>

    <details class="panel reputation-shop-panel collapsible-panel" data-collapsible-key="donation" open>
    <!-- 해설: data-* 속성은 JavaScript가 읽을 수 있는 화면 상태나 식별자를 HTML에 심어 둔다. -->
        <summary>기부</summary>
        <p>현금 50,000원당 평판 1</p>
        <div class="donation-actions">
            <form class="donation-card confirm-form" method="post" action="/donations" data-confirm-title="구매 하시겠습니까?" data-confirm-message="기부 x1">
            <!-- 해설: POST 폼이다. 버튼 클릭 시 서버 상태를 변경하는 요청을 보낸다. -->
                <img class="shop-image donation-image" src="/assets/shop/donation-1.png" alt="기부 x1">
                <strong>x1</strong>
                <span>50,000원</span>
                <span>평판 +1</span>
                <input type="hidden" name="multiplier" value="1">
                <button class="secondary" type="submit" th:disabled="${player.paused || player.cash < 50000}" th:text="${player.paused ? '일시정지' : '구매'}">구매</button>
                <!-- 해설: 서버 모델 값을 HTML 텍스트로 출력한다. 기본적으로 이스케이프되어 안전하다. -->
            </form>
            <form class="donation-card confirm-form" method="post" action="/donations" data-confirm-title="구매 하시겠습니까?" data-confirm-message="기부 x10">
            <!-- 해설: POST 폼이다. 버튼 클릭 시 서버 상태를 변경하는 요청을 보낸다. -->
                <img class="shop-image donation-image" src="/assets/shop/donation-10.png" alt="기부 x10">
                <strong>x10</strong>
                <span>500,000원</span>
                <span>평판 +10</span>
                <input type="hidden" name="multiplier" value="10">
                <button class="secondary" type="submit" th:disabled="${player.paused || player.cash < 500000}" th:text="${player.paused ? '일시정지' : '구매'}">구매</button>
                <!-- 해설: 서버 모델 값을 HTML 텍스트로 출력한다. 기본적으로 이스케이프되어 안전하다. -->
            </form>
            <form class="donation-card confirm-form" method="post" action="/donations" data-confirm-title="구매 하시겠습니까?" data-confirm-message="기부 x100">
            <!-- 해설: POST 폼이다. 버튼 클릭 시 서버 상태를 변경하는 요청을 보낸다. -->
                <img class="shop-image donation-image" src="/assets/shop/donation-100.png" alt="기부 x100">
                <strong>x100</strong>
                <span>5,000,000원</span>
                <span>평판 +100</span>
                <input type="hidden" name="multiplier" value="100">
                <button class="secondary" type="submit" th:disabled="${player.paused || player.cash < 5000000}" th:text="${player.paused ? '일시정지' : '구매'}">구매</button>
                <!-- 해설: 서버 모델 값을 HTML 텍스트로 출력한다. 기본적으로 이스케이프되어 안전하다. -->
            </form>
            <form class="donation-card confirm-form" method="post" action="/donations" data-confirm-title="구매 하시겠습니까?" data-confirm-message="기부 x1000">
            <!-- 해설: POST 폼이다. 버튼 클릭 시 서버 상태를 변경하는 요청을 보낸다. -->
                <img class="shop-image donation-image" src="/assets/shop/donation-1000.png" alt="기부 x1000">
                <strong>x1000</strong>
                <span>50,000,000원</span>
                <span>평판 +1000</span>
                <input type="hidden" name="multiplier" value="1000">
                <button class="secondary" type="submit" th:disabled="${player.paused || player.cash < 50000000}" th:text="${player.paused ? '일시정지' : '구매'}">구매</button>
                <!-- 해설: 서버 모델 값을 HTML 텍스트로 출력한다. 기본적으로 이스케이프되어 안전하다. -->
            </form>
        </div>
    </details>

    <section class="shop-split-grid">
        <details class="panel reputation-shop-panel split-shop-panel collapsible-panel" data-collapsible-key="luxury" open>
        <!-- 해설: data-* 속성은 JavaScript가 읽을 수 있는 화면 상태나 식별자를 HTML에 심어 둔다. -->
            <summary>사치품</summary>
            <p>기부 대비 평판 효율 1.5배 · 각 아이템 1회 구매</p>
            <div class="luxury-list">
                <div class="luxury-row" th:each="item : ${luxuryItems}" th:with="itemOwned=${@gameService.isLuxuryItemOwned(player, item)}">
                <!-- 해설: 서버에서 받은 목록을 반복 렌더링한다. 목록 원소마다 이 HTML 블록이 한 번씩 생성된다. -->
                    <img class="shop-image luxury-image" th:src="${item.imagePath()}" th:alt="${item.name()} + ' 이미지'">
                    <!-- 해설: 서버 모델이나 URL 표현식으로 링크/이미지/폼 액션 경로를 만든다. -->
                    <strong th:text="${item.name()}">아이템</strong>
                    <!-- 해설: 서버 모델 값을 HTML 텍스트로 출력한다. 기본적으로 이스케이프되어 안전하다. -->
                    <span th:text="${@moneyText.format(item.price())}">가격</span>
                    <!-- 해설: 서버 모델 값을 HTML 텍스트로 출력한다. 기본적으로 이스케이프되어 안전하다. -->
                    <span th:text="'평판 +' + ${@gameService.reputationText(item.reputationReward())}">평판</span>
                    <!-- 해설: 서버 모델 값을 HTML 텍스트로 출력한다. 기본적으로 이스케이프되어 안전하다. -->
                    <form class="confirm-form" method="post" th:action="@{/luxury-items/{key}/buy(key=${item.key()})}" th:attr="data-confirm-title='구매 하시겠습니까?', data-confirm-message=${item.name()} + ' · ' + ${@moneyText.format(item.price())}">
                    <!-- 해설: 서버 모델이나 URL 표현식으로 링크/이미지/폼 액션 경로를 만든다. -->
                        <button class="secondary" type="submit" th:disabled="${player.paused || itemOwned || player.cash < item.price()}" th:text="${player.paused ? '일시정지' : (itemOwned ? '구매완료' : (player.cash < item.price() ? '현금부족' : '구매'))}">구매</button>
                        <!-- 해설: 서버 모델 값을 HTML 텍스트로 출력한다. 기본적으로 이스케이프되어 안전하다. -->
                    </form>
                </div>
            </div>
        </details>

        <details class="panel reputation-shop-panel split-shop-panel collapsible-panel" data-collapsible-key="gift" open>
        <!-- 해설: data-* 속성은 JavaScript가 읽을 수 있는 화면 상태나 식별자를 HTML에 심어 둔다. -->
            <summary>선물</summary>
            <p>구입 후 보유 비서에게 선물 가능</p>
            <div class="gift-list">
                <form class="gift-row confirm-form" method="post" th:each="gift : ${giftItems}" th:action="@{/gift-items/{key}/buy(key=${gift.key()})}" th:with="ownedQuantity=${@gameService.ownedGiftQuantity(player, gift)}" th:attr="data-confirm-title='구매 하시겠습니까?', data-confirm-message=${gift.name()}">
                <!-- 해설: 서버에서 받은 목록을 반복 렌더링한다. 목록 원소마다 이 HTML 블록이 한 번씩 생성된다. -->
                    <img class="shop-image gift-image" th:src="${gift.imagePath()}" th:alt="${gift.name()} + ' 이미지'">
                    <!-- 해설: 서버 모델이나 URL 표현식으로 링크/이미지/폼 액션 경로를 만든다. -->
                    <strong th:text="${gift.name()}">선물</strong>
                    <!-- 해설: 서버 모델 값을 HTML 텍스트로 출력한다. 기본적으로 이스케이프되어 안전하다. -->
                    <span th:text="${@moneyText.format(gift.price())}">가격</span>
                    <!-- 해설: 서버 모델 값을 HTML 텍스트로 출력한다. 기본적으로 이스케이프되어 안전하다. -->
                    <span th:text="'호감도 +' + ${gift.affinityExperience()}">호감도</span>
                    <!-- 해설: 서버 모델 값을 HTML 텍스트로 출력한다. 기본적으로 이스케이프되어 안전하다. -->
                    <input class="quantity-input" type="number" name="quantity" min="1" max="99" value="1" th:disabled="${player.paused}">
                    <span class="gift-owned" th:text="'보유 ' + ${ownedQuantity}">보유 0</span>
                    <!-- 해설: 서버 모델 값을 HTML 텍스트로 출력한다. 기본적으로 이스케이프되어 안전하다. -->
                    <button class="secondary" type="submit" th:disabled="${player.paused || player.cash < gift.price()}" th:text="${player.paused ? '일시정지' : (player.cash < gift.price() ? '현금부족' : '구매')}">구매</button>
                    <!-- 해설: 서버 모델 값을 HTML 텍스트로 출력한다. 기본적으로 이스케이프되어 안전하다. -->
                </form>
            </div>
        </details>
    </section>

    <article th:replace="~{fragments/main-test-panel :: testPanel}"></article>
    <!-- 해설: Thymeleaf fragment를 현재 위치에 끼워 넣는다. 반복되는 화면 조각을 재사용한다. -->
</section>
</body>
</html>
```