# main-test-panel.html 코드 주석형 해설

원본 파일: `src/main/resources/templates/fragments/main-test-panel.html`

형식:
- 원본 코드를 그대로 배치한다.
- 필요한 코드 바로 아래에 해설 주석을 붙인다.
- import/package/단순 선언은 과하게 설명하지 않는다.

```html
<!doctype html>
<html lang="ko" xmlns:th="http://www.thymeleaf.org">
<body>
<details th:fragment="testPanel" class="panel test-panel collapsible-panel" data-collapsible-key="test" open>
<!-- 해설: data-* 속성은 JavaScript가 읽을 수 있는 화면 상태나 식별자를 HTML에 심어 둔다. -->
    <!--
    개발용 테스트 패널이다.
    QAController로 POST를 보내 현금/평판/이벤트 조건을 빠르게 조정한다.
    -->
    <summary>테스트</summary>
    <form method="post" action="/test/cash">
    <!-- 해설: POST 폼이다. 버튼 클릭 시 서버 상태를 변경하는 요청을 보낸다. -->
        <button type="submit">현금 1조원 증가</button>
        <!-- 해설: 사용자가 클릭해 폼 제출이나 프론트 동작을 실행하는 버튼이다. -->
    </form>
    <form method="post" action="/test/chances" class="test-controls">
    <!-- 해설: POST 폼이다. 버튼 클릭 시 서버 상태를 변경하는 요청을 보낸다. -->
        <label>
            입주확률
            <input type="number" name="moveInChance" min="0" max="100" th:value="${@gameService.baseMoveInChancePercent(player)}">
        </label>
        <label>
            퇴거확률
            <input type="number" name="moveOutChance" min="0" max="100" th:value="${@gameService.baseMoveOutChancePercent(player)}">
        </label>
        <label>
            수리확률
            <input type="number" name="repairChance" min="0" max="100" th:value="${@gameService.baseRepairRequestChancePercent(player)}">
        </label>
        <button class="secondary" type="submit">확률 적용</button>
        <!-- 해설: 사용자가 클릭해 폼 제출이나 프론트 동작을 실행하는 버튼이다. -->
    </form>
    <form method="post" action="/test/reputation" class="test-controls">
    <!-- 해설: POST 폼이다. 버튼 클릭 시 서버 상태를 변경하는 요청을 보낸다. -->
        <label>
            평판
            <input type="number" name="reputation" min="0" th:value="${player.reputation}">
        </label>
        <button class="secondary" type="submit">평판 적용</button>
        <!-- 해설: 사용자가 클릭해 폼 제출이나 프론트 동작을 실행하는 버튼이다. -->
    </form>
    <form method="post" action="/test/market-news" class="test-controls">
    <!-- 해설: POST 폼이다. 버튼 클릭 시 서버 상태를 변경하는 요청을 보낸다. -->
        <label>
            부동산 이벤트
            <select name="trend">
                <option value="RISE">현재 도시 폭등</option>
                <option value="FALL">현재 도시 폭락</option>
            </select>
        </label>
        <button class="secondary" type="submit">이벤트 발생</button>
        <!-- 해설: 사용자가 클릭해 폼 제출이나 프론트 동작을 실행하는 버튼이다. -->
    </form>
    <form method="post" action="/test/secretary-proficiency" class="test-controls">
    <!-- 해설: POST 폼이다. 버튼 클릭 시 서버 상태를 변경하는 요청을 보낸다. -->
        <label>
            비서
            <select name="secretaryKey">
                <option th:each="secretary : ${secretarySpecs}" th:value="${secretary.key()}" th:text="${secretary.name()} + ' - ' + ${secretary.origin()}">비서</option>
                <!-- 해설: 서버에서 받은 목록을 반복 렌더링한다. 목록 원소마다 이 HTML 블록이 한 번씩 생성된다. -->
            </select>
        </label>
        <label>
            숙련도
            <input type="number" name="proficiency" min="1" max="30" value="1">
        </label>
        <button class="secondary" type="submit">숙련도 적용</button>
        <!-- 해설: 사용자가 클릭해 폼 제출이나 프론트 동작을 실행하는 버튼이다. -->
    </form>
    <div class="test-divider"></div>
    <h3>비서 이벤트 QA</h3>
    <form method="post" action="/test/secretary-event/conditions" class="test-controls">
    <!-- 해설: POST 폼이다. 버튼 클릭 시 서버 상태를 변경하는 요청을 보낸다. -->
        <label>
            비서
            <select name="secretaryKey">
                <option th:each="secretary : ${secretarySpecs}" th:value="${secretary.key()}" th:text="${secretary.name()} + ' - ' + ${secretary.origin()}">비서</option>
                <!-- 해설: 서버에서 받은 목록을 반복 렌더링한다. 목록 원소마다 이 HTML 블록이 한 번씩 생성된다. -->
            </select>
        </label>
        <button class="secondary" type="submit">조건 맞추기</button>
        <!-- 해설: 사용자가 클릭해 폼 제출이나 프론트 동작을 실행하는 버튼이다. -->
    </form>
    <form method="post" action="/test/secretary-event/building" class="test-controls">
    <!-- 해설: POST 폼이다. 버튼 클릭 시 서버 상태를 변경하는 요청을 보낸다. -->
        <label>
            비서
            <select name="secretaryKey">
                <option th:each="secretary : ${secretarySpecs}" th:value="${secretary.key()}" th:text="${secretary.name()} + ' - ' + ${secretary.origin()}">비서</option>
                <!-- 해설: 서버에서 받은 목록을 반복 렌더링한다. 목록 원소마다 이 HTML 블록이 한 번씩 생성된다. -->
            </select>
        </label>
        <button class="secondary" type="submit">입주 세팅</button>
        <!-- 해설: 사용자가 클릭해 폼 제출이나 프론트 동작을 실행하는 버튼이다. -->
    </form>
    <form method="post" action="/test/secretary-event/stage" class="test-controls">
    <!-- 해설: POST 폼이다. 버튼 클릭 시 서버 상태를 변경하는 요청을 보낸다. -->
        <label>
            비서
            <select name="secretaryKey">
                <option th:each="secretary : ${secretarySpecs}" th:value="${secretary.key()}" th:text="${secretary.name()} + ' - ' + ${secretary.origin()}">비서</option>
                <!-- 해설: 서버에서 받은 목록을 반복 렌더링한다. 목록 원소마다 이 HTML 블록이 한 번씩 생성된다. -->
            </select>
        </label>
        <label>
            단계
            <select name="stage">
                <option value="TENANT">입주</option>
                <option value="REQUEST">부탁 모달</option>
                <option value="ACCEPTED">부탁 수락 후</option>
                <option value="HIRE">고용 모달</option>
                <option value="COMPLETED">완료</option>
            </select>
        </label>
        <button class="secondary" type="submit">단계 이동</button>
        <!-- 해설: 사용자가 클릭해 폼 제출이나 프론트 동작을 실행하는 버튼이다. -->
    </form>
    <ul class="event-list" th:if="${!secretaryTenantEvents.isEmpty()}">
    <!-- 해설: 서버 모델 값 조건에 따라 이 요소를 보여주거나 숨긴다. -->
        <li th:each="event : ${secretaryTenantEvents}">
        <!-- 해설: 서버에서 받은 목록을 반복 렌더링한다. 목록 원소마다 이 HTML 블록이 한 번씩 생성된다. -->
            <b th:text="${@gameService.secretarySpec(event.secretaryKey).name()}">비서</b>
            <!-- 해설: 서버 모델 값을 HTML 텍스트로 출력한다. 기본적으로 이스케이프되어 안전하다. -->
            <span th:text="${event.city} + ' · ' + ${event.status}">상태</span>
            <!-- 해설: 서버 모델 값을 HTML 텍스트로 출력한다. 기본적으로 이스케이프되어 안전하다. -->
            <small th:text="${event.building.name}">건물</small>
            <!-- 해설: 서버 모델 값을 HTML 텍스트로 출력한다. 기본적으로 이스케이프되어 안전하다. -->
        </li>
    </ul>
    <p th:if="${secretaryTenantEvents.isEmpty()}">진행 중인 비서 이벤트 없음</p>
    <!-- 해설: 서버 모델 값 조건에 따라 이 요소를 보여주거나 숨긴다. -->
</details>
</body>
</html>
```
