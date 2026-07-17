# app-ui.js 코드 주석형 해설

원본 파일: `src/main/resources/static/app-ui.js`

형식:
- 원본 코드를 그대로 배치한다.
- 필요한 코드 바로 아래에 해설 주석을 붙인다.
- import/package/단순 선언은 과하게 설명하지 않는다.

```javascript
/*
 * app-ui.js는 메인 화면 여러 곳에서 재사용하는 UI 상호작용을 모은 모듈이다.
 *
 * app.js가 게임 시간, 주식, 건물 선택처럼 화면별 기능을 담당한다면,
 * 이 파일은 확인 모달, 이미지 모달, 도움말 팝오버, 선물 수량 입력처럼
 * 여러 화면 요소가 공통으로 쓰는 동작을 초기화한다.
 */
export function setupUiInteractions({ syncGamePauseState }) {
    const imageModal = document.querySelector("#imageModal");
    // 해설: DOM에서 특정 HTML 요소를 찾는다. 이후 텍스트, 클래스, 속성 등을 조작한다.
    const imageModalImg = document.querySelector("#imageModalImg");
    // 해설: DOM에서 특정 HTML 요소를 찾는다. 이후 텍스트, 클래스, 속성 등을 조작한다.
    const imageModalTitle = document.querySelector("#imageModalTitle");
    // 해설: DOM에서 특정 HTML 요소를 찾는다. 이후 텍스트, 클래스, 속성 등을 조작한다.
    const imageModalClose = document.querySelector("#imageModalClose");
    // 해설: DOM에서 특정 HTML 요소를 찾는다. 이후 텍스트, 클래스, 속성 등을 조작한다.
    const secretaryListButton = document.querySelector("#secretaryListButton");
    // 해설: DOM에서 특정 HTML 요소를 찾는다. 이후 텍스트, 클래스, 속성 등을 조작한다.
    const secretaryModal = document.querySelector("#secretaryModal");
    // 해설: DOM에서 특정 HTML 요소를 찾는다. 이후 텍스트, 클래스, 속성 등을 조작한다.
    const secretaryModalClose = document.querySelector("#secretaryModalClose");
    // 해설: DOM에서 특정 HTML 요소를 찾는다. 이후 텍스트, 클래스, 속성 등을 조작한다.
    const confirmModal = document.querySelector("#confirmModal");
    // 해설: DOM에서 특정 HTML 요소를 찾는다. 이후 텍스트, 클래스, 속성 등을 조작한다.
    const confirmTitle = document.querySelector("#confirmTitle");
    // 해설: DOM에서 특정 HTML 요소를 찾는다. 이후 텍스트, 클래스, 속성 등을 조작한다.
    const confirmMessage = document.querySelector("#confirmMessage");
    // 해설: DOM에서 특정 HTML 요소를 찾는다. 이후 텍스트, 클래스, 속성 등을 조작한다.
    const confirmCancel = document.querySelector("#confirmCancel");
    // 해설: DOM에서 특정 HTML 요소를 찾는다. 이후 텍스트, 클래스, 속성 등을 조작한다.
    const confirmSubmit = document.querySelector("#confirmSubmit");
    // 해설: DOM에서 특정 HTML 요소를 찾는다. 이후 텍스트, 클래스, 속성 등을 조작한다.
    let pendingConfirmForm = null;

    function closeConfirmModal() {
    // 해설: 함수 또는 화살표 함수다. 반복되는 프론트 동작을 묶어 재사용한다.
        // 확인 모달이 닫히면 보류 중인 form 참조를 반드시 비운다. 그렇지 않으면 이전 form이 다시 제출될 수 있다.
        pendingConfirmForm = null;
        if (confirmModal) {
        // 해설: 조건 분기다. 현재 화면 상태나 응답 값에 따라 다른 동작을 수행한다.
            confirmModal.hidden = true;
        }
        syncGamePauseState();
    }

    document.querySelectorAll(".confirm-form[data-confirm-message]").forEach((form) => {
    // 해설: 함수 또는 화살표 함수다. 반복되는 프론트 동작을 묶어 재사용한다.
        form.addEventListener("submit", (event) => {
        // 해설: 함수 또는 화살표 함수다. 반복되는 프론트 동작을 묶어 재사용한다.
            // 첫 submit은 막고 확인 모달을 띄운다. 사용자가 확인하면 dataset.confirmed를 표시하고 다시 submit한다.
            if (!confirmModal || form.dataset.confirmed === "true") {
            // 해설: 조건 분기다. 현재 화면 상태나 응답 값에 따라 다른 동작을 수행한다.
                form.dataset.confirmed = "";
                return;
            }
            event.preventDefault();
            pendingConfirmForm = form;
            if (confirmTitle) {
            // 해설: 조건 분기다. 현재 화면 상태나 응답 값에 따라 다른 동작을 수행한다.
                confirmTitle.textContent = form.dataset.confirmTitle || "확인";
            }
            if (confirmMessage) {
            // 해설: 조건 분기다. 현재 화면 상태나 응답 값에 따라 다른 동작을 수행한다.
                const quantityInput = form.querySelector('input[name="quantity"]');
                // 해설: DOM에서 특정 HTML 요소를 찾는다. 이후 텍스트, 클래스, 속성 등을 조작한다.
                const quantityText = quantityInput ? ` · ${quantityInput.value || 1}개` : "";
                confirmMessage.textContent = `${form.dataset.confirmMessage || "진행?"}${quantityText}`;
            }
            confirmModal.hidden = false;
            syncGamePauseState();
        });
    });

    if (confirmCancel) {
    // 해설: 조건 분기다. 현재 화면 상태나 응답 값에 따라 다른 동작을 수행한다.
        confirmCancel.addEventListener("click", closeConfirmModal);
        // 해설: 브라우저 이벤트를 등록한다. 클릭, 입력, 로드 같은 사용자/화면 동작에 반응한다.
    }

    if (confirmSubmit) {
    // 해설: 조건 분기다. 현재 화면 상태나 응답 값에 따라 다른 동작을 수행한다.
        confirmSubmit.addEventListener("click", () => {
        // 해설: 함수 또는 화살표 함수다. 반복되는 프론트 동작을 묶어 재사용한다.
            if (!pendingConfirmForm) {
            // 해설: 조건 분기다. 현재 화면 상태나 응답 값에 따라 다른 동작을 수행한다.
                closeConfirmModal();
                return;
            }
            const form = pendingConfirmForm;
            form.dataset.confirmed = "true";
            closeConfirmModal();
            form.requestSubmit();
        });
    }

    if (confirmModal) {
    // 해설: 조건 분기다. 현재 화면 상태나 응답 값에 따라 다른 동작을 수행한다.
        confirmModal.addEventListener("click", (event) => {
        // 해설: 함수 또는 화살표 함수다. 반복되는 프론트 동작을 묶어 재사용한다.
            if (event.target === confirmModal) {
            // 해설: 조건 분기다. 현재 화면 상태나 응답 값에 따라 다른 동작을 수행한다.
                closeConfirmModal();
            }
        });
    }

    setupImageModal({ imageModal, imageModalImg, imageModalTitle, imageModalClose });
    setupHelpPopovers();
    setupSecretaryModal({ secretaryListButton, secretaryModal, secretaryModalClose, syncGamePauseState });
    setupAbilityModals(syncGamePauseState);
    setupGiftPopovers(syncGamePauseState);
    setupQuantityInputs();
    setupEscapeClose({ closeConfirmModal, syncGamePauseState });
}

function setupImageModal({ imageModal, imageModalImg, imageModalTitle, imageModalClose }) {
// 해설: 함수 또는 화살표 함수다. 반복되는 프론트 동작을 묶어 재사용한다.
    // data-full-image가 붙은 버튼을 누르면 작은 카드 이미지를 큰 모달 이미지로 보여준다.
    function closeImageModal() {
    // 해설: 함수 또는 화살표 함수다. 반복되는 프론트 동작을 묶어 재사용한다.
        if (!imageModal || !imageModalImg) {
        // 해설: 조건 분기다. 현재 화면 상태나 응답 값에 따라 다른 동작을 수행한다.
            return;
        }
        imageModal.hidden = true;
        imageModalImg.src = "";
    }

    document.querySelectorAll(".image-open-button[data-full-image]").forEach((button) => {
    // 해설: 함수 또는 화살표 함수다. 반복되는 프론트 동작을 묶어 재사용한다.
        button.addEventListener("click", () => {
        // 해설: 함수 또는 화살표 함수다. 반복되는 프론트 동작을 묶어 재사용한다.
            if (!imageModal || !imageModalImg || !imageModalTitle) {
            // 해설: 조건 분기다. 현재 화면 상태나 응답 값에 따라 다른 동작을 수행한다.
                return;
            }
            imageModalImg.src = button.dataset.fullImage;
            imageModalTitle.textContent = button.dataset.imageTitle || "이미지";
            imageModal.hidden = false;
        });
    });

    if (imageModalClose) {
    // 해설: 조건 분기다. 현재 화면 상태나 응답 값에 따라 다른 동작을 수행한다.
        imageModalClose.addEventListener("click", closeImageModal);
        // 해설: 브라우저 이벤트를 등록한다. 클릭, 입력, 로드 같은 사용자/화면 동작에 반응한다.
    }

    if (imageModal) {
    // 해설: 조건 분기다. 현재 화면 상태나 응답 값에 따라 다른 동작을 수행한다.
        imageModal.addEventListener("click", (event) => {
        // 해설: 함수 또는 화살표 함수다. 반복되는 프론트 동작을 묶어 재사용한다.
            if (event.target === imageModal) {
            // 해설: 조건 분기다. 현재 화면 상태나 응답 값에 따라 다른 동작을 수행한다.
                closeImageModal();
            }
        });
    }
}

function setupHelpPopovers() {
// 해설: 함수 또는 화살표 함수다. 반복되는 프론트 동작을 묶어 재사용한다.
    // 도움말 팝오버는 aria-controls로 연결된 요소를 열고 닫는다. 다른 곳을 클릭하면 모두 닫힌다.
    document.querySelectorAll(".help-dot[aria-controls]").forEach((button) => {
    // 해설: 함수 또는 화살표 함수다. 반복되는 프론트 동작을 묶어 재사용한다.
        button.addEventListener("click", (event) => {
        // 해설: 함수 또는 화살표 함수다. 반복되는 프론트 동작을 묶어 재사용한다.
            event.stopPropagation();
            const target = document.getElementById(button.getAttribute("aria-controls"));
            // 해설: DOM에서 특정 HTML 요소를 찾는다. 이후 텍스트, 클래스, 속성 등을 조작한다.
            if (!target) {
            // 해설: 조건 분기다. 현재 화면 상태나 응답 값에 따라 다른 동작을 수행한다.
                return;
            }
            const shouldOpen = target.hidden;
            document.querySelectorAll(".help-popover").forEach((popover) => {
            // 해설: 함수 또는 화살표 함수다. 반복되는 프론트 동작을 묶어 재사용한다.
                popover.hidden = true;
            });
            document.querySelectorAll(".help-dot.active").forEach((activeButton) => {
            // 해설: 함수 또는 화살표 함수다. 반복되는 프론트 동작을 묶어 재사용한다.
                activeButton.classList.remove("active");
                // 해설: HTML 요소의 CSS 클래스를 추가/삭제/토글해 화면 상태를 바꾼다.
            });
            if (!shouldOpen) {
            // 해설: 조건 분기다. 현재 화면 상태나 응답 값에 따라 다른 동작을 수행한다.
                return;
            }
            const card = button.closest(".secretary-card") || button.closest(".panel");
            const cardRect = card.getBoundingClientRect();
            const buttonRect = button.getBoundingClientRect();
            target.hidden = false;
            button.classList.add("active");
            // 해설: HTML 요소의 CSS 클래스를 추가/삭제/토글해 화면 상태를 바꾼다.
            target.style.left = `${buttonRect.left - cardRect.left + buttonRect.width + 8}px`;
            target.style.top = `${buttonRect.top - cardRect.top}px`;
        });
    });

    document.addEventListener("click", (event) => {
    // 해설: 함수 또는 화살표 함수다. 반복되는 프론트 동작을 묶어 재사용한다.
        if (event.target.closest(".help-popover") || event.target.closest(".help-dot")) {
        // 해설: 조건 분기다. 현재 화면 상태나 응답 값에 따라 다른 동작을 수행한다.
            return;
        }
        document.querySelectorAll(".help-popover").forEach((popover) => {
        // 해설: 함수 또는 화살표 함수다. 반복되는 프론트 동작을 묶어 재사용한다.
            popover.hidden = true;
        });
        document.querySelectorAll(".help-dot.active").forEach((button) => {
        // 해설: 함수 또는 화살표 함수다. 반복되는 프론트 동작을 묶어 재사용한다.
            button.classList.remove("active");
            // 해설: HTML 요소의 CSS 클래스를 추가/삭제/토글해 화면 상태를 바꾼다.
        });
    });
}

function setupSecretaryModal({ secretaryListButton, secretaryModal, secretaryModalClose, syncGamePauseState }) {
// 해설: 함수 또는 화살표 함수다. 반복되는 프론트 동작을 묶어 재사용한다.
    if (secretaryListButton && secretaryModal) {
    // 해설: 조건 분기다. 현재 화면 상태나 응답 값에 따라 다른 동작을 수행한다.
        secretaryListButton.addEventListener("click", () => {
        // 해설: 함수 또는 화살표 함수다. 반복되는 프론트 동작을 묶어 재사용한다.
            secretaryModal.hidden = false;
            document.body.classList.add("game-paused");
            // 해설: HTML 요소의 CSS 클래스를 추가/삭제/토글해 화면 상태를 바꾼다.
        });
    }

    if (secretaryModalClose && secretaryModal) {
    // 해설: 조건 분기다. 현재 화면 상태나 응답 값에 따라 다른 동작을 수행한다.
        secretaryModalClose.addEventListener("click", () => {
        // 해설: 함수 또는 화살표 함수다. 반복되는 프론트 동작을 묶어 재사용한다.
            secretaryModal.hidden = true;
            syncGamePauseState();
        });
    }

    if (secretaryModal) {
    // 해설: 조건 분기다. 현재 화면 상태나 응답 값에 따라 다른 동작을 수행한다.
        secretaryModal.addEventListener("click", (event) => {
        // 해설: 함수 또는 화살표 함수다. 반복되는 프론트 동작을 묶어 재사용한다.
            if (event.target === secretaryModal) {
            // 해설: 조건 분기다. 현재 화면 상태나 응답 값에 따라 다른 동작을 수행한다.
                secretaryModal.hidden = true;
                syncGamePauseState();
            }
        });
    }
}

function setupAbilityModals(syncGamePauseState) {
// 해설: 함수 또는 화살표 함수다. 반복되는 프론트 동작을 묶어 재사용한다.
    document.addEventListener("click", (event) => {
    // 해설: 함수 또는 화살표 함수다. 반복되는 프론트 동작을 묶어 재사용한다.
        const openButton = event.target.closest(".ability-modal-open[data-ability-modal]");
        if (!openButton) {
        // 해설: 조건 분기다. 현재 화면 상태나 응답 값에 따라 다른 동작을 수행한다.
            return;
        }
        const target = document.getElementById(openButton.dataset.abilityModal);
        // 해설: DOM에서 특정 HTML 요소를 찾는다. 이후 텍스트, 클래스, 속성 등을 조작한다.
        if (!target) {
        // 해설: 조건 분기다. 현재 화면 상태나 응답 값에 따라 다른 동작을 수행한다.
            return;
        }
        target.hidden = false;
        syncGamePauseState();
    });

    document.querySelectorAll(".ability-modal-close").forEach((button) => {
    // 해설: 함수 또는 화살표 함수다. 반복되는 프론트 동작을 묶어 재사용한다.
        button.addEventListener("click", () => {
        // 해설: 함수 또는 화살표 함수다. 반복되는 프론트 동작을 묶어 재사용한다.
            const target = button.closest(".ability-modal-backdrop");
            if (!target) {
            // 해설: 조건 분기다. 현재 화면 상태나 응답 값에 따라 다른 동작을 수행한다.
                return;
            }
            target.hidden = true;
            syncGamePauseState();
        });
    });

    document.querySelectorAll(".ability-modal-backdrop").forEach((modal) => {
    // 해설: 함수 또는 화살표 함수다. 반복되는 프론트 동작을 묶어 재사용한다.
        modal.addEventListener("click", (event) => {
        // 해설: 함수 또는 화살표 함수다. 반복되는 프론트 동작을 묶어 재사용한다.
            if (event.target !== modal) {
            // 해설: 조건 분기다. 현재 화면 상태나 응답 값에 따라 다른 동작을 수행한다.
                return;
            }
            modal.hidden = true;
            syncGamePauseState();
        });
    });
}

function setupGiftPopovers(syncGamePauseState) {
// 해설: 함수 또는 화살표 함수다. 반복되는 프론트 동작을 묶어 재사용한다.
    document.querySelectorAll(".gift-modal-open[data-gift-modal]").forEach((button) => {
    // 해설: 함수 또는 화살표 함수다. 반복되는 프론트 동작을 묶어 재사용한다.
        button.addEventListener("click", () => {
        // 해설: 함수 또는 화살표 함수다. 반복되는 프론트 동작을 묶어 재사용한다.
            const target = document.getElementById(button.dataset.giftModal);
            // 해설: DOM에서 특정 HTML 요소를 찾는다. 이후 텍스트, 클래스, 속성 등을 조작한다.
            if (target) {
            // 해설: 조건 분기다. 현재 화면 상태나 응답 값에 따라 다른 동작을 수행한다.
                target.hidden = false;
                syncGamePauseState();
            }
        });
    });

    document.querySelectorAll(".gift-modal-close").forEach((button) => {
    // 해설: 함수 또는 화살표 함수다. 반복되는 프론트 동작을 묶어 재사용한다.
        button.addEventListener("click", () => {
        // 해설: 함수 또는 화살표 함수다. 반복되는 프론트 동작을 묶어 재사용한다.
            const target = button.closest(".gift-select-popover");
            if (target) {
            // 해설: 조건 분기다. 현재 화면 상태나 응답 값에 따라 다른 동작을 수행한다.
                target.hidden = true;
                syncGamePauseState();
            }
        });
    });
}

function setupQuantityInputs() {
// 해설: 함수 또는 화살표 함수다. 반복되는 프론트 동작을 묶어 재사용한다.
    document.querySelectorAll(".quantity-input").forEach((input) => {
    // 해설: 함수 또는 화살표 함수다. 반복되는 프론트 동작을 묶어 재사용한다.
        input.addEventListener("input", () => {
        // 해설: 함수 또는 화살표 함수다. 반복되는 프론트 동작을 묶어 재사용한다.
            const min = Number(input.min) || 1;
            const max = Number(input.max) || 99;
            const value = Number(input.value);
            if (!Number.isFinite(value)) {
            // 해설: 조건 분기다. 현재 화면 상태나 응답 값에 따라 다른 동작을 수행한다.
                return;
            }
            if (value > max) {
            // 해설: 조건 분기다. 현재 화면 상태나 응답 값에 따라 다른 동작을 수행한다.
                input.value = String(max);
            }
            if (value < min) {
            // 해설: 조건 분기다. 현재 화면 상태나 응답 값에 따라 다른 동작을 수행한다.
                input.value = String(min);
            }
        });
    });
}

function setupEscapeClose({ closeConfirmModal, syncGamePauseState }) {
// 해설: 함수 또는 화살표 함수다. 반복되는 프론트 동작을 묶어 재사용한다.
    document.addEventListener("keydown", (event) => {
    // 해설: 함수 또는 화살표 함수다. 반복되는 프론트 동작을 묶어 재사용한다.
        if (event.key !== "Escape") {
        // 해설: 조건 분기다. 현재 화면 상태나 응답 값에 따라 다른 동작을 수행한다.
            return;
        }
        document.querySelectorAll(".gift-select-popover").forEach((popover) => {
        // 해설: 함수 또는 화살표 함수다. 반복되는 프론트 동작을 묶어 재사용한다.
            popover.hidden = true;
        });
        document.querySelectorAll(".ability-modal-backdrop").forEach((modal) => {
        // 해설: 함수 또는 화살표 함수다. 반복되는 프론트 동작을 묶어 재사용한다.
            modal.hidden = true;
        });
        syncGamePauseState();
        closeConfirmModal();
    });
}
```