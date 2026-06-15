const buildingSlots = document.querySelectorAll(".building-slot[data-building]");
const selectedBuilding = document.querySelector("#selectedBuilding");
const toast = document.querySelector("#toast");
const flashToast = document.querySelector("#flashToast");

buildingSlots.forEach((slot) => {
    slot.addEventListener("click", () => {
        buildingSlots.forEach((item) => item.classList.remove("selected"));
        slot.classList.add("selected");
        if (selectedBuilding) {
            selectedBuilding.textContent = slot.dataset.building;
        }
    });
});

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

if (document.querySelector(".city-panel")) {
    window.setInterval(async () => {
        if (document.body.classList.contains("game-paused")) {
            return;
        }
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
        }
    }, 10000);
}
