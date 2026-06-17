const buildingSlots = document.querySelectorAll(".building-slot[data-building]");
const buildingDetails = document.querySelectorAll(".building-detail[data-building-detail-id]");
const toast = document.querySelector("#toast");
const flashToast = document.querySelector("#flashToast");
const sideJobButton = document.querySelector("#sideJobBtn");
const cashValue = document.querySelector("#cashValue");
const totalMonthlyRentValue = document.querySelector("#totalMonthlyRentValue");
const dayProgress = document.querySelector("#dayProgress");
const dayProgressText = document.querySelector("#dayProgressText");
const imageModal = document.querySelector("#imageModal");
const imageModalImg = document.querySelector("#imageModalImg");
const imageModalTitle = document.querySelector("#imageModalTitle");
const imageModalClose = document.querySelector("#imageModalClose");
const secretaryListButton = document.querySelector("#secretaryListButton");
const secretaryModal = document.querySelector("#secretaryModal");
const secretaryModalClose = document.querySelector("#secretaryModalClose");
const auctionTimer = document.querySelector(".auction-timer[data-auction-seconds]");
const confirmModal = document.querySelector("#confirmModal");
const confirmTitle = document.querySelector("#confirmTitle");
const confirmMessage = document.querySelector("#confirmMessage");
const confirmCancel = document.querySelector("#confirmCancel");
const confirmSubmit = document.querySelector("#confirmSubmit");
const TICK_DURATION_MS = 5000;
const SCROLL_RESTORE_KEY = "buildingStory.scrollY";
let tickStartedAt = Date.now();
let ticking = false;
const SELECTED_BUILDING_KEY = "buildingStory.selectedBuildingId";

if ("scrollRestoration" in window.history) {
    window.history.scrollRestoration = "manual";
}

function saveScrollPosition() {
    window.sessionStorage.setItem(SCROLL_RESTORE_KEY, String(window.scrollY));
}

function restoreScrollPosition() {
    const savedY = window.sessionStorage.getItem(SCROLL_RESTORE_KEY);
    if (savedY === null) {
        document.documentElement.classList.remove("restore-scroll-pending");
        return;
    }
    window.sessionStorage.removeItem(SCROLL_RESTORE_KEY);
    window.requestAnimationFrame(() => {
        window.scrollTo(0, Number(savedY) || 0);
        document.documentElement.classList.remove("restore-scroll-pending");
    });
}

restoreScrollPosition();

function selectBuilding(buildingId) {
    const targetSlot = Array.from(buildingSlots).find((slot) => slot.dataset.buildingId === buildingId) || buildingSlots[0];
    if (!targetSlot) {
        return;
    }
    const selectedId = targetSlot.dataset.buildingId;
    window.localStorage.setItem(SELECTED_BUILDING_KEY, selectedId);

    buildingSlots.forEach((item) => item.classList.toggle("selected", item.dataset.buildingId === selectedId));
    buildingDetails.forEach((detail) => {
        detail.classList.toggle("active", detail.dataset.buildingDetailId === selectedId);
    });
}

buildingSlots.forEach((slot) => {
    slot.addEventListener("click", () => selectBuilding(slot.dataset.buildingId));
});

if (buildingSlots.length > 0) {
    selectBuilding(window.localStorage.getItem(SELECTED_BUILDING_KEY));
}

function showToast(message) {
    if (!message || !toast) {
        return;
    }
    toast.textContent = message;
    toast.classList.add("show");
    window.setTimeout(() => toast.classList.remove("show"), 3200);
}

if (flashToast) {
    window.setTimeout(() => flashToast.classList.remove("show"), 3200);
}

let pendingConfirmForm = null;
const playerPausedFromServer = document.body.dataset.playerPaused === "true";

function closeConfirmModal() {
    pendingConfirmForm = null;
    if (confirmModal) {
        confirmModal.hidden = true;
    }
    syncGamePauseState();
}

function shouldKeepGamePaused() {
    return playerPausedFromServer
        || !!document.querySelector(".event-modal-backdrop:not([hidden])")
        || (secretaryModal && !secretaryModal.hidden)
        || (confirmModal && !confirmModal.hidden)
        || !!document.querySelector(".gift-select-popover:not([hidden])");
}

function syncGamePauseState() {
    if (shouldKeepGamePaused()) {
        document.body.classList.add("game-paused");
    } else {
        document.body.classList.remove("game-paused");
    }
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

function closeImageModal() {
    if (!imageModal || !imageModalImg) {
        return;
    }
    imageModal.hidden = true;
    imageModalImg.src = "";
}

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

document.addEventListener("keydown", (event) => {
    if (event.key !== "Escape") {
        return;
    }
    document.querySelectorAll(".gift-select-popover").forEach((popover) => {
        popover.hidden = true;
    });
    syncGamePauseState();
    closeConfirmModal();
});

if (auctionTimer) {
    const timerValue = auctionTimer.querySelector("strong");
    const cancelUrl = auctionTimer.dataset.auctionCancelUrl;
    let remainingSeconds = Number(auctionTimer.dataset.auctionSeconds) || 0;
    let auctionClosing = false;
    const closeAuction = async () => {
        if (auctionClosing || !cancelUrl) {
            return;
        }
        auctionClosing = true;
        try {
            await fetch(cancelUrl, { method: "POST" });
        } finally {
            saveScrollPosition();
            window.location.reload();
        }
    };
    if (remainingSeconds <= 0) {
        closeAuction();
    } else {
        const auctionCountdown = window.setInterval(() => {
            remainingSeconds -= 1;
            if (timerValue) {
                timerValue.textContent = String(Math.max(0, remainingSeconds));
            }
            if (remainingSeconds <= 0) {
                window.clearInterval(auctionCountdown);
                closeAuction();
            }
        }, 1000);
    }
}

if (sideJobButton) {
    sideJobButton.addEventListener("click", async () => {
        sideJobButton.disabled = true;
        try {
            const response = await fetch("/side-job/quick", { method: "POST" });
            const result = await response.json();
            if (result.redirect) {
                window.location.href = result.redirect;
                return;
            }
            if (cashValue) {
                cashValue.textContent = result.cash;
            }
            if (totalMonthlyRentValue) {
                totalMonthlyRentValue.textContent = result.totalMonthlyRent;
            }
            showToast(result.notice);
        } catch (error) {
            console.warn("side job failed", error);
            showToast("부업 처리 실패");
        } finally {
            sideJobButton.disabled = false;
        }
    });
}

async function advanceDay() {
    if (ticking || document.body.classList.contains("game-paused")) {
        return;
    }
    ticking = true;
    try {
        const response = await fetch("/tick", { method: "POST" });
        const result = await response.json();
        if (result.redirect) {
            window.location.href = result.redirect;
            return;
        }
        if (result.event) {
            saveScrollPosition();
            window.location.reload();
            return;
        }
        if (result.auction) {
            saveScrollPosition();
            window.location.reload();
            return;
        }
        if (result.notice) {
            showToast(result.notice);
            window.setTimeout(() => {
                saveScrollPosition();
                window.location.reload();
            }, 1000);
        } else {
            saveScrollPosition();
            window.location.reload();
        }
    } catch (error) {
        console.warn("tick failed", error);
        tickStartedAt = Date.now();
    } finally {
        ticking = false;
    }
}

function updateDayProgress() {
    if (!dayProgress || !dayProgressText) {
        return;
    }
    if (document.body.classList.contains("game-paused")) {
        dayProgressText.textContent = "일시정지";
        return;
    }
    const elapsed = Date.now() - tickStartedAt;
    const percent = Math.min(100, Math.floor((elapsed / TICK_DURATION_MS) * 100));
    dayProgress.style.width = `${percent}%`;
    dayProgressText.textContent = `다음 날 ${percent}%`;
    if (elapsed >= TICK_DURATION_MS) {
        advanceDay();
    }
}

if (document.querySelector(".city-panel")) {
    window.setInterval(updateDayProgress, 100);
}
