# main-modals.html 코드 주석형 해설

원본 파일: `src/main/resources/templates/fragments/main-modals.html`

형식:
- 원본 코드를 그대로 배치한다.
- 필요한 코드 바로 아래에 해설 주석을 붙인다.
- import/package/단순 선언은 과하게 설명하지 않는다.

```html
<!doctype html>
<html lang="ko" xmlns:th="http://www.thymeleaf.org">
<body>
<div th:fragment="modals">
    <!--
    메인 화면에서 재사용하는 모달 묶음이다.
    서버 이벤트, 비서 상세, 선물 선택, 확인 대화상자처럼 화면 위에 떠야 하는 UI를 한 fragment로 관리한다.
    -->
    <div id="secretaryModal" class="image-modal-backdrop" hidden>
        <section class="secretary-modal">
            <div class="section-head">
                <div>
                    <h2>보유 비서 목록</h2>
                    <p>고용 조건을 달성한 비서는 여기서 고용 가능</p>
                </div>
                <button id="secretaryModalClose" type="button">닫기</button>
                <!-- 해설: 사용자가 클릭해 폼 제출이나 프론트 동작을 실행하는 버튼이다. -->
            </div>
            <div class="secretary-list">
                <article class="secretary-card" th:each="secretary : ${secretarySpecs}"
                <!-- 해설: 서버에서 받은 목록을 반복 렌더링한다. 목록 원소마다 이 HTML 블록이 한 번씩 생성된다. -->
                         th:with="owned=${@gameService.ownedSecretary(player, secretary).orElse(null)}, secretaryOwned=${owned != null}, secretaryHireable=${@gameService.canHireSecretary(player, secretary)}"
                         th:classappend="${!secretaryOwned && !secretaryHireable} ? ' locked-secretary'">
                    <img class="secretary-thumb" th:src="${secretary.imagePath()}" th:alt="${secretary.name()} + ' 비서 이미지'">
                    <!-- 해설: 서버 모델이나 URL 표현식으로 링크/이미지/폼 액션 경로를 만든다. -->
                    <div>
                        <div class="secretary-status-row">
                            <span class="badge" th:classappend="${secretaryOwned} ? ' good'" th:text="${secretaryOwned ? (owned.assignedCity == null ? '보유중 · 미배치' : '보유중 · ' + owned.assignedCity + ' 배치') : (secretaryHireable ? '고용 가능' : '조건 미달')}">상태</span>
                            <!-- 해설: 서버 모델 값을 HTML 텍스트로 출력한다. 기본적으로 이스케이프되어 안전하다. -->
                            <span class="badge" th:text="'숙련도 ' + ${secretaryOwned ? owned.proficiency : secretary.baseProficiency()} + '/30 · 호감도 ' + ${secretaryOwned ? owned.affinity : 1} + '/30'">숙련도</span>
                            <!-- 해설: 서버 모델 값을 HTML 텍스트로 출력한다. 기본적으로 이스케이프되어 안전하다. -->
                        </div>
                        <h3>
                            <span th:text="${secretary.name()} + ' - ' + ${secretary.origin()}">비서명</span>
                            <!-- 해설: 서버 모델 값을 HTML 텍스트로 출력한다. 기본적으로 이스케이프되어 안전하다. -->
                            <button class="help-dot" type="button" th:attr="aria-controls='main-secretary-hire-' + ${secretary.key()}">?</button>
                            <!-- 해설: 사용자가 클릭해 폼 제출이나 프론트 동작을 실행하는 버튼이다. -->
                        </h3>
                        <div class="help-detail help-popover" th:id="'main-secretary-hire-' + ${secretary.key()}" hidden>
                            <p th:text="'입주조건: ' + ${@gameService.secretaryMoveInConditionText(secretary)}">입주조건</p>
                            <!-- 해설: 서버 모델 값을 HTML 텍스트로 출력한다. 기본적으로 이스케이프되어 안전하다. -->
                            <p th:text="'고용조건: ' + ${@gameService.secretaryHireConditionText(secretary)}">고용조건</p>
                            <!-- 해설: 서버 모델 값을 HTML 텍스트로 출력한다. 기본적으로 이스케이프되어 안전하다. -->
                        </div>
                        <dl>
                            <div>
                                <dt>월급 <button class="help-dot" type="button" th:attr="aria-controls='main-secretary-salary-' + ${secretary.key()}">?</button></dt>
                                <dd th:text="${@moneyText.format(secretary.monthlySalaryForProficiency(secretaryOwned ? owned.proficiency : secretary.baseProficiency()))}">월급</dd>
                                <!-- 해설: 서버 모델 값을 HTML 텍스트로 출력한다. 기본적으로 이스케이프되어 안전하다. -->
                            </div>
                            <div class="help-detail help-popover dl-detail" th:id="'main-secretary-salary-' + ${secretary.key()}" hidden>
                                <p th:text="${secretary.salaryDetail()}">월급 상세</p>
                                <!-- 해설: 서버 모델 값을 HTML 텍스트로 출력한다. 기본적으로 이스케이프되어 안전하다. -->
                            </div>
                            <div><dt>기본 효과</dt><dd th:text="${secretary.effect()}">효과</dd></div>
                            <!-- 해설: 서버 모델 값을 HTML 텍스트로 출력한다. 기본적으로 이스케이프되어 안전하다. -->
                            <div><dt>특수효과</dt><dd th:text="${secretary.specialEffectSummary()}">특수효과</dd></div>
                            <!-- 해설: 서버 모델 값을 HTML 텍스트로 출력한다. 기본적으로 이스케이프되어 안전하다. -->
                            <div>
                                <dt>숙련도 <button class="help-dot" type="button" th:attr="aria-controls='main-secretary-growth-' + ${secretary.key()}">?</button></dt>
                                <dd th:text="${secretaryOwned ? owned.proficiency : secretary.baseProficiency()} + '/30'">숙련도</dd>
                                <!-- 해설: 서버 모델 값을 HTML 텍스트로 출력한다. 기본적으로 이스케이프되어 안전하다. -->
                            </div>
                            <div class="help-detail help-popover dl-detail" th:id="'main-secretary-growth-' + ${secretary.key()}" hidden>
                                <p th:text="${secretary.growthDetail()}">숙련도 상세</p>
                                <!-- 해설: 서버 모델 값을 HTML 텍스트로 출력한다. 기본적으로 이스케이프되어 안전하다. -->
                            </div>
                            <div>
                                <dt>호감도 <button class="help-dot" type="button" th:attr="aria-controls='main-secretary-affinity-' + ${secretary.key()}">?</button></dt>
                                <dd th:text="${secretaryOwned ? owned.affinity : 1} + '/30'">1/30</dd>
                                <!-- 해설: 서버 모델 값을 HTML 텍스트로 출력한다. 기본적으로 이스케이프되어 안전하다. -->
                            </div>
                            <div class="help-detail help-popover dl-detail" th:id="'main-secretary-affinity-' + ${secretary.key()}" hidden>
                                <p th:text="${secretary.specialEffectDetail()}">호감도 상세</p>
                                <!-- 해설: 서버 모델 값을 HTML 텍스트로 출력한다. 기본적으로 이스케이프되어 안전하다. -->
                            </div>
                        </dl>
                        <form method="post" th:action="@{/secretaries/{key}/hire(key=${secretary.key()})}" th:if="${!secretaryOwned}">
                        <!-- 해설: 서버 모델 값 조건에 따라 이 요소를 보여주거나 숨긴다. -->
                            <button type="submit" th:disabled="${player.paused || !secretaryHireable}" th:text="${player.paused ? '일시정지' : '고용'}">고용</button>
                            <!-- 해설: 서버 모델 값을 HTML 텍스트로 출력한다. 기본적으로 이스케이프되어 안전하다. -->
                        </form>
                        <div class="secretary-assignment" th:if="${secretaryOwned}">
                        <!-- 해설: 서버 모델 값 조건에 따라 이 요소를 보여주거나 숨긴다. -->
                            <div class="secretary-action-row">
                                <form method="post" th:action="@{/owned-secretaries/{id}/assign(id=${owned.id})}">
                                <!-- 해설: 서버 모델이나 URL 표현식으로 링크/이미지/폼 액션 경로를 만든다. -->
                                    <select name="city">
                                        <option th:each="city : ${cities}" th:if="${cityUnlocks.get(city) == true}" th:value="${city}" th:text="${city}" th:selected="${owned.assignedCity == city}" th:disabled="${!@gameService.canAssignSecretaryToCity(player, owned, city)}">도시</option>
                                        <!-- 해설: 서버에서 받은 목록을 반복 렌더링한다. 목록 원소마다 이 HTML 블록이 한 번씩 생성된다. -->
                                    </select>
                                    <button class="secondary" type="submit" th:disabled="${player.paused}" th:text="${player.paused ? '일시정지' : '배치'}">배치</button>
                                    <!-- 해설: 서버 모델 값을 HTML 텍스트로 출력한다. 기본적으로 이스케이프되어 안전하다. -->
                                </form>
                                <button class="secondary gift-modal-open" type="button" th:disabled="${player.paused}" th:attr="data-gift-modal='gift-modal-' + ${owned.id}">선물</button>
                                <!-- 해설: 사용자가 클릭해 폼 제출이나 프론트 동작을 실행하는 버튼이다. -->
                            </div>
                            <form method="post" th:action="@{/owned-secretaries/{id}/unassign(id=${owned.id})}" th:if="${owned.assignedCity != null}">
                            <!-- 해설: 서버 모델 값 조건에 따라 이 요소를 보여주거나 숨긴다. -->
                                <button class="secondary" type="submit" th:disabled="${player.paused}" th:text="${player.paused ? '일시정지' : '배치 제외'}">배치 제외</button>
                                <!-- 해설: 서버 모델 값을 HTML 텍스트로 출력한다. 기본적으로 이스케이프되어 안전하다. -->
                            </form>
                        </div>
                        <div class="gift-select-popover" th:if="${secretaryOwned}" th:id="'gift-modal-' + ${owned.id}" hidden>
                        <!-- 해설: 서버 모델 값 조건에 따라 이 요소를 보여주거나 숨긴다. -->
                            <div class="section-head">
                                <div>
                                    <h3 th:text="${secretary.name()} + ' 선물'">선물</h3>
                                    <!-- 해설: 서버 모델 값을 HTML 텍스트로 출력한다. 기본적으로 이스케이프되어 안전하다. -->
                                    <p th:text="'현재 호감도 ' + ${owned.affinity} + '/30'">호감도</p>
                                    <!-- 해설: 서버 모델 값을 HTML 텍스트로 출력한다. 기본적으로 이스케이프되어 안전하다. -->
                                    <p th:text="'호감도 경험치 ' + ${owned.affinityExperience} + '/' + ${owned.requiredAffinityExperience}">호감도 경험치</p>
                                    <!-- 해설: 서버 모델 값을 HTML 텍스트로 출력한다. 기본적으로 이스케이프되어 안전하다. -->
                                </div>
                                <button class="secondary gift-modal-close" type="button">닫기</button>
                                <!-- 해설: 사용자가 클릭해 폼 제출이나 프론트 동작을 실행하는 버튼이다. -->
                            </div>
                            <form class="gift-give-row confirm-form" method="post" th:each="gift : ${giftItems}" th:action="@{/owned-secretaries/{id}/gifts(id=${owned.id})}" th:with="ownedQuantity=${@gameService.ownedGiftQuantity(player, gift)}, maxGiftQuantity=${@gameService.maxGiftQuantityForSecretary(player, owned, gift)}, giftUsable=${maxGiftQuantity > 0}" th:attr="data-confirm-title='선물 확인', data-confirm-message=${secretary.name()} + '에게 ' + ${gift.name()} + ' 선물?'">
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
                    </div>
                    <div class="secretary-lock-overlay" th:if="${!secretaryOwned && !secretaryHireable}">
                    <!-- 해설: 서버 모델 값 조건에 따라 이 요소를 보여주거나 숨긴다. -->
                        <span class="lock-icon" aria-hidden="true"></span>
                        <strong>해금 필요</strong>
                    </div>
                </article>
            </div>
        </section>
    </div>

    <div id="confirmModal" class="shop-confirm-backdrop" hidden>
        <section class="shop-confirm-modal">
            <h2 id="confirmTitle">확인</h2>
            <p id="confirmMessage">진행?</p>
            <div class="confirm-actions">
                <button class="secondary" id="confirmCancel" type="button">취소</button>
                <!-- 해설: 사용자가 클릭해 폼 제출이나 프론트 동작을 실행하는 버튼이다. -->
                <button id="confirmSubmit" type="button">확인</button>
                <!-- 해설: 사용자가 클릭해 폼 제출이나 프론트 동작을 실행하는 버튼이다. -->
            </div>
        </section>
    </div>

    <div th:if="${secretaryOffer != null && activeEvent == null && activeAuction == null}" class="event-modal-backdrop">
    <!-- 해설: 서버 모델 값 조건에 따라 이 요소를 보여주거나 숨긴다. -->
        <section class="event-modal">
            <img class="event-image" th:src="${secretaryOffer.imagePath()}" alt="비서 이미지">
            <!-- 해설: 서버 모델이나 URL 표현식으로 링크/이미지/폼 액션 경로를 만든다. -->
            <div class="event-copy">
                <span class="badge good">비서 고용 제안</span>
                <h2 th:text="${secretaryOffer.name()} + ' 고용 가능'">비서 고용 가능</h2>
                <!-- 해설: 서버 모델 값을 HTML 텍스트로 출력한다. 기본적으로 이스케이프되어 안전하다. -->
                <p th:text="${secretaryOffer.effect()} + ' · ' + ${secretaryOffer.specialEffectSummary()} + ' · 월급 ' + ${@moneyText.format(secretaryOffer.monthlySalaryForProficiency(secretaryOffer.baseProficiency()))}">비서 정보</p>
                <!-- 해설: 서버 모델 값을 HTML 텍스트로 출력한다. 기본적으로 이스케이프되어 안전하다. -->
                <form method="post" th:action="@{/secretaries/{key}/hire(key=${secretaryOffer.key()})}">
                <!-- 해설: 서버 모델이나 URL 표현식으로 링크/이미지/폼 액션 경로를 만든다. -->
                    <button type="submit" th:disabled="${player.paused}" th:text="${player.paused ? '일시정지' : '고용하기'}">고용하기</button>
                    <!-- 해설: 서버 모델 값을 HTML 텍스트로 출력한다. 기본적으로 이스케이프되어 안전하다. -->
                </form>
                <form method="post" th:action="@{/secretaries/{key}/dismiss(key=${secretaryOffer.key()})}">
                <!-- 해설: 서버 모델이나 URL 표현식으로 링크/이미지/폼 액션 경로를 만든다. -->
                    <button class="secondary" type="submit" th:disabled="${player.paused}" th:text="${player.paused ? '일시정지' : '나중에'}">나중에</button>
                    <!-- 해설: 서버 모델 값을 HTML 텍스트로 출력한다. 기본적으로 이스케이프되어 안전하다. -->
                </form>
            </div>
        </section>
    </div>

    <div th:if="${activeAuction != null}" class="event-modal-backdrop auction-modal-backdrop">
    <!-- 해설: 서버 모델 값 조건에 따라 이 요소를 보여주거나 숨긴다. -->
        <section class="event-modal auction-modal" th:if="${activeAuction.status.name() == 'ACTIVE'}">
        <!-- 해설: 서버 모델 값 조건에 따라 이 요소를 보여주거나 숨긴다. -->
            <img class="event-image auction-image" src="/assets/auction/auction-main.png" alt="경매 이미지">
            <div class="event-copy auction-copy">
                <span class="badge good">경매</span>
                <h2 th:text="${activeAuction.city} + ' ' + ${@gameService.auctionDisplayTypeName(activeAuction)} + ' 경매'">경매</h2>
                <!-- 해설: 서버 모델 값을 HTML 텍스트로 출력한다. 기본적으로 이스케이프되어 안전하다. -->
                <p>
                    <span th:text="'시장가 ' + ${@moneyText.format(activeAuction.marketPrice)}">시장가</span>
                    <!-- 해설: 서버 모델 값을 HTML 텍스트로 출력한다. 기본적으로 이스케이프되어 안전하다. -->
                    <span th:text="' · ' + ${@gameService.auctionDisplayName(activeAuction)}">물건명</span>
                    <!-- 해설: 서버 모델 값을 HTML 텍스트로 출력한다. 기본적으로 이스케이프되어 안전하다. -->
                    <span th:text="' · 월세 ' + ${@moneyText.format(activeAuction.monthlyRent)}">월세</span>
                    <!-- 해설: 서버 모델 값을 HTML 텍스트로 출력한다. 기본적으로 이스케이프되어 안전하다. -->
                </p>
                <p>시장가 대비 얼마로 경매 하시겠습니까?</p>
                <div class="auction-bid-grid">
                    <form method="post" th:action="@{/auctions/{id}/bid(id=${activeAuction.id})}">
                    <!-- 해설: 서버 모델이나 URL 표현식으로 링크/이미지/폼 액션 경로를 만든다. -->
                        <input type="hidden" name="rate" value="90">
                        <button type="submit" th:disabled="${player.cash < activeAuction.bidPrice(90)}">
                        <!-- 해설: 사용자가 클릭해 폼 제출이나 프론트 동작을 실행하는 버튼이다. -->
                            <span>시장가 90%</span>
                            <strong th:text="${@moneyText.format(activeAuction.bidPrice(90))}">금액</strong>
                            <!-- 해설: 서버 모델 값을 HTML 텍스트로 출력한다. 기본적으로 이스케이프되어 안전하다. -->
                            <small>성공 80%</small>
                        </button>
                    </form>
                    <form method="post" th:action="@{/auctions/{id}/bid(id=${activeAuction.id})}">
                    <!-- 해설: 서버 모델이나 URL 표현식으로 링크/이미지/폼 액션 경로를 만든다. -->
                        <input type="hidden" name="rate" value="70">
                        <button type="submit" th:disabled="${player.cash < activeAuction.bidPrice(70)}">
                        <!-- 해설: 사용자가 클릭해 폼 제출이나 프론트 동작을 실행하는 버튼이다. -->
                            <span>시장가 70%</span>
                            <strong th:text="${@moneyText.format(activeAuction.bidPrice(70))}">금액</strong>
                            <!-- 해설: 서버 모델 값을 HTML 텍스트로 출력한다. 기본적으로 이스케이프되어 안전하다. -->
                            <small>성공 60%</small>
                        </button>
                    </form>
                    <form method="post" th:action="@{/auctions/{id}/bid(id=${activeAuction.id})}">
                    <!-- 해설: 서버 모델이나 URL 표현식으로 링크/이미지/폼 액션 경로를 만든다. -->
                        <input type="hidden" name="rate" value="50">
                        <button type="submit" th:disabled="${player.cash < activeAuction.bidPrice(50)}">
                        <!-- 해설: 사용자가 클릭해 폼 제출이나 프론트 동작을 실행하는 버튼이다. -->
                            <span>시장가 50%</span>
                            <strong th:text="${@moneyText.format(activeAuction.bidPrice(50))}">금액</strong>
                            <!-- 해설: 서버 모델 값을 HTML 텍스트로 출력한다. 기본적으로 이스케이프되어 안전하다. -->
                            <small>성공 40%</small>
                        </button>
                    </form>
                </div>
                <form class="auction-cancel-form" method="post" th:action="@{/auctions/{id}/cancel(id=${activeAuction.id})}">
                <!-- 해설: 서버 모델이나 URL 표현식으로 링크/이미지/폼 액션 경로를 만든다. -->
                    <button class="danger-button auction-cancel-button" type="submit">경매 취소</button>
                    <!-- 해설: 사용자가 클릭해 폼 제출이나 프론트 동작을 실행하는 버튼이다. -->
                </form>
                <p class="auction-timer" th:attr="data-auction-seconds=${activeAuction.remainingSeconds()}, data-auction-cancel-url=@{/auctions/{id}/cancel(id=${activeAuction.id})}">
                <!-- 해설: data-* 속성은 JavaScript가 읽을 수 있는 화면 상태나 식별자를 HTML에 심어 둔다. -->
                    자동 취소까지 남은 시간: <strong th:text="${activeAuction.remainingSeconds()}">20</strong>초
                    <!-- 해설: 서버 모델 값을 HTML 텍스트로 출력한다. 기본적으로 이스케이프되어 안전하다. -->
                </p>
            </div>
        </section>
        <section class="event-modal auction-modal auction-result-modal" th:if="${activeAuction.status.name() == 'RESULT'}">
        <!-- 해설: 서버 모델 값 조건에 따라 이 요소를 보여주거나 숨긴다. -->
            <img class="event-image auction-image" th:src="${activeAuction.successful ? '/assets/auction/auction-success.png' : '/assets/auction/auction-fail.png'}" alt="경매 결과">
            <!-- 해설: 서버 모델이나 URL 표현식으로 링크/이미지/폼 액션 경로를 만든다. -->
            <div class="event-copy auction-copy">
                <span class="badge" th:classappend="${activeAuction.successful} ? ' good' : ' danger'" th:text="${activeAuction.successful ? '낙찰 성공' : '낙찰 실패'}">결과</span>
                <!-- 해설: 서버 모델 값을 HTML 텍스트로 출력한다. 기본적으로 이스케이프되어 안전하다. -->
                <h2 th:text="${activeAuction.resultMessage}">경매 결과</h2>
                <!-- 해설: 서버 모델 값을 HTML 텍스트로 출력한다. 기본적으로 이스케이프되어 안전하다. -->
                <p th:text="${@gameService.auctionDisplayName(activeAuction)} + ' · 시장가 ' + ${activeAuction.selectedRate} + '% 입찰 · 성공확률 ' + ${activeAuction.successChance} + '%'">결과 상세</p>
                <!-- 해설: 서버 모델 값을 HTML 텍스트로 출력한다. 기본적으로 이스케이프되어 안전하다. -->
                <form method="post" th:action="@{/auctions/{id}/complete(id=${activeAuction.id})}">
                <!-- 해설: 서버 모델이나 URL 표현식으로 링크/이미지/폼 액션 경로를 만든다. -->
                    <button type="submit">확인</button>
                    <!-- 해설: 사용자가 클릭해 폼 제출이나 프론트 동작을 실행하는 버튼이다. -->
                </form>
            </div>
        </section>
    </div>

    <div th:if="${activeEvent != null}" class="event-modal-backdrop">
    <!-- 해설: 서버 모델 값 조건에 따라 이 요소를 보여주거나 숨긴다. -->
        <section class="event-modal">
            <img th:if="${#strings.startsWith(activeEvent.imageLabel, '/')}" class="event-image" th:src="${activeEvent.imageLabel}" alt="이벤트 이미지">
            <!-- 해설: 서버 모델 값 조건에 따라 이 요소를 보여주거나 숨긴다. -->
            <div th:if="${activeEvent.imageLabel == 'EMPTY_SECRETARY_EVENT_IMAGE'}" class="event-image event-image-empty"></div>
            <!-- 해설: 서버 모델 값 조건에 따라 이 요소를 보여주거나 숨긴다. -->
            <div th:if="${!#strings.startsWith(activeEvent.imageLabel, '/') && activeEvent.imageLabel != 'EMPTY_SECRETARY_EVENT_IMAGE'}" class="event-image" th:text="${activeEvent.imageLabel}">AI 이벤트 이미지</div>
            <!-- 해설: 서버 모델 값 조건에 따라 이 요소를 보여주거나 숨긴다. -->
            <div class="event-copy">
                <span class="badge good">이벤트</span>
                <h2 th:text="${activeEvent.title}">이벤트 제목</h2>
                <!-- 해설: 서버 모델 값을 HTML 텍스트로 출력한다. 기본적으로 이스케이프되어 안전하다. -->
                <p th:text="${activeEvent.body}">이벤트 내용</p>
                <!-- 해설: 서버 모델 값을 HTML 텍스트로 출력한다. 기본적으로 이스케이프되어 안전하다. -->
                <div class="event-button-row" th:classappend="${activeEvent.effectKey == 'RESIGN_CONFIRM'} ? ' split'">
                    <form method="post" th:action="@{/events/{id}/complete(id=${activeEvent.id})}">
                    <!-- 해설: 서버 모델이나 URL 표현식으로 링크/이미지/폼 액션 경로를 만든다. -->
                        <button type="submit" th:text="${activeEvent.actionLabel}">계속 진행하기</button>
                        <!-- 해설: 서버 모델 값을 HTML 텍스트로 출력한다. 기본적으로 이스케이프되어 안전하다. -->
                    </form>
                    <form method="post" th:if="${activeEvent.effectKey == 'RESIGN_CONFIRM'}" th:action="@{/events/{id}/cancel(id=${activeEvent.id})}">
                    <!-- 해설: 서버 모델 값 조건에 따라 이 요소를 보여주거나 숨긴다. -->
                        <button type="submit" class="secondary-button">아직.. 아니야..</button>
                        <!-- 해설: 사용자가 클릭해 폼 제출이나 프론트 동작을 실행하는 버튼이다. -->
                    </form>
                </div>
            </div>
        </section>
    </div>
</div>
</body>
</html>
```