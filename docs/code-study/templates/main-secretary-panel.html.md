# main-secretary-panel.html 코드 주석형 해설

원본 파일: `src/main/resources/templates/fragments/main-secretary-panel.html`

형식:
- 원본 코드를 그대로 배치한다.
- 필요한 코드 바로 아래에 해설 주석을 붙인다.
- import/package/단순 선언은 과하게 설명하지 않는다.

```html
<!doctype html>
<html lang="ko" xmlns:th="http://www.thymeleaf.org">
<body>
<aside th:fragment="secretaryPanel" class="secretary-panel">
    <!--
    비서 관리 패널이다.
    배치된 비서의 현재 효과, 보유 비서 목록, 선물/고용 UI를 한 영역에서 보여준다.
    -->
    <section class="panel secretary-focus">
        <div class="section-head compact">
            <div>
                <h2 th:text="${assignedSecretary == null ? '비서' : @gameService.secretarySpec(assignedSecretary.secretaryKey).name() + ' · ' + @gameService.secretarySpec(assignedSecretary.secretaryKey).origin()}">비서</h2>
                <!-- 해설: 서버 모델 값을 HTML 텍스트로 출력한다. 기본적으로 이스케이프되어 안전하다. -->
                <p th:text="${assignedSecretary == null ? '미배치' : '숙련도 ' + assignedSecretary.proficiency + '/30 · 호감도 ' + assignedSecretary.affinity + '/30'}">미배치</p>
                <!-- 해설: 서버 모델 값을 HTML 텍스트로 출력한다. 기본적으로 이스케이프되어 안전하다. -->
            </div>
            <span class="badge" th:text="${assignedSecretary == null ? '미배정' : @moneyText.format(@gameService.secretarySpec(assignedSecretary.secretaryKey).monthlySalaryForProficiency(assignedSecretary.proficiency))}">미배정</span>
            <!-- 해설: 서버 모델 값을 HTML 텍스트로 출력한다. 기본적으로 이스케이프되어 안전하다. -->
        </div>
        <div class="secretary-visual">
            <div th:if="${assignedSecretary == null}" class="secretary-empty">배치된 비서 없음</div>
            <!-- 해설: 서버 모델 값 조건에 따라 이 요소를 보여주거나 숨긴다. -->
            <img th:if="${assignedSecretary != null}" class="secretary-image" th:src="${@gameService.secretarySpec(assignedSecretary.secretaryKey).imagePath()}" alt="배치 비서 이미지">
            <!-- 해설: 서버 모델 값 조건에 따라 이 요소를 보여주거나 숨긴다. -->
        </div>
        <div class="secretary-info">
            <h3 th:text="${assignedSecretary == null ? '비서 없음' : @gameService.secretarySpec(assignedSecretary.secretaryKey).effect()}">비서 상태</h3>
            <!-- 해설: 서버 모델 값을 HTML 텍스트로 출력한다. 기본적으로 이스케이프되어 안전하다. -->
            <p th:text="${assignedSecretary == null ? '보유 비서 목록에서 도시 배치 가능' : '자동수리 ' + @gameService.secretarySpec(assignedSecretary.secretaryKey).autoCheckDays(assignedSecretary.proficiency) + '일 · ' + @gameService.appliedSecretarySpecialEffectSummary(assignedSecretary)}">비서 능력</p>
            <!-- 해설: 서버 모델 값을 HTML 텍스트로 출력한다. 기본적으로 이스케이프되어 안전하다. -->
            <div class="secretary-ability-actions" th:if="${assignedSecretary != null}">
            <!-- 해설: 서버 모델 값 조건에 따라 이 요소를 보여주거나 숨긴다. -->
                <span class="badge" th:text="${assignedSecretary.canAutoRepair(player.elapsedDays) ? '수리 준비' : '수리 D-' + assignedSecretary.autoRepairCooldownDaysLeft(player.elapsedDays)}">수리 D-0</span>
                <!-- 해설: 서버 모델 값을 HTML 텍스트로 출력한다. 기본적으로 이스케이프되어 안전하다. -->
                <button class="secondary ability-modal-open" type="button" data-ability-modal="assigned-ability-modal">능력</button>
                <!-- 해설: 사용자가 클릭해 폼 제출이나 프론트 동작을 실행하는 버튼이다. -->
            </div>
            <div class="secretary-exp-grid">
                <div class="secretary-exp-card">
                    <div>
                        <span>숙련도 경험치</span>
                        <strong th:text="${assignedSecretary == null ? '0/3' : assignedSecretary.proficiencyExperience + '/' + assignedSecretary.requiredProficiencyExperience}">0/3</strong>
                        <!-- 해설: 서버 모델 값을 HTML 텍스트로 출력한다. 기본적으로 이스케이프되어 안전하다. -->
                    </div>
                    <div class="skill-meter"><span th:style="${assignedSecretary == null ? 'width: 0' : 'width: ' + assignedSecretary.proficiencyExperiencePercent + '%'}"></span></div>
                    <small th:text="${assignedSecretary == null ? '다음 숙련도까지 3' : (assignedSecretary.proficiency >= 30 ? '최대 숙련도' : '다음 숙련도까지 ' + (assignedSecretary.requiredProficiencyExperience - assignedSecretary.proficiencyExperience))}">다음 숙련도까지 3</small>
                    <!-- 해설: 서버 모델 값을 HTML 텍스트로 출력한다. 기본적으로 이스케이프되어 안전하다. -->
                </div>
                <div class="secretary-exp-card affinity">
                    <div>
                        <span>호감도 경험치</span>
                        <strong th:text="${assignedSecretary == null ? '0/3' : assignedSecretary.affinityExperience + '/' + assignedSecretary.requiredAffinityExperience}">0/3</strong>
                        <!-- 해설: 서버 모델 값을 HTML 텍스트로 출력한다. 기본적으로 이스케이프되어 안전하다. -->
                    </div>
                    <div class="skill-meter"><span th:style="${assignedSecretary == null ? 'width: 0' : 'width: ' + assignedSecretary.affinityExperiencePercent + '%'}"></span></div>
                    <small th:text="${assignedSecretary == null ? '다음 호감도까지 3' : (assignedSecretary.affinity >= 30 ? '최대 호감도' : '다음 호감도까지 ' + (assignedSecretary.requiredAffinityExperience - assignedSecretary.affinityExperience))}">다음 호감도까지 3</small>
                    <!-- 해설: 서버 모델 값을 HTML 텍스트로 출력한다. 기본적으로 이스케이프되어 안전하다. -->
                </div>
            </div>
        </div>
        <div class="secretary-panel-actions">
            <button id="secretaryListButton" type="button">보유 비서 목록</button>
            <!-- 해설: 사용자가 클릭해 폼 제출이나 프론트 동작을 실행하는 버튼이다. -->
            <button class="secondary gift-modal-open" type="button" th:disabled="${assignedSecretary == null || player.paused}" th:attr="data-gift-modal=${assignedSecretary == null ? '' : 'assigned-gift-modal'}">선물</button>
            <!-- 해설: 사용자가 클릭해 폼 제출이나 프론트 동작을 실행하는 버튼이다. -->
        </div>
        <div id="assigned-ability-modal" class="image-modal-backdrop ability-modal-backdrop" th:if="${assignedSecretary != null}" hidden>
        <!-- 해설: 서버 모델 값 조건에 따라 이 요소를 보여주거나 숨긴다. -->
            <section class="ability-modal">
                <div class="section-head">
                    <div>
                        <h3 th:text="${@gameService.secretarySpec(assignedSecretary.secretaryKey).name()} + ' 능력'">비서 능력</h3>
                        <!-- 해설: 서버 모델 값을 HTML 텍스트로 출력한다. 기본적으로 이스케이프되어 안전하다. -->
                        <p th:text="'숙련도 ' + ${assignedSecretary.proficiency} + '/30 · 호감도 ' + ${assignedSecretary.affinity} + '/30'">능력 상태</p>
                        <!-- 해설: 서버 모델 값을 HTML 텍스트로 출력한다. 기본적으로 이스케이프되어 안전하다. -->
                    </div>
                    <button class="secondary ability-modal-close" type="button">닫기</button>
                    <!-- 해설: 사용자가 클릭해 폼 제출이나 프론트 동작을 실행하는 버튼이다. -->
                </div>
                <ul class="ability-list">
                    <li th:each="ability : ${@gameService.activeSecretaryAbilitySummaries(assignedSecretary)}" th:text="${ability}">능력</li>
                    <!-- 해설: 서버에서 받은 목록을 반복 렌더링한다. 목록 원소마다 이 HTML 블록이 한 번씩 생성된다. -->
                </ul>
            </section>
        </div>
        <div class="gift-select-popover" id="assigned-gift-modal" th:if="${assignedSecretary != null}" hidden>
        <!-- 해설: 서버 모델 값 조건에 따라 이 요소를 보여주거나 숨긴다. -->
            <div class="section-head">
                <div>
                    <h3 th:text="${@gameService.secretarySpec(assignedSecretary.secretaryKey).name()} + ' 선물'">선물</h3>
                    <!-- 해설: 서버 모델 값을 HTML 텍스트로 출력한다. 기본적으로 이스케이프되어 안전하다. -->
                    <p th:text="'현재 호감도 ' + ${assignedSecretary.affinity} + '/30'">호감도</p>
                    <!-- 해설: 서버 모델 값을 HTML 텍스트로 출력한다. 기본적으로 이스케이프되어 안전하다. -->
                    <p th:text="'호감도 경험치 ' + ${assignedSecretary.affinityExperience} + '/' + ${assignedSecretary.requiredAffinityExperience}">호감도 경험치</p>
                    <!-- 해설: 서버 모델 값을 HTML 텍스트로 출력한다. 기본적으로 이스케이프되어 안전하다. -->
                </div>
                <button class="secondary gift-modal-close" type="button">닫기</button>
                <!-- 해설: 사용자가 클릭해 폼 제출이나 프론트 동작을 실행하는 버튼이다. -->
            </div>
            <form class="gift-give-row confirm-form" method="post" th:each="gift : ${giftItems}" th:action="@{/owned-secretaries/{id}/gifts(id=${assignedSecretary.id})}" th:with="ownedQuantity=${@gameService.ownedGiftQuantity(player, gift)}, maxGiftQuantity=${@gameService.maxGiftQuantityForSecretary(player, assignedSecretary, gift)}, giftUsable=${maxGiftQuantity > 0}" th:attr="data-confirm-title='선물 확인', data-confirm-message=${@gameService.secretarySpec(assignedSecretary.secretaryKey).name()} + '에게 ' + ${gift.name()} + ' 선물?'">
            <!-- 해설: 서버에서 받은 목록을 반복 렌더링한다. 목록 원소마다 이 HTML 블록이 한 번씩 생성된다. -->
                <img class="shop-image gift-image" th:src="${gift.imagePath()}" th:alt="${gift.name()} + ' 이미지'">
                <!-- 해설: 서버 모델이나 URL 표현식으로 링크/이미지/폼 액션 경로를 만든다. -->
                <strong th:text="${gift.name()}">선물</strong>
                <!-- 해설: 서버 모델 값을 HTML 텍스트로 출력한다. 기본적으로 이스케이프되어 안전하다. -->
                <span th:text="'보유 ' + ${ownedQuantity}">보유</span>
                <!-- 해설: 서버 모델 값을 HTML 텍스트로 출력한다. 기본적으로 이스케이프되어 안전하다. -->
                <span>호감도 +1</span>
                <input type="hidden" name="giftKey" th:value="${gift.key()}">
                <input class="quantity-input" type="number" name="quantity" min="1" th:max="${maxGiftQuantity}" value="1" th:disabled="${!giftUsable}">
                <button class="secondary" type="submit" th:disabled="${!giftUsable}" th:text="${ownedQuantity <= 0 ? '미보유' : (giftUsable ? '선물' : '구간불가')}">선물</button>
                <!-- 해설: 서버 모델 값을 HTML 텍스트로 출력한다. 기본적으로 이스케이프되어 안전하다. -->
            </form>
        </div>
    </section>
</aside>
</body>
</html>
```