import { setupUiInteractions } from "./app-ui.js";

const buildingSlots = document.querySelectorAll(".building-slot[data-building]");
const buildingDetails = document.querySelectorAll(".building-detail[data-building-detail-id]");
const toast = document.querySelector("#toast");
const flashToast = document.querySelector("#flashToast");
const sideJobButton = document.querySelector("#sideJobBtn");
const cashValue = document.querySelector("#cashValue");
const coinValue = document.querySelector("#coinValue");
const totalMonthlyRentValue = document.querySelector("#totalMonthlyRentValue");
const dayProgress = document.querySelector("#dayProgress");
const dayProgressText = document.querySelector("#dayProgressText");
const auctionTimer = document.querySelector(".auction-timer[data-auction-seconds]");
const TICK_DURATION_MS = 5000;
const STOCK_FEE_RATE = 0.005;
const CASH_PER_COIN = 100;
const SCROLL_RESTORE_KEY = "buildingStory.scrollY";
const SELECTED_BUILDING_KEY = "buildingStory.selectedBuildingId";
const SELECTED_STOCK_KEY = "buildingStory.selectedStockKey";
const STOCK_LIST_SCROLL_KEY = "buildingStory.stockListScrollTop";
const STOCK_ORDER_QUANTITY_KEY = "buildingStory.stockOrderQuantities";
const STOCK_EXCHANGE_QUANTITY_KEY = "buildingStory.stockExchangeQuantities";
const RECORD_PANEL_DOCKED_KEY = "buildingStory.recordPanelDocked";
const COLLAPSIBLE_PANEL_STATE_KEY = "buildingStory.collapsiblePanels";
let tickStartedAt = Date.now();
let ticking = false;
let navigating = false;

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

function setupRecordPanel() {
    const panel = document.querySelector("[data-recent-record-panel]");
    const toggle = document.querySelector("[data-record-panel-toggle]");
    if (!panel || !toggle) {
        return;
    }

    function applyRecordPanelMode(docked) {
        panel.classList.toggle("docked", docked);
        toggle.textContent = docked ? "따라오기" : "고정";
        toggle.setAttribute("aria-pressed", String(docked));
        window.localStorage.setItem(RECORD_PANEL_DOCKED_KEY, docked ? "true" : "false");
    }

    applyRecordPanelMode(window.localStorage.getItem(RECORD_PANEL_DOCKED_KEY) === "true");
    toggle.addEventListener("click", () => {
        applyRecordPanelMode(!panel.classList.contains("docked"));
    });
}

setupRecordPanel();

function setupCollapsiblePanels() {
    const panels = document.querySelectorAll(".collapsible-panel[data-collapsible-key]");
    if (panels.length === 0) {
        return;
    }

    let savedState = {};
    try {
        savedState = JSON.parse(window.localStorage.getItem(COLLAPSIBLE_PANEL_STATE_KEY) || "{}");
    } catch {
        savedState = {};
    }

    panels.forEach((panel) => {
        const key = panel.dataset.collapsibleKey;
        if (Object.prototype.hasOwnProperty.call(savedState, key)) {
            panel.open = savedState[key] === true;
        }
        panel.addEventListener("toggle", () => {
            savedState[key] = panel.open;
            window.localStorage.setItem(COLLAPSIBLE_PANEL_STATE_KEY, JSON.stringify(savedState));
        });
    });
}

setupCollapsiblePanels();

function setupStockPanel() {
    const buttons = document.querySelectorAll(".stock-company-button[data-stock-key]");
    const details = document.querySelectorAll(".stock-detail[data-stock-detail]");
    const list = document.querySelector(".stock-company-list");
    const filterButtons = document.querySelectorAll("[data-stock-filter]");
    const emptyMessage = document.querySelector(".stock-empty-message");
    if (buttons.length === 0 || details.length === 0) {
        return;
    }

    let currentFilter = window.sessionStorage.getItem("buildingStory.stockFilter") || "all";

    function visibleButtons() {
        return Array.from(buttons).filter((button) => !button.hidden);
    }

    function selectStock(key) {
        const visible = visibleButtons();
        const target = visible.find((button) => button.dataset.stockKey === key) || visible[0];
        if (!target) {
            details.forEach((detail) => detail.classList.remove("active"));
            return;
        }
        const selectedKey = target.dataset.stockKey;
        window.localStorage.setItem(SELECTED_STOCK_KEY, selectedKey);
        buttons.forEach((item) => item.classList.toggle("selected", item.dataset.stockKey === selectedKey));
        details.forEach((detail) => {
            detail.classList.toggle("active", detail.dataset.stockDetail === selectedKey);
        });
    }

    function applyFilter(filter) {
        currentFilter = filter === "owned" ? "owned" : "all";
        window.sessionStorage.setItem("buildingStory.stockFilter", currentFilter);
        filterButtons.forEach((button) => {
            button.classList.toggle("active", button.dataset.stockFilter === currentFilter);
        });
        buttons.forEach((button) => {
            button.hidden = currentFilter === "owned" && Number(button.dataset.ownedQuantity || 0) <= 0;
        });
        const hasVisibleStock = visibleButtons().length > 0;
        if (emptyMessage) {
            emptyMessage.hidden = hasVisibleStock;
        }
        selectStock(window.localStorage.getItem(SELECTED_STOCK_KEY));
    }

    buttons.forEach((button) => {
        button.addEventListener("click", () => selectStock(button.dataset.stockKey));
    });
    filterButtons.forEach((button) => {
        button.addEventListener("click", () => applyFilter(button.dataset.stockFilter));
    });
    applyFilter(currentFilter);
    if (list) {
        list.scrollTop = Number(window.sessionStorage.getItem(STOCK_LIST_SCROLL_KEY)) || 0;
        list.addEventListener("scroll", () => {
            window.sessionStorage.setItem(STOCK_LIST_SCROLL_KEY, String(list.scrollTop));
        }, { passive: true });
    }
}

setupStockPanel();

function formatStockAmount(amount, unit) {
    const safeAmount = Math.max(0, Math.trunc(Number(amount) || 0));
    if (safeAmount === 0) {
        return `0${unit}`;
    }
    let remaining = safeAmount;
    const eok = Math.floor(remaining / 100000000);
    remaining %= 100000000;
    const man = Math.floor(remaining / 10000);
    const won = remaining % 10000;
    let text = "";
    if (eok > 0) {
        text += `${eok}억`;
    }
    if (man > 0) {
        text += `${man}만`;
    }
    if (won > 0 || text === "") {
        text += `${won}`;
    }
    return `${text}${unit}`;
}

function formatCashAmount(amount) {
    return formatStockAmount(amount, "원");
}

function stockTradeFee(grossAmount) {
    return Math.ceil(Math.max(0, grossAmount) * STOCK_FEE_RATE);
}

function maxAffordableStockQuantity(coin, price) {
    let low = 0;
    let high = Math.floor(Math.max(0, coin) / Math.max(1, price));
    while (low < high) {
        const mid = Math.floor((low + high + 1) / 2);
        const grossAmount = price * mid;
        if (grossAmount + stockTradeFee(grossAmount) <= coin) {
            low = mid;
        } else {
            high = mid - 1;
        }
    }
    return low;
}

function stockPanelRoot() {
    return document.querySelector(".stock-panel");
}

function playerCoinBalance() {
    return Number(stockPanelRoot()?.dataset.playerCoin || 0);
}

function playerCashBalance() {
    return Number(stockPanelRoot()?.dataset.playerCash || 0);
}

function readStockOrderQuantities() {
    try {
        return JSON.parse(window.sessionStorage.getItem(STOCK_ORDER_QUANTITY_KEY) || "{}");
    } catch {
        return {};
    }
}

function stockOrderQuantityKey(form) {
    const detail = form?.closest(".stock-detail[data-stock-detail]");
    if (!detail || !form) {
        return "";
    }
    const side = form.hasAttribute("data-stock-buy-form") ? "buy" : "sell";
    return `${detail.dataset.stockDetail}:${side}`;
}

function saveStockOrderQuantity(form, value) {
    const key = stockOrderQuantityKey(form);
    if (!key) {
        return;
    }
    const saved = readStockOrderQuantities();
    saved[key] = String(Math.max(1, Math.trunc(Number(value) || 1)));
    window.sessionStorage.setItem(STOCK_ORDER_QUANTITY_KEY, JSON.stringify(saved));
}

function restoreStockOrderQuantities() {
    const saved = readStockOrderQuantities();
    document.querySelectorAll(".stock-order-form").forEach((form) => {
        const key = stockOrderQuantityKey(form);
        const input = form.querySelector("input[name='quantity']");
        if (key && input && saved[key]) {
            input.value = saved[key];
        }
    });
}

function readStockExchangeQuantities() {
    try {
        return JSON.parse(window.sessionStorage.getItem(STOCK_EXCHANGE_QUANTITY_KEY) || "{}");
    } catch {
        return {};
    }
}

function saveStockExchangeQuantity(form, value) {
    if (!form?.dataset.exchangeType) {
        return;
    }
    const saved = readStockExchangeQuantities();
    saved[form.dataset.exchangeType] = String(Math.max(1, Math.trunc(Number(value) || 1)));
    window.sessionStorage.setItem(STOCK_EXCHANGE_QUANTITY_KEY, JSON.stringify(saved));
}

function restoreStockExchangeQuantities() {
    const saved = readStockExchangeQuantities();
    document.querySelectorAll("[data-stock-exchange-form]").forEach((form) => {
        const input = form.querySelector("input[name='coinAmount']");
        const value = saved[form.dataset.exchangeType];
        if (input && value) {
            input.value = value;
        }
    });
}

function updateStockTradeEstimates() {
    document.querySelectorAll(".stock-detail[data-stock-detail]").forEach((detail) => {
        const price = Number(detail.dataset.stockPrice || 0);
        const ownedQuantity = Number(detail.dataset.ownedQuantity || 0);
        const maxBuyQuantity = maxAffordableStockQuantity(playerCoinBalance(), price);

        const buyInput = detail.querySelector("[data-stock-buy-form] input[name='quantity']");
        const buyPreview = detail.querySelector("[data-stock-buy-preview]");
        if (buyInput && buyPreview) {
            const quantity = Math.max(0, Number(buyInput.value || 0));
            const grossAmount = price * quantity;
            const fee = stockTradeFee(grossAmount);
            buyPreview.textContent = `최대 ${maxBuyQuantity}주 / 총 ${formatStockAmount(grossAmount + fee, "코인")} / 수수료 ${formatStockAmount(fee, "코인")}`;
            buyInput.max = String(Math.max(1, maxBuyQuantity));
        }

        const sellInput = detail.querySelector("[data-stock-sell-form] input[name='quantity']");
        const sellPreview = detail.querySelector("[data-stock-sell-preview]");
        if (sellInput && sellPreview) {
            const quantity = Math.max(0, Number(sellInput.value || 0));
            const grossAmount = price * quantity;
            const fee = stockTradeFee(grossAmount);
            const payout = Math.max(0, grossAmount - fee);
            sellPreview.textContent = `보유 ${ownedQuantity}주 / 수령 ${formatStockAmount(payout, "코인")} / 수수료 ${formatStockAmount(fee, "코인")}`;
            sellInput.max = String(Math.max(1, ownedQuantity));
        }
    });
}

function updateStockExchangeEstimates() {
    document.querySelectorAll("[data-stock-exchange-form]").forEach((form) => {
        const input = form.querySelector("input[name='coinAmount']");
        const preview = form.querySelector("[data-exchange-preview]");
        if (!input || !preview) {
            return;
        }
        const coinAmount = Math.max(0, Number(input.value || 0));
        const cashAmount = coinAmount * CASH_PER_COIN;
        if (form.dataset.exchangeType === "cash-to-coin") {
            const maxCoin = Math.floor(playerCashBalance() / CASH_PER_COIN);
            preview.textContent = `최대 ${formatStockAmount(maxCoin, "코인")} / 필요 ${formatCashAmount(cashAmount)}`;
            input.max = String(Math.max(1, maxCoin));
        } else {
            preview.textContent = `보유 ${formatStockAmount(playerCoinBalance(), "코인")} / 수령 ${formatCashAmount(cashAmount)}`;
            input.max = String(Math.max(1, playerCoinBalance()));
        }
    });
}

function setupStockEstimateInputs() {
    restoreStockOrderQuantities();
    restoreStockExchangeQuantities();
    document.querySelectorAll(".stock-order-form input[name='quantity']").forEach((input) => {
        input.addEventListener("input", () => {
            saveStockOrderQuantity(input.closest(".stock-order-form"), input.value);
            updateStockTradeEstimates();
        });
    });
    document.querySelectorAll("[data-stock-exchange-form] input[name='coinAmount']").forEach((input) => {
        input.addEventListener("input", () => {
            saveStockExchangeQuantity(input.closest("[data-stock-exchange-form]"), input.value);
            updateStockExchangeEstimates();
        });
    });
    updateStockTradeEstimates();
    updateStockExchangeEstimates();
}

setupStockEstimateInputs();

document.addEventListener("click", (event) => {
    const button = event.target.closest("[data-quantity-action]");
    if (!button) {
        return;
    }
    const form = button.closest(".stock-order-form");
    const detail = button.closest(".stock-detail[data-stock-detail]");
    const input = form?.querySelector("input[name='quantity']");
    if (!form || !detail || !input) {
        return;
    }
    event.preventDefault();
    event.stopPropagation();
    const price = Number(detail.dataset.stockPrice || 0);
    const ownedQuantity = Number(detail.dataset.ownedQuantity || 0);
    const maxQuantity = form.hasAttribute("data-stock-buy-form")
        ? maxAffordableStockQuantity(playerCoinBalance(), price)
        : ownedQuantity;
    const currentQuantity = Math.max(0, Number(input.value || 0));
    let nextQuantity = currentQuantity;
    if (button.dataset.quantityAction === "plus") {
        nextQuantity += Number(button.dataset.quantityValue || 0);
    } else if (button.dataset.quantityAction === "half") {
        nextQuantity = Math.max(1, Math.floor(maxQuantity / 2));
    } else if (button.dataset.quantityAction === "max") {
        nextQuantity = maxQuantity;
    }
    input.value = String(Math.max(1, Math.min(Math.max(1, maxQuantity), nextQuantity)));
    saveStockOrderQuantity(form, input.value);
    input.dispatchEvent(new Event("input", { bubbles: true }));
    updateStockTradeEstimates();
}, true);

document.addEventListener("click", (event) => {
    const button = event.target.closest("[data-exchange-action]");
    if (!button) {
        return;
    }
    const form = button.closest("[data-stock-exchange-form]");
    const input = form?.querySelector("input[name='coinAmount']");
    if (!form || !input) {
        return;
    }
    event.preventDefault();
    event.stopPropagation();
    const maxCoin = form.dataset.exchangeType === "cash-to-coin"
        ? Math.floor(playerCashBalance() / CASH_PER_COIN)
        : playerCoinBalance();
    let nextAmount = Number(input.value || 0);
    if (button.dataset.exchangeAction === "max") {
        nextAmount = maxCoin;
    } else if (button.dataset.exchangeAction === "set") {
        nextAmount = Number(button.dataset.exchangeValue || 0);
    }
    input.value = String(Math.max(1, Math.min(Math.max(1, maxCoin), Math.trunc(nextAmount) || 1)));
    saveStockExchangeQuantity(form, input.value);
    input.dispatchEvent(new Event("input", { bubbles: true }));
    updateStockExchangeEstimates();
}, true);

function setupStockTradeHistoryFilters() {
    const panel = document.querySelector("[data-stock-history-panel]");
    if (!panel) {
        return;
    }
    const typeButtons = panel.querySelectorAll("[data-trade-filter]");
    const stockSelect = panel.querySelector("[data-trade-stock-filter]");
    const rows = panel.querySelectorAll(".stock-trade-history li[data-trade-type]");
    const emptyMessage = panel.querySelector(".stock-trade-empty-message");
    let currentType = "all";

    function applyHistoryFilter() {
        const stockKey = stockSelect?.value || "all";
        let visibleCount = 0;
        rows.forEach((row) => {
            const typeMatches = currentType === "all" || row.dataset.tradeType === currentType;
            const stockMatches = stockKey === "all" || row.dataset.tradeStockKey === stockKey;
            const visible = typeMatches && stockMatches;
            row.hidden = !visible;
            if (visible) {
                visibleCount += 1;
            }
        });
        if (emptyMessage) {
            emptyMessage.hidden = visibleCount > 0 || rows.length === 0;
        }
    }

    typeButtons.forEach((button) => {
        button.addEventListener("click", () => {
            currentType = button.dataset.tradeFilter || "all";
            typeButtons.forEach((item) => item.classList.toggle("active", item === button));
            applyHistoryFilter();
        });
    });
    stockSelect?.addEventListener("change", applyHistoryFilter);
    applyHistoryFilter();
}

setupStockTradeHistoryFilters();

function setupStockExchangeForms() {
    document.querySelectorAll("[data-stock-exchange-form]").forEach((form) => {
        form.addEventListener("submit", async (event) => {
            event.preventDefault();
            const button = form.querySelector("button[type='submit']");
            if (button) {
                button.disabled = true;
            }
            try {
                const response = await fetch(form.action, {
                    method: "POST",
                    body: new FormData(form)
                });
                const result = await response.json();
                if (result.redirect) {
                    navigating = true;
                    window.location.href = result.redirect;
                    return;
                }
                if (cashValue && result.cash) {
                    cashValue.textContent = result.cash;
                }
                if (coinValue && result.coin) {
                    coinValue.textContent = result.coin;
                }
                if (result.cashRaw && stockPanelRoot()) {
                    stockPanelRoot().dataset.playerCash = result.cashRaw;
                }
                if (result.coinRaw && stockPanelRoot()) {
                    stockPanelRoot().dataset.playerCoin = result.coinRaw;
                }
                updateStockTradeEstimates();
                updateStockExchangeEstimates();
                showToast(result.notice);
            } catch (error) {
                console.warn("stock exchange failed", error);
                showToast("교환 처리 실패");
            } finally {
                if (button) {
                    button.disabled = false;
                }
            }
        });
    });
}

setupStockExchangeForms();

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
    if (navigating || ticking || document.body.classList.contains("game-paused")) {
        return;
    }
    ticking = true;
    try {
        const view = document.querySelector(".stock-panel") ? "stocks" : "city";
        const response = await fetch(`/tick?view=${view}`, { method: "POST" });
        const result = await response.json();
        if (result.redirect) {
            navigating = true;
            window.location.href = result.redirect;
            return;
        }
        if (result.event || result.auction) {
            saveScrollPosition();
            navigating = true;
            window.location.reload();
            return;
        }
        if (result.notice) {
            showToast(result.notice);
            window.setTimeout(() => {
                saveScrollPosition();
                navigating = true;
                window.location.reload();
            }, 1000);
        } else {
            saveScrollPosition();
            navigating = true;
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
    if (navigating) {
        return;
    }
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

if (document.querySelector(".city-panel") || document.querySelector(".stock-panel")) {
    window.setInterval(updateDayProgress, 100);
}
