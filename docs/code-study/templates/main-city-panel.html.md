# main-city-panel.html 코드 주석형 해설

원본 파일: `src/main/resources/templates/fragments/main-city-panel.html`

형식:
- 원본 코드를 그대로 배치한다.
- 필요한 코드 바로 아래에 해설 주석을 붙인다.
- import/package/단순 선언은 과하게 설명하지 않는다.

```html
<!doctype html>
<html lang="ko" xmlns:th="http://www.thymeleaf.org">
<body>
<section th:fragment="cityPanel" class="city-panel">
    <!--
    도시 보유 건물 패널이다.
    buildings는 OwnedBuilding 목록이며 입주, 수리, 매각 가능 상태는 엔티티 상태와 서비스 계산값으로 표시된다.
    -->
    <div class="section-head">
        <div>
            <h2 th:text="${player.currentCity}">청주</h2>
            <!-- 해설: 서버 모델 값을 HTML 텍스트로 출력한다. 기본적으로 이스케이프되어 안전하다. -->
            <p>
                보유 건물 <span th:text="${buildings.size()}">0</span>/8 ·
                <!-- 해설: 서버 모델 값을 HTML 텍스트로 출력한다. 기본적으로 이스케이프되어 안전하다. -->
                입주 <span th:text="${@gameService.effectiveMoveInChancePercentText(player, player.currentCity)}">35%</span> ·
                <!-- 해설: 서버 모델 값을 HTML 텍스트로 출력한다. 기본적으로 이스케이프되어 안전하다. -->
                퇴거 <span th:text="${@gameService.effectiveMoveOutChancePercentText(player, player.currentCity)}">25%</span> ·
                <!-- 해설: 서버 모델 값을 HTML 텍스트로 출력한다. 기본적으로 이스케이프되어 안전하다. -->
                수리 <span th:text="${@gameService.effectiveRepairRequestChancePercentText(player, player.currentCity)}">35%</span>
                <!-- 해설: 서버 모델 값을 HTML 텍스트로 출력한다. 기본적으로 이스케이프되어 안전하다. -->
                <span th:if="${@gameService.rentBonusPercent(player, player.currentCity) > 0}" th:text="' · 월세 +' + ${@gameService.rentBonusPercentText(player, player.currentCity)}">월세</span>
                <!-- 해설: 서버 모델 값 조건에 따라 이 요소를 보여주거나 숨긴다. -->
                <span th:if="${@gameService.buildingWaitReductionPercent(player, player.currentCity) > 0}" th:text="' · 건물대기시간 -' + ${@gameService.buildingWaitReductionPercentText(player, player.currentCity)}">대기</span>
                <!-- 해설: 서버 모델 값 조건에 따라 이 요소를 보여주거나 숨긴다. -->
                <span th:if="${!@gameService.marketNewsStatusText(player, player.currentCity).isBlank()}" th:text="' · ' + ${@gameService.marketNewsStatusText(player, player.currentCity)}">뉴스</span>
                <!-- 해설: 서버 모델 값 조건에 따라 이 요소를 보여주거나 숨긴다. -->
            </p>
        </div>
        <div class="quick-actions">
            <button id="sideJobBtn" type="button" th:disabled="${player.paused}" th:text="${player.paused ? '일시정지' : '부업'}">부업</button>
            <!-- 해설: 서버 모델 값을 HTML 텍스트로 출력한다. 기본적으로 이스케이프되어 안전하다. -->
            <form method="post" action="/resign" th:if="${player.employed}">
            <!-- 해설: 서버 모델 값 조건에 따라 이 요소를 보여주거나 숨긴다. -->
                <button class="danger-button" type="submit" th:disabled="${player.paused || !player.canResign()}" th:text="${player.paused ? '일시정지' : (player.canResign() ? '퇴사' : '퇴사 ' + player.ddayText(player.daysUntilResignAvailable()))}">퇴사</button>
                <!-- 해설: 서버 모델 값을 HTML 텍스트로 출력한다. 기본적으로 이스케이프되어 안전하다. -->
            </form>
        </div>
    </div>

    <div class="skyline" th:classappend="' city-bg-' + ${@gameService.cityBackgroundClass(player.currentCity)}">
        <div class="sun"></div>
        <div class="cloud cloud-a"></div>
        <div class="cloud cloud-b"></div>

        <article th:each="building : ${buildings}" class="building-slot owned" th:classappend="${buildingStat.index == 0} ? ' selected'" th:attr="data-building=${building.name}, data-building-id=${building.id}">
        <!-- 해설: 서버에서 받은 목록을 반복 렌더링한다. 목록 원소마다 이 HTML 블록이 한 번씩 생성된다. -->
            <span th:if="${building.secretaryResident}" class="sale-lock-badge">판매불가</span>
            <!-- 해설: 서버 모델 값 조건에 따라 이 요소를 보여주거나 숨긴다. -->
            <span th:if="${building.tenantProtectedDaysLeft(player.elapsedDays) > 0}" class="protection" th:text="'보호 D-' + ${building.tenantProtectedDaysLeft(player.elapsedDays)}">보호</span>
            <!-- 해설: 서버 모델 값 조건에 따라 이 요소를 보여주거나 숨긴다. -->
            <form th:if="${building.repairRequested}" method="post" th:action="@{/buildings/{id}/repair(id=${building.id})}">
            <!-- 해설: 서버 모델 값 조건에 따라 이 요소를 보여주거나 숨긴다. -->
                <button class="repair" type="submit" th:disabled="${player.paused || player.cash < building.repairCost()}" th:text="${player.paused ? '일시정지' : (player.cash < building.repairCost() ? '수리비 부족' : '수리요청')}">수리요청</button>
                <!-- 해설: 서버 모델 값을 HTML 텍스트로 출력한다. 기본적으로 이스케이프되어 안전하다. -->
            </form>
            <img class="building-image" th:src="${@gameService.buildingImagePath(building)}" th:alt="${building.name} + ' 이미지'">
            <!-- 해설: 서버 모델이나 URL 표현식으로 링크/이미지/폼 액션 경로를 만든다. -->
            <strong th:text="${building.name} + ' #' + ${buildingStat.count}">건물명</strong>
            <!-- 해설: 서버 모델 값을 HTML 텍스트로 출력한다. 기본적으로 이스케이프되어 안전하다. -->
            <span th:text="${(@gameService.secretaryTenantStatusText(building).isBlank() ? (building.occupied ? (building.protectedTenant ? '후배 거주중' : '입주중') : '공실') : @gameService.secretaryTenantStatusText(building)) + ' · ' + (@gameService.rentWaivedBySecretaryEvent(player, building) ? '월세 감면' : '월세 ' + @moneyText.format(@gameService.effectiveMonthlyRent(player, building)))}">상태</span>
            <!-- 해설: 서버 모델 값을 HTML 텍스트로 출력한다. 기본적으로 이스케이프되어 안전하다. -->
        </article>

        <button th:each="slot : ${#numbers.sequence(1, 8 - buildings.size())}" class="building-slot empty" th:if="${buildings.size() < 8}">
        <!-- 해설: 서버에서 받은 목록을 반복 렌더링한다. 목록 원소마다 이 HTML 블록이 한 번씩 생성된다. -->
            <div class="empty-plus">+</div>
            <strong>빈 부지</strong>
            <span>매물 구매 가능</span>
        </button>
    </div>
</section>
</body>
</html>
```