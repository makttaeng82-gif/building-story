# main.html 코드 주석형 해설

원본 파일: `src/main/resources/templates/main.html`

형식:
- 원본 코드를 그대로 배치한다.
- 필요한 코드 바로 아래에 해설 주석을 붙인다.
- import/package/단순 선언은 과하게 설명하지 않는다.

```html
<!doctype html>
<html lang="ko" xmlns:th="http://www.thymeleaf.org">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>건물주이야기</title>
    <script>
        (function () {
            if ("scrollRestoration" in window.history) {
                window.history.scrollRestoration = "manual";
            }
            if (window.sessionStorage.getItem("buildingStory.scrollY") !== null) {
                document.documentElement.classList.add("restore-scroll-pending");
            }
        })();
    </script>
    <link rel="stylesheet" href="/styles.css">
</head>
<body th:classappend="${screenPaused} ? ' game-paused'"
      th:attr="data-player-paused=${screenPaused}">
      <!-- 해설: data-* 속성은 JavaScript가 읽을 수 있는 화면 상태나 식별자를 HTML에 심어 둔다. -->
<!--
main.html은 로그인 후 플레이어가 가장 오래 머무는 메인 화면이다.
MainPageModelAssembler가 Model에 담은 값들을 Thymeleaf 표현식(${...})으로 읽어
도시/주식/비서/이벤트 상태를 한 번에 렌더링한다.
-->
<div class="app">
    <header class="topbar">
        <div class="brand">
            <span class="logo">B</span>
            <div>
                <h1>건물주이야기</h1>
                <p th:text="${player.username}">사용자</p>
                <!-- 해설: 서버 모델 값을 HTML 텍스트로 출력한다. 기본적으로 이스케이프되어 안전하다. -->
            </div>
        </div>

        <div class="status-grid">
            <div class="status-card">
                <span>현금</span>
                <strong id="cashValue" class="money" th:text="${@moneyText.format(player.cash)}">0원</strong>
                <!-- 해설: 서버 모델 값을 HTML 텍스트로 출력한다. 기본적으로 이스케이프되어 안전하다. -->
                <small class="status-subline">대출 <b th:text="${@moneyText.format(loanRemainingRepayment)}">0원</b></small>
                <!-- 해설: 서버 모델 값을 HTML 텍스트로 출력한다. 기본적으로 이스케이프되어 안전하다. -->
            </div>
            <div class="status-card">
                <span>총 월세</span>
                <strong id="totalMonthlyRentValue" class="money" th:text="${@moneyText.format(totalMonthlyRent)}">0원</strong>
                <!-- 해설: 서버 모델 값을 HTML 텍스트로 출력한다. 기본적으로 이스케이프되어 안전하다. -->
            </div>
            <div class="status-card">
                <span>코인</span>
                <strong id="coinValue" class="money" th:text="${@gameService.stockCoinText(player.coin)}">0코인</strong>
                <!-- 해설: 서버 모델 값을 HTML 텍스트로 출력한다. 기본적으로 이스케이프되어 안전하다. -->
            </div>
            <div class="status-card" th:if="${player.employed}">
            <!-- 해설: 서버 모델 값 조건에 따라 이 요소를 보여주거나 숨긴다. -->
                <span>직장</span>
                <strong th:text="${player.employed ? '재직중' : '퇴사'}">재직중</strong>
                <!-- 해설: 서버 모델 값을 HTML 텍스트로 출력한다. 기본적으로 이스케이프되어 안전하다. -->
            </div>
            <div class="status-card">
                <span>평판</span>
                <strong th:text="${@gameService.reputationText(player.reputation)}">0</strong>
                <!-- 해설: 서버 모델 값을 HTML 텍스트로 출력한다. 기본적으로 이스케이프되어 안전하다. -->
                <small class="status-subline">칭호 <b th:text="${player.title}">칭호</b></small>
                <!-- 해설: 서버 모델 값을 HTML 텍스트로 출력한다. 기본적으로 이스케이프되어 안전하다. -->
            </div>
            <div class="status-card date-card">
                <span>날짜</span>
                <strong th:text="${player.month} + '월 ' + ${player.day} + '일'">1월 1일</strong>
                <!-- 해설: 서버 모델 값을 HTML 텍스트로 출력한다. 기본적으로 이스케이프되어 안전하다. -->
                <div class="tick-progress" aria-label="다음 날 진행률">
                    <span id="dayProgress"></span>
                </div>
                <small id="dayProgressText">다음 날 0%</small>
            </div>
        </div>
    </header>

    <div id="flashToast" th:if="${notice}" class="toast show" th:text="${notice}">알림</div>
    <!-- 해설: 서버 모델 값 조건에 따라 이 요소를 보여주거나 숨긴다. -->

    <nav class="city-tabs" aria-label="도시 선택">
        <form method="post" action="/city" th:each="city : ${cities}">
        <!-- 해설: 서버에서 받은 목록을 반복 렌더링한다. 목록 원소마다 이 HTML 블록이 한 번씩 생성된다. -->
            <input type="hidden" name="city" th:value="${city}">
            <button type="submit"
            <!-- 해설: 사용자가 클릭해 폼 제출이나 프론트 동작을 실행하는 버튼이다. -->
                    th:disabled="${cityUnlocks.get(city) != true}"
                    th:classappend="${cityUnlocks.get(city) != true} ? ' locked' : (${viewMode == 'city' && city == player.currentCity} ? ' active' : '')">
                <span th:text="${city}">도시</span>
                <!-- 해설: 서버 모델 값을 HTML 텍스트로 출력한다. 기본적으로 이스케이프되어 안전하다. -->
                <span th:if="${cityUnlocks.get(city) != true}">잠김</span>
                <!-- 해설: 서버 모델 값 조건에 따라 이 요소를 보여주거나 숨긴다. -->
                <span class="repair-count-badge" th:if="${repairCountsByCity.get(city) != null && repairCountsByCity.get(city) > 0}" th:text="${repairCountsByCity.get(city)}">0</span>
                <!-- 해설: 서버 모델 값 조건에 따라 이 요소를 보여주거나 숨긴다. -->
            </button>
        </form>
        <a class="info-tab" th:if="${stockContentUnlocked}" href="/main?view=stocks" th:classappend="${viewMode == 'stocks'} ? ' active'">주식</a>
        <!-- 해설: 서버 모델 값 조건에 따라 이 요소를 보여주거나 숨긴다. -->
        <span class="info-tab locked" th:if="${!stockContentUnlocked}" th:text="'주식 · ' + ${stockContentStatus}">주식</span>
        <!-- 해설: 서버 모델 값 조건에 따라 이 요소를 보여주거나 숨긴다. -->
        <a class="info-tab" href="/info">정보</a>
        <form class="tab-pause-form" method="post" action="/pause/toggle">
        <!-- 해설: POST 폼이다. 버튼 클릭 시 서버 상태를 변경하는 요청을 보낸다. -->
            <input type="hidden" name="redirectView" th:value="${viewMode}">
            <button class="tab-pause-button" type="submit" th:text="${player.paused ? '진행 재개' : '일시정지'}">일시정지</button>
            <!-- 해설: 서버 모델 값을 HTML 텍스트로 출력한다. 기본적으로 이스케이프되어 안전하다. -->
        </form>
    </nav>

    <th:block th:if="${viewMode == 'city'}">
    <!-- 해설: 서버 모델 값 조건에 따라 이 요소를 보여주거나 숨긴다. -->
        <main class="layout">
            <section th:replace="~{fragments/main-city-panel :: cityPanel}"></section>
            <!-- 해설: Thymeleaf fragment를 현재 위치에 끼워 넣는다. 반복되는 화면 조각을 재사용한다. -->
            <aside th:replace="~{fragments/main-secretary-panel :: secretaryPanel}"></aside>
            <!-- 해설: Thymeleaf fragment를 현재 위치에 끼워 넣는다. 반복되는 화면 조각을 재사용한다. -->
        </main>
        <section th:replace="~{fragments/main-market :: market}"></section>
        <!-- 해설: Thymeleaf fragment를 현재 위치에 끼워 넣는다. 반복되는 화면 조각을 재사용한다. -->
        <section th:replace="~{fragments/main-info-grid :: infoGrid}"></section>
        <!-- 해설: Thymeleaf fragment를 현재 위치에 끼워 넣는다. 반복되는 화면 조각을 재사용한다. -->
    </th:block>
    <th:block th:if="${viewMode == 'stocks'}">
    <!-- 해설: 서버 모델 값 조건에 따라 이 요소를 보여주거나 숨긴다. -->
        <main class="stock-layout">
            <section th:replace="~{fragments/main-stock-panel :: stockPanel}"></section>
            <!-- 해설: Thymeleaf fragment를 현재 위치에 끼워 넣는다. 반복되는 화면 조각을 재사용한다. -->
            <aside class="panel stock-record-panel">
                <section class="stock-holding-summary">
                    <h2>보유종목요약</h2>
                    <dl>
                        <div>
                            <dt>보유 종목</dt>
                            <dd th:text="${stockHoldingSummary.holdingCount()} + '개'">0개</dd>
                            <!-- 해설: 서버 모델 값을 HTML 텍스트로 출력한다. 기본적으로 이스케이프되어 안전하다. -->
                        </div>
                        <div>
                            <dt>보유 주식</dt>
                            <dd th:text="${stockHoldingSummary.totalQuantity()} + '주'">0주</dd>
                            <!-- 해설: 서버 모델 값을 HTML 텍스트로 출력한다. 기본적으로 이스케이프되어 안전하다. -->
                        </div>
                        <div>
                            <dt>매입원금</dt>
                            <dd th:text="${stockHoldingSummary.totalCostText()}">0코인</dd>
                            <!-- 해설: 서버 모델 값을 HTML 텍스트로 출력한다. 기본적으로 이스케이프되어 안전하다. -->
                        </div>
                        <div>
                            <dt>평가금액</dt>
                            <dd th:text="${stockHoldingSummary.totalValuationText()}">0코인</dd>
                            <!-- 해설: 서버 모델 값을 HTML 텍스트로 출력한다. 기본적으로 이스케이프되어 안전하다. -->
                        </div>
                        <div>
                            <dt>평가손익</dt>
                            <dd th:class="${stockHoldingSummary.profitDirection()}" th:text="${stockHoldingSummary.totalProfitText()}">0코인</dd>
                            <!-- 해설: 서버 모델 값을 HTML 텍스트로 출력한다. 기본적으로 이스케이프되어 안전하다. -->
                        </div>
                    </dl>
                </section>
                <section class="stock-side-records">
                    <h2>최근 기록</h2>
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
                </section>
            </aside>
        </main>
    </th:block>

    <form class="logout-form" method="post" action="/logout">
    <!-- 해설: POST 폼이다. 버튼 클릭 시 서버 상태를 변경하는 요청을 보낸다. -->
        <button class="secondary" type="submit">로그아웃</button>
        <!-- 해설: 사용자가 클릭해 폼 제출이나 프론트 동작을 실행하는 버튼이다. -->
    </form>

    <div class="toast" id="toast"></div>

    <div th:replace="~{fragments/main-modals :: modals}"></div>
    <!-- 해설: Thymeleaf fragment를 현재 위치에 끼워 넣는다. 반복되는 화면 조각을 재사용한다. -->
</div>

<script type="module" src="/app.js?v=stock-tools-1"></script>
</body>
</html>
```