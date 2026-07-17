# info.html 코드 주석형 해설

원본 파일: `src/main/resources/templates/info.html`

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
    <title>건물주이야기 정보</title>
    <link rel="stylesheet" href="/styles.css">
</head>
<body>
<!--
정보 화면이다.
게임 규칙, 비서 목록, 도시 해금 조건, 건물 가격/월세처럼 플레이어가 참고할 정적 데이터를 보여준다.
-->
<div class="app">
    <header class="topbar">
        <div class="brand">
            <span class="logo">B</span>
            <div>
                <h1>정보</h1>
                <p>정보 확인 중에는 시간이 흐르지 않음</p>
            </div>
        </div>

        <div class="status-grid">
            <div class="status-card">
                <span>현금</span>
                <strong class="money" th:text="${@moneyText.format(player.cash)}">0원</strong>
                <!-- 해설: 서버 모델 값을 HTML 텍스트로 출력한다. 기본적으로 이스케이프되어 안전하다. -->
                <small class="status-subline">대출 <b th:text="${@moneyText.format(loanRemainingRepayment)}">0원</b></small>
                <!-- 해설: 서버 모델 값을 HTML 텍스트로 출력한다. 기본적으로 이스케이프되어 안전하다. -->
            </div>
            <div class="status-card">
                <span>총 월세</span>
                <strong class="money" th:text="${@moneyText.format(totalMonthlyRent)}">0원</strong>
                <!-- 해설: 서버 모델 값을 HTML 텍스트로 출력한다. 기본적으로 이스케이프되어 안전하다. -->
            </div>
            <div class="status-card">
                <span>코인</span>
                <strong class="money" th:text="${@gameService.stockCoinText(player.coin)}">0코인</strong>
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
                <small>정보 확인 중 정지</small>
            </div>
        </div>
    </header>

    <nav class="city-tabs" aria-label="도시 선택">
        <form method="post" action="/city" th:each="city : ${cities}">
        <!-- 해설: 서버에서 받은 목록을 반복 렌더링한다. 목록 원소마다 이 HTML 블록이 한 번씩 생성된다. -->
            <input type="hidden" name="city" th:value="${city}">
            <button type="submit"
            <!-- 해설: 사용자가 클릭해 폼 제출이나 프론트 동작을 실행하는 버튼이다. -->
                    th:disabled="${cityUnlocks.get(city) != true}"
                    th:classappend="${city == player.currentCity} ? ' active' : (${cityUnlocks.get(city) != true} ? ' locked' : '')">
                <span th:text="${city}">도시</span>
                <!-- 해설: 서버 모델 값을 HTML 텍스트로 출력한다. 기본적으로 이스케이프되어 안전하다. -->
                <span th:if="${cityUnlocks.get(city) != true}">잠김</span>
                <!-- 해설: 서버 모델 값 조건에 따라 이 요소를 보여주거나 숨긴다. -->
                <span class="repair-count-badge" th:if="${repairCountsByCity.get(city) != null && repairCountsByCity.get(city) > 0}" th:text="${repairCountsByCity.get(city)}">0</span>
                <!-- 해설: 서버 모델 값 조건에 따라 이 요소를 보여주거나 숨긴다. -->
            </button>
        </form>
        <a class="info-tab" th:if="${stockContentUnlocked}" href="/main?view=stocks">주식</a>
        <!-- 해설: 서버 모델 값 조건에 따라 이 요소를 보여주거나 숨긴다. -->
        <span class="info-tab locked" th:if="${!stockContentUnlocked}" th:text="'주식 · ' + ${stockContentStatus}">주식</span>
        <!-- 해설: 서버 모델 값 조건에 따라 이 요소를 보여주거나 숨긴다. -->
        <a class="info-tab active" href="/info">정보</a>
    </nav>

    <main class="info-page">
        <section class="panel">
            <div class="section-head">
                <div>
                    <h2>비서 목록</h2>
                    <p>숙련도 범위 1~30 · 최종 강화 시 성능 격차 축소 예정</p>
                </div>
            </div>
            <div class="secretary-list">
                <article class="secretary-card" th:each="secretary : ${secretarySpecs}">
                <!-- 해설: 서버에서 받은 목록을 반복 렌더링한다. 목록 원소마다 이 HTML 블록이 한 번씩 생성된다. -->
                    <button class="image-open-button" type="button" th:attr="data-full-image=${secretary.imagePath()}, data-image-title=${secretary.name()} + ' - ' + ${secretary.origin()}">
                    <!-- 해설: 사용자가 클릭해 폼 제출이나 프론트 동작을 실행하는 버튼이다. -->
                        <img class="secretary-thumb" th:src="${secretary.imagePath()}" th:alt="${secretary.name()} + ' 비서 이미지'">
                        <!-- 해설: 서버 모델이나 URL 표현식으로 링크/이미지/폼 액션 경로를 만든다. -->
                    </button>
                    <div>
                        <h3>
                            <span th:text="${secretary.name()} + ' - ' + ${secretary.origin()}">비서명</span>
                            <!-- 해설: 서버 모델 값을 HTML 텍스트로 출력한다. 기본적으로 이스케이프되어 안전하다. -->
                            <button class="help-dot" type="button" th:attr="aria-controls='info-secretary-hire-' + ${secretary.key()}">?</button>
                            <!-- 해설: 사용자가 클릭해 폼 제출이나 프론트 동작을 실행하는 버튼이다. -->
                        </h3>
                        <div class="help-detail help-popover" th:id="'info-secretary-hire-' + ${secretary.key()}" hidden>
                            <p th:text="'입주조건: ' + ${@gameService.secretaryMoveInConditionText(secretary)}">입주조건</p>
                            <!-- 해설: 서버 모델 값을 HTML 텍스트로 출력한다. 기본적으로 이스케이프되어 안전하다. -->
                            <p th:text="'고용조건: ' + ${@gameService.secretaryHireConditionText(secretary)}">고용조건</p>
                            <!-- 해설: 서버 모델 값을 HTML 텍스트로 출력한다. 기본적으로 이스케이프되어 안전하다. -->
                        </div>
                        <dl>
                            <div>
                                <dt>월급 <button class="help-dot" type="button" th:attr="aria-controls='info-secretary-salary-' + ${secretary.key()}">?</button></dt>
                                <dd th:text="${@moneyText.format(secretary.monthlySalaryForProficiency(secretary.baseProficiency()))}">월급</dd>
                                <!-- 해설: 서버 모델 값을 HTML 텍스트로 출력한다. 기본적으로 이스케이프되어 안전하다. -->
                            </div>
                            <div class="help-detail help-popover dl-detail" th:id="'info-secretary-salary-' + ${secretary.key()}" hidden>
                                <p th:text="${secretary.salaryDetail()}">월급 상세</p>
                                <!-- 해설: 서버 모델 값을 HTML 텍스트로 출력한다. 기본적으로 이스케이프되어 안전하다. -->
                            </div>
                            <div><dt>기본 효과</dt><dd th:text="${secretary.effect()}">효과</dd></div>
                            <!-- 해설: 서버 모델 값을 HTML 텍스트로 출력한다. 기본적으로 이스케이프되어 안전하다. -->
                            <div><dt>특수효과</dt><dd th:text="${secretary.specialEffectSummary()}">특수효과</dd></div>
                            <!-- 해설: 서버 모델 값을 HTML 텍스트로 출력한다. 기본적으로 이스케이프되어 안전하다. -->
                            <div>
                                <dt>숙련도 <button class="help-dot" type="button" th:attr="aria-controls='info-secretary-growth-' + ${secretary.key()}">?</button></dt>
                                <dd th:text="${secretary.baseProficiency()} + '/30'">숙련도</dd>
                                <!-- 해설: 서버 모델 값을 HTML 텍스트로 출력한다. 기본적으로 이스케이프되어 안전하다. -->
                            </div>
                            <div class="help-detail help-popover dl-detail" th:id="'info-secretary-growth-' + ${secretary.key()}" hidden>
                                <p th:text="${secretary.growthDetail()}">숙련도 상세</p>
                                <!-- 해설: 서버 모델 값을 HTML 텍스트로 출력한다. 기본적으로 이스케이프되어 안전하다. -->
                            </div>
                            <div>
                                <dt>호감도 <button class="help-dot" type="button" th:attr="aria-controls='info-secretary-affinity-' + ${secretary.key()}">?</button></dt>
                                <dd>1/30</dd>
                            </div>
                            <div class="help-detail help-popover dl-detail" th:id="'info-secretary-affinity-' + ${secretary.key()}" hidden>
                                <p th:text="${secretary.specialEffectDetail()}">호감도 상세</p>
                                <!-- 해설: 서버 모델 값을 HTML 텍스트로 출력한다. 기본적으로 이스케이프되어 안전하다. -->
                            </div>
                        </dl>
                    </div>
                </article>
            </div>
        </section>

        <section class="panel">
            <div class="section-head">
                <div>
                    <h2>칭호와 해금 조건</h2>
                    <p>평판 달성 시 칭호와 매물 해금이 진행됨</p>
                </div>
            </div>
            <div class="table-wrap">
                <table>
                    <thead>
                    <tr>
                        <th>칭호</th>
                        <th>필요 평판</th>
                        <th>추가 조건</th>
                        <th>해금</th>
                    </tr>
                    </thead>
                    <tbody>
                    <tr th:each="tier : ${reputationTiers}">
                    <!-- 해설: 서버에서 받은 목록을 반복 렌더링한다. 목록 원소마다 이 HTML 블록이 한 번씩 생성된다. -->
                        <td th:text="${tier.title()}">칭호</td>
                        <!-- 해설: 서버 모델 값을 HTML 텍스트로 출력한다. 기본적으로 이스케이프되어 안전하다. -->
                        <td th:text="${@gameService.reputationText(tier.requiredReputation())}">0</td>
                        <!-- 해설: 서버 모델 값을 HTML 텍스트로 출력한다. 기본적으로 이스케이프되어 안전하다. -->
                        <td th:text="${tier.requiresResignation() ? '퇴사 필요' : '-'}">-</td>
                        <!-- 해설: 서버 모델 값을 HTML 텍스트로 출력한다. 기본적으로 이스케이프되어 안전하다. -->
                        <td th:text="${tier.unlockLabel().isEmpty() ? '-' : tier.unlockLabel()}">해금</td>
                        <!-- 해설: 서버 모델 값을 HTML 텍스트로 출력한다. 기본적으로 이스케이프되어 안전하다. -->
                    </tr>
                    </tbody>
                </table>
            </div>
        </section>

        <section class="panel">
            <div class="section-head">
                <div>
                    <h2>건물 가격과 월세</h2>
                    <p>월세는 매물 평가와 무관하게 고정 · 판매가는 판매 시점 평가 기준</p>
                </div>
            </div>
            <div class="table-wrap">
                <table>
                    <thead>
                    <tr>
                        <th>도시</th>
                        <th>순번</th>
                        <th>건물</th>
                        <th>시장가</th>
                        <th>월세</th>
                        <th>거래 쿨타임</th>
                    </tr>
                    </thead>
                    <tbody>
                    <tr th:each="building : ${buildingSpecs}">
                    <!-- 해설: 서버에서 받은 목록을 반복 렌더링한다. 목록 원소마다 이 HTML 블록이 한 번씩 생성된다. -->
                        <td th:text="${building.city()}">도시</td>
                        <!-- 해설: 서버 모델 값을 HTML 텍스트로 출력한다. 기본적으로 이스케이프되어 안전하다. -->
                        <td th:text="${building.slot()}">1</td>
                        <!-- 해설: 서버 모델 값을 HTML 텍스트로 출력한다. 기본적으로 이스케이프되어 안전하다. -->
                        <td th:text="${building.name()}">건물</td>
                        <!-- 해설: 서버 모델 값을 HTML 텍스트로 출력한다. 기본적으로 이스케이프되어 안전하다. -->
                        <td th:text="${@moneyText.format(building.marketPrice())}">시장가</td>
                        <!-- 해설: 서버 모델 값을 HTML 텍스트로 출력한다. 기본적으로 이스케이프되어 안전하다. -->
                        <td th:text="${@moneyText.format(building.monthlyRent())}">월세</td>
                        <!-- 해설: 서버 모델 값을 HTML 텍스트로 출력한다. 기본적으로 이스케이프되어 안전하다. -->
                        <td th:text="${building.tradeCooldownDays()} + '일'">쿨타임</td>
                        <!-- 해설: 서버 모델 값을 HTML 텍스트로 출력한다. 기본적으로 이스케이프되어 안전하다. -->
                    </tr>
                    </tbody>
                </table>
            </div>
        </section>

        <section class="panel">
            <div class="section-head">
                <div>
                    <h2>주식 종목</h2>
                    <p>서울 진출 후 개방 · 업계별 호황/불황 이벤트 추가 예정</p>
                </div>
            </div>
            <div class="table-wrap">
                <table>
                    <thead>
                    <tr>
                        <th>업계</th>
                        <th>성격</th>
                        <th>기업</th>
                        <th>기준가</th>
                    </tr>
                    </thead>
                    <tbody>
                    <tr th:each="stock : ${stockSpecs}">
                    <!-- 해설: 서버에서 받은 목록을 반복 렌더링한다. 목록 원소마다 이 HTML 블록이 한 번씩 생성된다. -->
                        <td th:text="${stock.industry()}">업계</td>
                        <!-- 해설: 서버 모델 값을 HTML 텍스트로 출력한다. 기본적으로 이스케이프되어 안전하다. -->
                        <td th:text="${stock.riskType().label()}">성격</td>
                        <!-- 해설: 서버 모델 값을 HTML 텍스트로 출력한다. 기본적으로 이스케이프되어 안전하다. -->
                        <td th:text="${stock.name()}">기업</td>
                        <!-- 해설: 서버 모델 값을 HTML 텍스트로 출력한다. 기본적으로 이스케이프되어 안전하다. -->
                        <td th:text="${@gameService.stockCoinText(stock.basePrice())}">가격</td>
                        <!-- 해설: 서버 모델 값을 HTML 텍스트로 출력한다. 기본적으로 이스케이프되어 안전하다. -->
                    </tr>
                    </tbody>
                </table>
            </div>
        </section>
    </main>

    <div class="image-modal-backdrop" id="imageModal" hidden>
        <section class="image-modal">
            <div class="section-head compact">
                <h2 id="imageModalTitle">이미지</h2>
                <button class="secondary" id="imageModalClose" type="button">닫기</button>
                <!-- 해설: 사용자가 클릭해 폼 제출이나 프론트 동작을 실행하는 버튼이다. -->
            </div>
            <img id="imageModalImg" src="" alt="원본 이미지">
        </section>
    </div>
</div>
<script type="module" src="/app.js"></script>
</body>
</html>
```