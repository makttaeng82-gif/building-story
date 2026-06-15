const buildingSlots = document.querySelectorAll(".building-slot[data-building]");
const buildingDetails = document.querySelectorAll(".building-detail[data-building-detail-id]");
const toast = document.querySelector("#toast");
const flashToast = document.querySelector("#flashToast");
const sideJobButton = document.querySelector("#sideJobBtn");
const cashValue = document.querySelector("#cashValue");
const monthlyNetIncomeValue = document.querySelector("#monthlyNetIncomeValue");
const dayProgress = document.querySelector("#dayProgress");
const dayProgressText = document.querySelector("#dayProgressText");
const imageModal = document.querySelector("#imageModal");
const imageModalImg = document.querySelector("#imageModalImg");
const imageModalTitle = document.querySelector("#imageModalTitle");
const imageModalClose = document.querySelector("#imageModalClose");
const TICK_DURATION_MS = 10000;
let tickStartedAt = Date.now();
let ticking = false;
const SELECTED_BUILDING_KEY = "buildingStory.selectedBuildingId";

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
    window.setTimeout(() => toast.classList.remove("show"), 1600);
}

if (flashToast) {
    window.setTimeout(() => flashToast.classList.remove("show"), 1600);
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
            if (monthlyNetIncomeValue) {
                monthlyNetIncomeValue.textContent = result.monthlyNetIncome;
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
            window.location.reload();
            return;
        }
        if (result.notice) {
            showToast(result.notice);
            window.setTimeout(() => window.location.reload(), 1000);
        } else {
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
        dayProgressText.textContent = "이벤트 진행 중";
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
