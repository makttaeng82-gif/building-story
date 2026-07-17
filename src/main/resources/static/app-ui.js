/*
 * app-ui.js는 메인 화면 여러 곳에서 재사용하는 UI 상호작용을 모은 모듈이다.
 *
 * app.js가 게임 시간, 주식, 건물 선택처럼 화면별 기능을 담당한다면,
 * 이 파일은 확인 모달, 이미지 모달, 도움말 팝오버, 선물 수량 입력처럼
 * 여러 화면 요소가 공통으로 쓰는 동작을 초기화한다.
 */
export function setupUiInteractions({ syncGamePauseState }) {
    const imageModal = document.querySelector("#imageModal");
    const imageModalImg = document.querySelector("#imageModalImg");
    const imageModalTitle = document.querySelector("#imageModalTitle");
    const imageModalClose = document.querySelector("#imageModalClose");
    const secretaryListButton = document.querySelector("#secretaryListButton");
    const secretaryModal = document.querySelector("#secretaryModal");
    const secretaryModalClose = document.querySelector("#secretaryModalClose");
    const confirmModal = document.querySelector("#confirmModal");
    const confirmTitle = document.querySelector("#confirmTitle");
    const confirmMessage = document.querySelector("#confirmMessage");
    const confirmCancel = document.querySelector("#confirmCancel");
    const confirmSubmit = document.querySelector("#confirmSubmit");
    let pendingConfirmForm = null;

    function closeConfirmModal() {
        // 확인 모달이 닫히면 보류 중인 form 참조를 반드시 비운다. 그렇지 않으면 이전 form이 다시 제출될 수 있다.
        pendingConfirmForm = null;
        if (confirmModal) {
            confirmModal.hidden = true;
        }
        syncGamePauseState();
    }

    document.querySelectorAll(".confirm-form[data-confirm-message]").forEach((form) => {
        form.addEventListener("submit", (event) => {
            // 첫 submit은 막고 확인 모달을 띄운다. 사용자가 확인하면 dataset.confirmed를 표시하고 다시 submit한다.
            if (!confirmModal || form.dataset.confirmed === "true") {
                form.dataset.confirmed = "";
                return;
            }
            event.preventDefault();
            pendingConfirmForm = form;
            if (confirmTitle) {
                confirmTitle.textContent = form.dataset.confirmTitle || "확인";
            }
            if (confirmMessage) {
                const quantityInput = form.querySelector('input[name="quantity"]');
                const quantityText = quantityInput ? ` · ${quantityInput.value || 1}개` : "";
                confirmMessage.textContent = `${form.dataset.confirmMessage || "진행?"}${quantityText}`;
            }
            confirmModal.hidden = false;
            syncGamePauseState();
        });
    });

    if (confirmCancel) {
        confirmCancel.addEventListener("click", closeConfirmModal);
    }

    if (confirmSubmit) {
        confirmSubmit.addEventListener("click", () => {
            if (!pendingConfirmForm) {
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
        confirmModal.addEventListener("click", (event) => {
            if (event.target === confirmModal) {
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
    // data-full-image가 붙은 버튼을 누르면 작은 카드 이미지를 큰 모달 이미지로 보여준다.
    function closeImageModal() {
        if (!imageModal || !imageModalImg) {
            return;
        }
        imageModal.hidden = true;
        imageModalImg.src = "";
    }

    document.querySelectorAll(".image-open-button[data-full-image]").forEach((button) => {
        button.addEventListener("click", () => {
            if (!imageModal || !imageModalImg || !imageModalTitle) {
                return;
            }
            imageModalImg.src = button.dataset.fullImage;
            imageModalTitle.textContent = button.dataset.imageTitle || "이미지";
            imageModal.hidden = false;
        });
    });

    if (imageModalClose) {
        imageModalClose.addEventListener("click", closeImageModal);
    }

    if (imageModal) {
        imageModal.addEventListener("click", (event) => {
            if (event.target === imageModal) {
                closeImageModal();
            }
        });
    }
}

function setupHelpPopovers() {
    // 도움말 팝오버는 aria-controls로 연결된 요소를 열고 닫는다. 다른 곳을 클릭하면 모두 닫힌다.
    document.querySelectorAll(".help-dot[aria-controls]").forEach((button) => {
        button.addEventListener("click", (event) => {
            event.stopPropagation();
            const target = document.getElementById(button.getAttribute("aria-controls"));
            if (!target) {
                return;
            }
            const shouldOpen = target.hidden;
            document.querySelectorAll(".help-popover").forEach((popover) => {
                popover.hidden = true;
            });
            document.querySelectorAll(".help-dot.active").forEach((activeButton) => {
                activeButton.classList.remove("active");
            });
            if (!shouldOpen) {
                return;
            }
            const card = button.closest(".secretary-card") || button.closest(".panel");
            const cardRect = card.getBoundingClientRect();
            const buttonRect = button.getBoundingClientRect();
            target.hidden = false;
            button.classList.add("active");
            target.style.left = `${buttonRect.left - cardRect.left + buttonRect.width + 8}px`;
            target.style.top = `${buttonRect.top - cardRect.top}px`;
        });
    });

    document.addEventListener("click", (event) => {
        if (event.target.closest(".help-popover") || event.target.closest(".help-dot")) {
            return;
        }
        document.querySelectorAll(".help-popover").forEach((popover) => {
            popover.hidden = true;
        });
        document.querySelectorAll(".help-dot.active").forEach((button) => {
            button.classList.remove("active");
        });
    });
}

function setupSecretaryModal({ secretaryListButton, secretaryModal, secretaryModalClose, syncGamePauseState }) {
    if (secretaryListButton && secretaryModal) {
        secretaryListButton.addEventListener("click", () => {
            secretaryModal.hidden = false;
            document.body.classList.add("game-paused");
        });
    }

    if (secretaryModalClose && secretaryModal) {
        secretaryModalClose.addEventListener("click", () => {
            secretaryModal.hidden = true;
            syncGamePauseState();
        });
    }

    if (secretaryModal) {
        secretaryModal.addEventListener("click", (event) => {
            if (event.target === secretaryModal) {
                secretaryModal.hidden = true;
                syncGamePauseState();
            }
        });
    }
}

function setupAbilityModals(syncGamePauseState) {
    document.addEventListener("click", (event) => {
        const openButton = event.target.closest(".ability-modal-open[data-ability-modal]");
        if (!openButton) {
            return;
        }
        const target = document.getElementById(openButton.dataset.abilityModal);
        if (!target) {
            return;
        }
        target.hidden = false;
        syncGamePauseState();
    });

    document.querySelectorAll(".ability-modal-close").forEach((button) => {
        button.addEventListener("click", () => {
            const target = button.closest(".ability-modal-backdrop");
            if (!target) {
                return;
            }
            target.hidden = true;
            syncGamePauseState();
        });
    });

    document.querySelectorAll(".ability-modal-backdrop").forEach((modal) => {
        modal.addEventListener("click", (event) => {
            if (event.target !== modal) {
                return;
            }
            modal.hidden = true;
            syncGamePauseState();
        });
    });
}

function setupGiftPopovers(syncGamePauseState) {
    document.querySelectorAll(".gift-modal-open[data-gift-modal]").forEach((button) => {
        button.addEventListener("click", () => {
            const target = document.getElementById(button.dataset.giftModal);
            if (target) {
                target.hidden = false;
                syncGamePauseState();
            }
        });
    });

    document.querySelectorAll(".gift-modal-close").forEach((button) => {
        button.addEventListener("click", () => {
            const target = button.closest(".gift-select-popover");
            if (target) {
                target.hidden = true;
                syncGamePauseState();
            }
        });
    });
}

function setupQuantityInputs() {
    document.querySelectorAll(".quantity-input").forEach((input) => {
        input.addEventListener("input", () => {
            const min = Number(input.min) || 1;
            const max = Number(input.max) || 99;
            const value = Number(input.value);
            if (!Number.isFinite(value)) {
                return;
            }
            if (value > max) {
                input.value = String(max);
            }
            if (value < min) {
                input.value = String(min);
            }
        });
    });
}

function setupEscapeClose({ closeConfirmModal, syncGamePauseState }) {
    document.addEventListener("keydown", (event) => {
        if (event.key !== "Escape") {
            return;
        }
        document.querySelectorAll(".gift-select-popover").forEach((popover) => {
            popover.hidden = true;
        });
        document.querySelectorAll(".ability-modal-backdrop").forEach((modal) => {
            modal.hidden = true;
        });
        syncGamePauseState();
        closeConfirmModal();
    });
}
