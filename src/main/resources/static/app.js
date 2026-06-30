import { setupUiInteractions } from "./app-ui.js";

const buildingSlots = document.querySelectorAll(".building-slot[data-building]");
const buildingDetails = document.querySelectorAll(".building-detail[data-building-detail-id]");
const toast = document.querySelector("#toast");
const flashToast = document.querySelector("#flashToast");
const sideJobButton = document.querySelector("#sideJobBtn");
const cashValue = document.querySelector("#cashValue");
const totalMonthlyRentValue = document.querySelector("#totalMonthlyRentValue");
const dayProgress = document.querySelector("#dayProgress");
const dayProgressText = document.querySelector("#dayProgressText");
const auctionTimer = document.querySelector(".auction-timer[data-auction-seconds]");
const TICK_DURATION_MS = 5000;
const SCROLL_RESTORE_KEY = "buildingStory.scrollY";
const SELECTED_BUILDING_KEY = "buildingStory.selectedBuildingId";
let tickStartedAt = Date.now();
let ticking = false;

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
    const item = document.createElement("div");
    item.className = "toast-item show";
    item.textContent = message;
    toast.prepend(item);
    while (toast.children.length > 5) {
        toast.lastElementChild.remove();
    }
    toast.classList.add("show");
    window.setTimeout(() => {
        item.classList.remove("show");
        window.setTimeout(() => {
            item.remove();
            if (toast.children.length === 0) {
                toast.classList.remove("show");
            }
        }, 300);
    }, 8000);
}

if (flashToast) {
    window.setTimeout(() => flashToast.classList.remove("show"), 8000);
}

function shouldKeepGamePaused() {
    return document.body.dataset.playerPaused === "true"
        || !!document.querySelector(".event-modal-backdrop:not([hidden])")
        || !!document.querySelector("#secretaryModal:not([hidden])")
        || !!document.querySelector("#confirmModal:not([hidden])")
        || !!document.querySelector(".ability-modal-backdrop:not([hidden])")
        || !!document.querySelector(".gift-select-popover:not([hidden])");
}

function syncGamePauseState() {
    if (shouldKeepGamePaused()) {
        document.body.classList.add("game-paused");
    } else {
        document.body.classList.remove("game-paused");
    }
}

setupUiInteractions({ syncGamePauseState });

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
        if (result.event || result.auction) {
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
