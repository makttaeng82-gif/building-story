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
        pendingConfirmForm = null;
        if (confirmModal) {
            confirmModal.hidden = true;
        }
        syncGamePauseState();
    }

    document.querySelectorAll(".confirm-form[data-confirm-message]").forEach((form) => {
        form.addEventListener("submit", (event) => {
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
