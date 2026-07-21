# app.js 코드 주석형 해설

원본 파일: `src/main/resources/static/app.js`

형식:
- 원본 코드를 그대로 배치한다.
- 필요한 코드 바로 아래에 해설 주석을 붙인다.
- import/package/단순 선언은 과하게 설명하지 않는다.

```javascript
import { setupUiInteractions } from "./app-ui.js";

/*
 * app.js는 서버가 렌더링한 HTML 위에 상호작용을 붙이는 파일이다.
 *
 * 서버가 가격, 보유 수량, 이벤트 상태 같은 핵심 데이터를 계산하고,
 * 이 파일은 클릭 처리, 선택 상태 저장, 미리보기 계산, 자동 시간 진행을 담당한다.
 * 게임 날짜를 실제로 증가시키는 곳은 서버의 /tick API이며, 브라우저는 일정 시간마다
 * 그 API를 한 번 호출할 뿐이다.
 */

const buildingSlots = document.querySelectorAll(".building-slot[data-building]");
// 해설: DOM에서 특정 HTML 요소를 찾는다. 이후 텍스트, 클래스, 속성 등을 조작한다.
const buildingDetails = document.querySelectorAll(".building-detail[data-building-detail-id]");
// 해설: DOM에서 특정 HTML 요소를 찾는다. 이후 텍스트, 클래스, 속성 등을 조작한다.
const toast = document.querySelector("#toast");
// 해설: DOM에서 특정 HTML 요소를 찾는다. 이후 텍스트, 클래스, 속성 등을 조작한다.
const flashToast = document.querySelector("#flashToast");
// 해설: DOM에서 특정 HTML 요소를 찾는다. 이후 텍스트, 클래스, 속성 등을 조작한다.
const sideJobButton = document.querySelector("#sideJobBtn");
// 해설: DOM에서 특정 HTML 요소를 찾는다. 이후 텍스트, 클래스, 속성 등을 조작한다.
const cashValue = document.querySelector("#cashValue");
// 해설: DOM에서 특정 HTML 요소를 찾는다. 이후 텍스트, 클래스, 속성 등을 조작한다.
const coinValue = document.querySelector("#coinValue");
// 해설: DOM에서 특정 HTML 요소를 찾는다. 이후 텍스트, 클래스, 속성 등을 조작한다.
const totalMonthlyRentValue = document.querySelector("#totalMonthlyRentValue");
// 해설: DOM에서 특정 HTML 요소를 찾는다. 이후 텍스트, 클래스, 속성 등을 조작한다.
const dayProgress = document.querySelector("#dayProgress");
// 해설: DOM에서 특정 HTML 요소를 찾는다. 이후 텍스트, 클래스, 속성 등을 조작한다.
const dayProgressText = document.querySelector("#dayProgressText");
// 해설: DOM에서 특정 HTML 요소를 찾는다. 이후 텍스트, 클래스, 속성 등을 조작한다.
const auctionTimer = document.querySelector(".auction-timer[data-auction-seconds]");
// 해설: DOM에서 특정 HTML 요소를 찾는다. 이후 텍스트, 클래스, 속성 등을 조작한다.
const TICK_DURATION_MS = 5000;
const STOCK_FEE_RATE = 0.0025;
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
// ticking은 /tick 중복 호출을 막는 플래그다. 이 값이 없으면 느린 네트워크에서 하루가 2번 지날 수 있다.
let ticking = false;
// navigating은 reload/redirect가 예정된 상태다. 화면 전환 중 새 tick이나 UI 갱신이 끼어드는 것을 막는다.
let navigating = false;

if ("scrollRestoration" in window.history) {
// 해설: 조건 분기다. 현재 화면 상태나 응답 값에 따라 다른 동작을 수행한다.
    window.history.scrollRestoration = "manual";
}

function saveScrollPosition() {
// 해설: 함수 또는 화살표 함수다. 반복되는 프론트 동작을 묶어 재사용한다.
    window.sessionStorage.setItem(SCROLL_RESTORE_KEY, String(window.scrollY));
}

function restoreScrollPosition() {
// 해설: 함수 또는 화살표 함수다. 반복되는 프론트 동작을 묶어 재사용한다.
    const savedY = window.sessionStorage.getItem(SCROLL_RESTORE_KEY);
    if (savedY === null) {
    // 해설: 조건 분기다. 현재 화면 상태나 응답 값에 따라 다른 동작을 수행한다.
        document.documentElement.classList.remove("restore-scroll-pending");
        // 해설: HTML 요소의 CSS 클래스를 추가/삭제/토글해 화면 상태를 바꾼다.
        return;
    }
    window.sessionStorage.removeItem(SCROLL_RESTORE_KEY);
    window.requestAnimationFrame(() => {
    // 해설: 함수 또는 화살표 함수다. 반복되는 프론트 동작을 묶어 재사용한다.
        window.scrollTo(0, Number(savedY) || 0);
        document.documentElement.classList.remove("restore-scroll-pending");
        // 해설: HTML 요소의 CSS 클래스를 추가/삭제/토글해 화면 상태를 바꾼다.
    });
}

restoreScrollPosition();

function selectBuilding(buildingId) {
// 해설: 함수 또는 화살표 함수다. 반복되는 프론트 동작을 묶어 재사용한다.
    const targetSlot = Array.from(buildingSlots).find((slot) => slot.dataset.buildingId === buildingId) || buildingSlots[0];
    // 해설: 함수 또는 화살표 함수다. 반복되는 프론트 동작을 묶어 재사용한다.
    if (!targetSlot) {
    // 해설: 조건 분기다. 현재 화면 상태나 응답 값에 따라 다른 동작을 수행한다.
        return;
    }
    const selectedId = targetSlot.dataset.buildingId;
    window.localStorage.setItem(SELECTED_BUILDING_KEY, selectedId);
    // 해설: 브라우저 로컬 저장소를 사용한다. 새로고침 후에도 간단한 화면 설정을 유지할 수 있다.

    buildingSlots.forEach((item) => item.classList.toggle("selected", item.dataset.buildingId === selectedId));
    // 해설: 함수 또는 화살표 함수다. 반복되는 프론트 동작을 묶어 재사용한다.
    buildingDetails.forEach((detail) => {
    // 해설: 함수 또는 화살표 함수다. 반복되는 프론트 동작을 묶어 재사용한다.
        detail.classList.toggle("active", detail.dataset.buildingDetailId === selectedId);
        // 해설: HTML 요소의 CSS 클래스를 추가/삭제/토글해 화면 상태를 바꾼다.
    });
}

buildingSlots.forEach((slot) => {
// 해설: 함수 또는 화살표 함수다. 반복되는 프론트 동작을 묶어 재사용한다.
    slot.addEventListener("click", () => selectBuilding(slot.dataset.buildingId));
    // 해설: 함수 또는 화살표 함수다. 반복되는 프론트 동작을 묶어 재사용한다.
});

if (buildingSlots.length > 0) {
// 해설: 조건 분기다. 현재 화면 상태나 응답 값에 따라 다른 동작을 수행한다.
    selectBuilding(window.localStorage.getItem(SELECTED_BUILDING_KEY));
    // 해설: 브라우저 로컬 저장소를 사용한다. 새로고침 후에도 간단한 화면 설정을 유지할 수 있다.
}

function setupRecordPanel() {
// 해설: 함수 또는 화살표 함수다. 반복되는 프론트 동작을 묶어 재사용한다.
    const panel = document.querySelector("[data-recent-record-panel]");
    // 해설: DOM에서 특정 HTML 요소를 찾는다. 이후 텍스트, 클래스, 속성 등을 조작한다.
    const toggle = document.querySelector("[data-record-panel-toggle]");
    // 해설: DOM에서 특정 HTML 요소를 찾는다. 이후 텍스트, 클래스, 속성 등을 조작한다.
    if (!panel || !toggle) {
    // 해설: 조건 분기다. 현재 화면 상태나 응답 값에 따라 다른 동작을 수행한다.
        return;
    }

    function applyRecordPanelMode(docked) {
    // 해설: 함수 또는 화살표 함수다. 반복되는 프론트 동작을 묶어 재사용한다.
        panel.classList.toggle("docked", docked);
        // 해설: HTML 요소의 CSS 클래스를 추가/삭제/토글해 화면 상태를 바꾼다.
        toggle.textContent = docked ? "따라오기" : "고정";
        toggle.setAttribute("aria-pressed", String(docked));
        window.localStorage.setItem(RECORD_PANEL_DOCKED_KEY, docked ? "true" : "false");
        // 해설: 브라우저 로컬 저장소를 사용한다. 새로고침 후에도 간단한 화면 설정을 유지할 수 있다.
    }

    applyRecordPanelMode(window.localStorage.getItem(RECORD_PANEL_DOCKED_KEY) === "true");
    // 해설: 브라우저 로컬 저장소를 사용한다. 새로고침 후에도 간단한 화면 설정을 유지할 수 있다.
    toggle.addEventListener("click", () => {
    // 해설: 함수 또는 화살표 함수다. 반복되는 프론트 동작을 묶어 재사용한다.
        applyRecordPanelMode(!panel.classList.contains("docked"));
        // 해설: HTML 요소의 CSS 클래스를 추가/삭제/토글해 화면 상태를 바꾼다.
    });
}

setupRecordPanel();

function setupCollapsiblePanels() {
// 해설: 함수 또는 화살표 함수다. 반복되는 프론트 동작을 묶어 재사용한다.
    const panels = document.querySelectorAll(".collapsible-panel[data-collapsible-key]");
    // 해설: DOM에서 특정 HTML 요소를 찾는다. 이후 텍스트, 클래스, 속성 등을 조작한다.
    if (panels.length === 0) {
    // 해설: 조건 분기다. 현재 화면 상태나 응답 값에 따라 다른 동작을 수행한다.
        return;
    }

    let savedState = {};
    try {
        savedState = JSON.parse(window.localStorage.getItem(COLLAPSIBLE_PANEL_STATE_KEY) || "{}");
        // 해설: 서버와 주고받는 데이터를 JSON 형식으로 변환하거나 해석한다.
    } catch {
        savedState = {};
    }

    panels.forEach((panel) => {
    // 해설: 함수 또는 화살표 함수다. 반복되는 프론트 동작을 묶어 재사용한다.
        const key = panel.dataset.collapsibleKey;
        if (Object.prototype.hasOwnProperty.call(savedState, key)) {
        // 해설: 조건 분기다. 현재 화면 상태나 응답 값에 따라 다른 동작을 수행한다.
            panel.open = savedState[key] === true;
        }
        panel.addEventListener("toggle", () => {
        // 해설: 함수 또는 화살표 함수다. 반복되는 프론트 동작을 묶어 재사용한다.
            savedState[key] = panel.open;
            window.localStorage.setItem(COLLAPSIBLE_PANEL_STATE_KEY, JSON.stringify(savedState));
            // 해설: 서버와 주고받는 데이터를 JSON 형식으로 변환하거나 해석한다.
        });
    });
}

setupCollapsiblePanels();

function setupStockPanel() {
// 해설: 함수 또는 화살표 함수다. 반복되는 프론트 동작을 묶어 재사용한다.
    /*
     * 주식 목록과 상세 패널은 서버가 모든 종목 HTML을 미리 렌더링한다.
     * JS는 사용자가 고른 종목만 active로 표시하고, 선택값을 localStorage에 저장해 새로고침 후에도 복원한다.
     // 해설: 브라우저 로컬 저장소를 사용한다. 새로고침 후에도 간단한 화면 설정을 유지할 수 있다.
     */
    const buttons = document.querySelectorAll(".stock-company-button[data-stock-key]");
    // 해설: DOM에서 특정 HTML 요소를 찾는다. 이후 텍스트, 클래스, 속성 등을 조작한다.
    const details = document.querySelectorAll(".stock-detail[data-stock-detail]");
    // 해설: DOM에서 특정 HTML 요소를 찾는다. 이후 텍스트, 클래스, 속성 등을 조작한다.
    const list = document.querySelector(".stock-company-list");
    // 해설: DOM에서 특정 HTML 요소를 찾는다. 이후 텍스트, 클래스, 속성 등을 조작한다.
    const filterButtons = document.querySelectorAll("[data-stock-filter]");
    // 해설: DOM에서 특정 HTML 요소를 찾는다. 이후 텍스트, 클래스, 속성 등을 조작한다.
    const emptyMessage = document.querySelector(".stock-empty-message");
    // 해설: DOM에서 특정 HTML 요소를 찾는다. 이후 텍스트, 클래스, 속성 등을 조작한다.
    if (buttons.length === 0 || details.length === 0) {
    // 해설: 조건 분기다. 현재 화면 상태나 응답 값에 따라 다른 동작을 수행한다.
        return;
    }

    let currentFilter = window.sessionStorage.getItem("buildingStory.stockFilter") || "all";

    function visibleButtons() {
    // 해설: 함수 또는 화살표 함수다. 반복되는 프론트 동작을 묶어 재사용한다.
        return Array.from(buttons).filter((button) => !button.hidden);
        // 해설: 함수 또는 화살표 함수다. 반복되는 프론트 동작을 묶어 재사용한다.
    }

    function selectStock(key) {
    // 해설: 함수 또는 화살표 함수다. 반복되는 프론트 동작을 묶어 재사용한다.
        const visible = visibleButtons();
        const target = visible.find((button) => button.dataset.stockKey === key) || visible[0];
        // 해설: 함수 또는 화살표 함수다. 반복되는 프론트 동작을 묶어 재사용한다.
        if (!target) {
        // 해설: 조건 분기다. 현재 화면 상태나 응답 값에 따라 다른 동작을 수행한다.
            details.forEach((detail) => detail.classList.remove("active"));
            // 해설: 함수 또는 화살표 함수다. 반복되는 프론트 동작을 묶어 재사용한다.
            return;
        }
        const selectedKey = target.dataset.stockKey;
        window.localStorage.setItem(SELECTED_STOCK_KEY, selectedKey);
        // 해설: 브라우저 로컬 저장소를 사용한다. 새로고침 후에도 간단한 화면 설정을 유지할 수 있다.
        buttons.forEach((item) => item.classList.toggle("selected", item.dataset.stockKey === selectedKey));
        // 해설: 함수 또는 화살표 함수다. 반복되는 프론트 동작을 묶어 재사용한다.
        details.forEach((detail) => {
        // 해설: 함수 또는 화살표 함수다. 반복되는 프론트 동작을 묶어 재사용한다.
            detail.classList.toggle("active", detail.dataset.stockDetail === selectedKey);
            // 해설: HTML 요소의 CSS 클래스를 추가/삭제/토글해 화면 상태를 바꾼다.
        });
    }

    function applyFilter(filter) {
    // 해설: 함수 또는 화살표 함수다. 반복되는 프론트 동작을 묶어 재사용한다.
        currentFilter = filter === "owned" ? "owned" : "all";
        window.sessionStorage.setItem("buildingStory.stockFilter", currentFilter);
        filterButtons.forEach((button) => {
        // 해설: 함수 또는 화살표 함수다. 반복되는 프론트 동작을 묶어 재사용한다.
            button.classList.toggle("active", button.dataset.stockFilter === currentFilter);
            // 해설: HTML 요소의 CSS 클래스를 추가/삭제/토글해 화면 상태를 바꾼다.
        });
        buttons.forEach((button) => {
        // 해설: 함수 또는 화살표 함수다. 반복되는 프론트 동작을 묶어 재사용한다.
            button.hidden = currentFilter === "owned" && Number(button.dataset.ownedQuantity || 0) <= 0;
        });
        const hasVisibleStock = visibleButtons().length > 0;
        if (emptyMessage) {
        // 해설: 조건 분기다. 현재 화면 상태나 응답 값에 따라 다른 동작을 수행한다.
            emptyMessage.hidden = hasVisibleStock;
        }
        selectStock(window.localStorage.getItem(SELECTED_STOCK_KEY));
        // 해설: 브라우저 로컬 저장소를 사용한다. 새로고침 후에도 간단한 화면 설정을 유지할 수 있다.
    }

    buttons.forEach((button) => {
    // 해설: 함수 또는 화살표 함수다. 반복되는 프론트 동작을 묶어 재사용한다.
        button.addEventListener("click", () => selectStock(button.dataset.stockKey));
        // 해설: 함수 또는 화살표 함수다. 반복되는 프론트 동작을 묶어 재사용한다.
    });
    filterButtons.forEach((button) => {
    // 해설: 함수 또는 화살표 함수다. 반복되는 프론트 동작을 묶어 재사용한다.
        button.addEventListener("click", () => applyFilter(button.dataset.stockFilter));
        // 해설: 함수 또는 화살표 함수다. 반복되는 프론트 동작을 묶어 재사용한다.
    });
    applyFilter(currentFilter);
    if (list) {
    // 해설: 조건 분기다. 현재 화면 상태나 응답 값에 따라 다른 동작을 수행한다.
        list.scrollTop = Number(window.sessionStorage.getItem(STOCK_LIST_SCROLL_KEY)) || 0;
        list.addEventListener("scroll", () => {
        // 해설: 함수 또는 화살표 함수다. 반복되는 프론트 동작을 묶어 재사용한다.
            window.sessionStorage.setItem(STOCK_LIST_SCROLL_KEY, String(list.scrollTop));
        }, { passive: true });
    }
}

setupStockPanel();

function formatStockAmount(amount, unit) {
// 해설: 함수 또는 화살표 함수다. 반복되는 프론트 동작을 묶어 재사용한다.
    const safeAmount = Math.max(0, Math.trunc(Number(amount) || 0));
    if (safeAmount === 0) {
    // 해설: 조건 분기다. 현재 화면 상태나 응답 값에 따라 다른 동작을 수행한다.
        return `0${unit}`;
    }
    let remaining = safeAmount;
    const eok = Math.floor(remaining / 100000000);
    remaining %= 100000000;
    const man = Math.floor(remaining / 10000);
    const won = remaining % 10000;
    let text = "";
    if (eok > 0) {
    // 해설: 조건 분기다. 현재 화면 상태나 응답 값에 따라 다른 동작을 수행한다.
        text += `${eok}억`;
    }
    if (man > 0) {
    // 해설: 조건 분기다. 현재 화면 상태나 응답 값에 따라 다른 동작을 수행한다.
        text += `${man}만`;
    }
    if (won > 0 || text === "") {
    // 해설: 조건 분기다. 현재 화면 상태나 응답 값에 따라 다른 동작을 수행한다.
        text += `${won}`;
    }
    return `${text}${unit}`;
}

function formatCashAmount(amount) {
// 해설: 함수 또는 화살표 함수다. 반복되는 프론트 동작을 묶어 재사용한다.
    return formatStockAmount(amount, "원");
}

function stockTradeFee(grossAmount) {
// 해설: 함수 또는 화살표 함수다. 반복되는 프론트 동작을 묶어 재사용한다.
    return Math.ceil(Math.max(0, grossAmount) * STOCK_FEE_RATE);
}

function maxAffordableStockQuantity(coin, price) {
// 해설: 함수 또는 화살표 함수다. 반복되는 프론트 동작을 묶어 재사용한다.
    let low = 0;
    let high = Math.floor(Math.max(0, coin) / Math.max(1, price));
    while (low < high) {
        const mid = Math.floor((low + high + 1) / 2);
        const grossAmount = price * mid;
        if (grossAmount + stockTradeFee(grossAmount) <= coin) {
        // 해설: 조건 분기다. 현재 화면 상태나 응답 값에 따라 다른 동작을 수행한다.
            low = mid;
        } else {
            high = mid - 1;
        }
    }
    return low;
}

function stockPanelRoot() {
// 해설: 함수 또는 화살표 함수다. 반복되는 프론트 동작을 묶어 재사용한다.
    return document.querySelector(".stock-panel");
    // 해설: DOM에서 특정 HTML 요소를 찾는다. 이후 텍스트, 클래스, 속성 등을 조작한다.
}

function playerCoinBalance() {
// 해설: 함수 또는 화살표 함수다. 반복되는 프론트 동작을 묶어 재사용한다.
    return Number(stockPanelRoot()?.dataset.playerCoin || 0);
}

function playerCashBalance() {
// 해설: 함수 또는 화살표 함수다. 반복되는 프론트 동작을 묶어 재사용한다.
    return Number(stockPanelRoot()?.dataset.playerCash || 0);
}

function readStockOrderQuantities() {
// 해설: 함수 또는 화살표 함수다. 반복되는 프론트 동작을 묶어 재사용한다.
    try {
        return JSON.parse(window.sessionStorage.getItem(STOCK_ORDER_QUANTITY_KEY) || "{}");
        // 해설: 서버와 주고받는 데이터를 JSON 형식으로 변환하거나 해석한다.
    } catch {
        return {};
    }
}

function stockOrderQuantityKey(form) {
// 해설: 함수 또는 화살표 함수다. 반복되는 프론트 동작을 묶어 재사용한다.
    const detail = form?.closest(".stock-detail[data-stock-detail]");
    if (!detail || !form) {
    // 해설: 조건 분기다. 현재 화면 상태나 응답 값에 따라 다른 동작을 수행한다.
        return "";
    }
    const side = form.hasAttribute("data-stock-buy-form") ? "buy" : "sell";
    return `${detail.dataset.stockDetail}:${side}`;
}

function saveStockOrderQuantity(form, value) {
// 해설: 함수 또는 화살표 함수다. 반복되는 프론트 동작을 묶어 재사용한다.
    const key = stockOrderQuantityKey(form);
    if (!key) {
    // 해설: 조건 분기다. 현재 화면 상태나 응답 값에 따라 다른 동작을 수행한다.
        return;
    }
    const saved = readStockOrderQuantities();
    saved[key] = String(Math.max(1, Math.trunc(Number(value) || 1)));
    window.sessionStorage.setItem(STOCK_ORDER_QUANTITY_KEY, JSON.stringify(saved));
    // 해설: 서버와 주고받는 데이터를 JSON 형식으로 변환하거나 해석한다.
}

function restoreStockOrderQuantities() {
// 해설: 함수 또는 화살표 함수다. 반복되는 프론트 동작을 묶어 재사용한다.
    const saved = readStockOrderQuantities();
    document.querySelectorAll(".stock-order-form").forEach((form) => {
    // 해설: 함수 또는 화살표 함수다. 반복되는 프론트 동작을 묶어 재사용한다.
        const key = stockOrderQuantityKey(form);
        const input = form.querySelector("input[name='quantity']");
        // 해설: DOM에서 특정 HTML 요소를 찾는다. 이후 텍스트, 클래스, 속성 등을 조작한다.
        if (key && input && saved[key]) {
        // 해설: 조건 분기다. 현재 화면 상태나 응답 값에 따라 다른 동작을 수행한다.
            input.value = saved[key];
        }
    });
}

function readStockExchangeQuantities() {
// 해설: 함수 또는 화살표 함수다. 반복되는 프론트 동작을 묶어 재사용한다.
    try {
        return JSON.parse(window.sessionStorage.getItem(STOCK_EXCHANGE_QUANTITY_KEY) || "{}");
        // 해설: 서버와 주고받는 데이터를 JSON 형식으로 변환하거나 해석한다.
    } catch {
        return {};
    }
}

function saveStockExchangeQuantity(form, value) {
// 해설: 함수 또는 화살표 함수다. 반복되는 프론트 동작을 묶어 재사용한다.
    if (!form?.dataset.exchangeType) {
    // 해설: 조건 분기다. 현재 화면 상태나 응답 값에 따라 다른 동작을 수행한다.
        return;
    }
    const saved = readStockExchangeQuantities();
    saved[form.dataset.exchangeType] = String(Math.max(1, Math.trunc(Number(value) || 1)));
    window.sessionStorage.setItem(STOCK_EXCHANGE_QUANTITY_KEY, JSON.stringify(saved));
    // 해설: 서버와 주고받는 데이터를 JSON 형식으로 변환하거나 해석한다.
}

function restoreStockExchangeQuantities() {
// 해설: 함수 또는 화살표 함수다. 반복되는 프론트 동작을 묶어 재사용한다.
    const saved = readStockExchangeQuantities();
    document.querySelectorAll("[data-stock-exchange-form]").forEach((form) => {
    // 해설: 함수 또는 화살표 함수다. 반복되는 프론트 동작을 묶어 재사용한다.
        const input = form.querySelector("input[name='coinAmount']");
        // 해설: DOM에서 특정 HTML 요소를 찾는다. 이후 텍스트, 클래스, 속성 등을 조작한다.
        const value = saved[form.dataset.exchangeType];
        if (input && value) {
        // 해설: 조건 분기다. 현재 화면 상태나 응답 값에 따라 다른 동작을 수행한다.
            input.value = value;
        }
    });
}

function updateStockTradeEstimates() {
// 해설: 함수 또는 화살표 함수다. 반복되는 프론트 동작을 묶어 재사용한다.
    // 매수/매도 미리보기는 클라이언트에서 즉시 계산한다. 실제 체결 가능 여부는 서버가 다시 검증한다.
    document.querySelectorAll(".stock-detail[data-stock-detail]").forEach((detail) => {
    // 해설: 함수 또는 화살표 함수다. 반복되는 프론트 동작을 묶어 재사용한다.
        const price = Number(detail.dataset.stockPrice || 0);
        const ownedQuantity = Number(detail.dataset.ownedQuantity || 0);
        const maxBuyQuantity = maxAffordableStockQuantity(playerCoinBalance(), price);

        const buyInput = detail.querySelector("[data-stock-buy-form] input[name='quantity']");
        // 해설: DOM에서 특정 HTML 요소를 찾는다. 이후 텍스트, 클래스, 속성 등을 조작한다.
        const buyPreview = detail.querySelector("[data-stock-buy-preview]");
        // 해설: DOM에서 특정 HTML 요소를 찾는다. 이후 텍스트, 클래스, 속성 등을 조작한다.
        if (buyInput && buyPreview) {
        // 해설: 조건 분기다. 현재 화면 상태나 응답 값에 따라 다른 동작을 수행한다.
            const quantity = Math.max(0, Number(buyInput.value || 0));
            const grossAmount = price * quantity;
            const fee = stockTradeFee(grossAmount);
            buyPreview.textContent = `최대 ${maxBuyQuantity}주 / 총 ${formatStockAmount(grossAmount + fee, "코인")} / 수수료 ${formatStockAmount(fee, "코인")}`;
            buyInput.max = String(Math.max(1, maxBuyQuantity));
        }

        const sellInput = detail.querySelector("[data-stock-sell-form] input[name='quantity']");
        // 해설: DOM에서 특정 HTML 요소를 찾는다. 이후 텍스트, 클래스, 속성 등을 조작한다.
        const sellPreview = detail.querySelector("[data-stock-sell-preview]");
        // 해설: DOM에서 특정 HTML 요소를 찾는다. 이후 텍스트, 클래스, 속성 등을 조작한다.
        if (sellInput && sellPreview) {
        // 해설: 조건 분기다. 현재 화면 상태나 응답 값에 따라 다른 동작을 수행한다.
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
// 해설: 함수 또는 화살표 함수다. 반복되는 프론트 동작을 묶어 재사용한다.
    document.querySelectorAll("[data-stock-exchange-form]").forEach((form) => {
    // 해설: 함수 또는 화살표 함수다. 반복되는 프론트 동작을 묶어 재사용한다.
        const input = form.querySelector("input[name='coinAmount']");
        // 해설: DOM에서 특정 HTML 요소를 찾는다. 이후 텍스트, 클래스, 속성 등을 조작한다.
        const preview = form.querySelector("[data-exchange-preview]");
        // 해설: DOM에서 특정 HTML 요소를 찾는다. 이후 텍스트, 클래스, 속성 등을 조작한다.
        if (!input || !preview) {
        // 해설: 조건 분기다. 현재 화면 상태나 응답 값에 따라 다른 동작을 수행한다.
            return;
        }
        const coinAmount = Math.max(0, Number(input.value || 0));
        const cashAmount = coinAmount * CASH_PER_COIN;
        if (form.dataset.exchangeType === "cash-to-coin") {
        // 해설: 조건 분기다. 현재 화면 상태나 응답 값에 따라 다른 동작을 수행한다.
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
// 해설: 함수 또는 화살표 함수다. 반복되는 프론트 동작을 묶어 재사용한다.
    restoreStockOrderQuantities();
    restoreStockExchangeQuantities();
    document.querySelectorAll(".stock-order-form input[name='quantity']").forEach((input) => {
    // 해설: 함수 또는 화살표 함수다. 반복되는 프론트 동작을 묶어 재사용한다.
        input.addEventListener("input", () => {
        // 해설: 함수 또는 화살표 함수다. 반복되는 프론트 동작을 묶어 재사용한다.
            saveStockOrderQuantity(input.closest(".stock-order-form"), input.value);
            updateStockTradeEstimates();
        });
    });
    document.querySelectorAll("[data-stock-exchange-form] input[name='coinAmount']").forEach((input) => {
    // 해설: 함수 또는 화살표 함수다. 반복되는 프론트 동작을 묶어 재사용한다.
        input.addEventListener("input", () => {
        // 해설: 함수 또는 화살표 함수다. 반복되는 프론트 동작을 묶어 재사용한다.
            saveStockExchangeQuantity(input.closest("[data-stock-exchange-form]"), input.value);
            updateStockExchangeEstimates();
        });
    });
    updateStockTradeEstimates();
    updateStockExchangeEstimates();
}

setupStockEstimateInputs();

document.addEventListener("click", (event) => {
// 해설: 함수 또는 화살표 함수다. 반복되는 프론트 동작을 묶어 재사용한다.
    const button = event.target.closest("[data-quantity-action]");
    if (!button) {
    // 해설: 조건 분기다. 현재 화면 상태나 응답 값에 따라 다른 동작을 수행한다.
        return;
    }
    const form = button.closest(".stock-order-form");
    const detail = button.closest(".stock-detail[data-stock-detail]");
    const input = form?.querySelector("input[name='quantity']");
    // 해설: DOM에서 특정 HTML 요소를 찾는다. 이후 텍스트, 클래스, 속성 등을 조작한다.
    if (!form || !detail || !input) {
    // 해설: 조건 분기다. 현재 화면 상태나 응답 값에 따라 다른 동작을 수행한다.
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
    // 해설: 조건 분기다. 현재 화면 상태나 응답 값에 따라 다른 동작을 수행한다.
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
// 해설: 함수 또는 화살표 함수다. 반복되는 프론트 동작을 묶어 재사용한다.
    const button = event.target.closest("[data-exchange-action]");
    if (!button) {
    // 해설: 조건 분기다. 현재 화면 상태나 응답 값에 따라 다른 동작을 수행한다.
        return;
    }
    const form = button.closest("[data-stock-exchange-form]");
    const input = form?.querySelector("input[name='coinAmount']");
    // 해설: DOM에서 특정 HTML 요소를 찾는다. 이후 텍스트, 클래스, 속성 등을 조작한다.
    if (!form || !input) {
    // 해설: 조건 분기다. 현재 화면 상태나 응답 값에 따라 다른 동작을 수행한다.
        return;
    }
    event.preventDefault();
    event.stopPropagation();
    const maxCoin = form.dataset.exchangeType === "cash-to-coin"
        ? Math.floor(playerCashBalance() / CASH_PER_COIN)
        : playerCoinBalance();
    let nextAmount = Number(input.value || 0);
    if (button.dataset.exchangeAction === "max") {
    // 해설: 조건 분기다. 현재 화면 상태나 응답 값에 따라 다른 동작을 수행한다.
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
// 해설: 함수 또는 화살표 함수다. 반복되는 프론트 동작을 묶어 재사용한다.
    const panel = document.querySelector("[data-stock-history-panel]");
    // 해설: DOM에서 특정 HTML 요소를 찾는다. 이후 텍스트, 클래스, 속성 등을 조작한다.
    if (!panel) {
    // 해설: 조건 분기다. 현재 화면 상태나 응답 값에 따라 다른 동작을 수행한다.
        return;
    }
    const typeButtons = panel.querySelectorAll("[data-trade-filter]");
    // 해설: DOM에서 특정 HTML 요소를 찾는다. 이후 텍스트, 클래스, 속성 등을 조작한다.
    const stockSelect = panel.querySelector("[data-trade-stock-filter]");
    // 해설: DOM에서 특정 HTML 요소를 찾는다. 이후 텍스트, 클래스, 속성 등을 조작한다.
    const rows = panel.querySelectorAll(".stock-trade-history li[data-trade-type]");
    // 해설: DOM에서 특정 HTML 요소를 찾는다. 이후 텍스트, 클래스, 속성 등을 조작한다.
    const emptyMessage = panel.querySelector(".stock-trade-empty-message");
    // 해설: DOM에서 특정 HTML 요소를 찾는다. 이후 텍스트, 클래스, 속성 등을 조작한다.
    let currentType = "all";

    function applyHistoryFilter() {
    // 해설: 함수 또는 화살표 함수다. 반복되는 프론트 동작을 묶어 재사용한다.
        const stockKey = stockSelect?.value || "all";
        let visibleCount = 0;
        rows.forEach((row) => {
        // 해설: 함수 또는 화살표 함수다. 반복되는 프론트 동작을 묶어 재사용한다.
            const typeMatches = currentType === "all" || row.dataset.tradeType === currentType;
            const stockMatches = stockKey === "all" || row.dataset.tradeStockKey === stockKey;
            const visible = typeMatches && stockMatches;
            row.hidden = !visible;
            if (visible) {
            // 해설: 조건 분기다. 현재 화면 상태나 응답 값에 따라 다른 동작을 수행한다.
                visibleCount += 1;
            }
        });
        if (emptyMessage) {
        // 해설: 조건 분기다. 현재 화면 상태나 응답 값에 따라 다른 동작을 수행한다.
            emptyMessage.hidden = visibleCount > 0 || rows.length === 0;
        }
    }

    typeButtons.forEach((button) => {
    // 해설: 함수 또는 화살표 함수다. 반복되는 프론트 동작을 묶어 재사용한다.
        button.addEventListener("click", () => {
        // 해설: 함수 또는 화살표 함수다. 반복되는 프론트 동작을 묶어 재사용한다.
            currentType = button.dataset.tradeFilter || "all";
            typeButtons.forEach((item) => item.classList.toggle("active", item === button));
            // 해설: 함수 또는 화살표 함수다. 반복되는 프론트 동작을 묶어 재사용한다.
            applyHistoryFilter();
        });
    });
    stockSelect?.addEventListener("change", applyHistoryFilter);
    // 해설: 브라우저 이벤트를 등록한다. 클릭, 입력, 로드 같은 사용자/화면 동작에 반응한다.
    applyHistoryFilter();
}

setupStockTradeHistoryFilters();

function setupStockExchangeForms() {
// 해설: 함수 또는 화살표 함수다. 반복되는 프론트 동작을 묶어 재사용한다.
    // 현금/코인 교환은 fetch로 처리해 페이지 전체를 새로고침하지 않고 상단 잔액과 미리보기만 갱신한다.
    document.querySelectorAll("[data-stock-exchange-form]").forEach((form) => {
    // 해설: 함수 또는 화살표 함수다. 반복되는 프론트 동작을 묶어 재사용한다.
        form.addEventListener("submit", async (event) => {
        // 해설: 함수 또는 화살표 함수다. 반복되는 프론트 동작을 묶어 재사용한다.
            event.preventDefault();
            const button = form.querySelector("button[type='submit']");
            // 해설: DOM에서 특정 HTML 요소를 찾는다. 이후 텍스트, 클래스, 속성 등을 조작한다.
            if (button) {
            // 해설: 조건 분기다. 현재 화면 상태나 응답 값에 따라 다른 동작을 수행한다.
                button.disabled = true;
            }
            try {
                const response = await fetch(form.action, {
                // 해설: 서버에 비동기 HTTP 요청을 보낸다. 페이지 전체 새로고침 없이 데이터를 주고받는다.
                    method: "POST",
                    body: new FormData(form)
                });
                const result = await response.json();
                if (result.redirect) {
                // 해설: 조건 분기다. 현재 화면 상태나 응답 값에 따라 다른 동작을 수행한다.
                    navigating = true;
                    window.location.href = result.redirect;
                    return;
                }
                if (cashValue && result.cash) {
                // 해설: 조건 분기다. 현재 화면 상태나 응답 값에 따라 다른 동작을 수행한다.
                    cashValue.textContent = result.cash;
                }
                if (coinValue && result.coin) {
                // 해설: 조건 분기다. 현재 화면 상태나 응답 값에 따라 다른 동작을 수행한다.
                    coinValue.textContent = result.coin;
                }
                if (result.cashRaw && stockPanelRoot()) {
                // 해설: 조건 분기다. 현재 화면 상태나 응답 값에 따라 다른 동작을 수행한다.
                    stockPanelRoot().dataset.playerCash = result.cashRaw;
                }
                if (result.coinRaw && stockPanelRoot()) {
                // 해설: 조건 분기다. 현재 화면 상태나 응답 값에 따라 다른 동작을 수행한다.
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
                // 해설: 조건 분기다. 현재 화면 상태나 응답 값에 따라 다른 동작을 수행한다.
                    button.disabled = false;
                }
            }
        });
    });
}

setupStockExchangeForms();

function showToast(message) {
// 해설: 함수 또는 화살표 함수다. 반복되는 프론트 동작을 묶어 재사용한다.
    if (!message || !toast) {
    // 해설: 조건 분기다. 현재 화면 상태나 응답 값에 따라 다른 동작을 수행한다.
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
    // 해설: HTML 요소의 CSS 클래스를 추가/삭제/토글해 화면 상태를 바꾼다.
    window.setTimeout(() => {
    // 해설: 함수 또는 화살표 함수다. 반복되는 프론트 동작을 묶어 재사용한다.
        item.classList.remove("show");
        // 해설: HTML 요소의 CSS 클래스를 추가/삭제/토글해 화면 상태를 바꾼다.
        window.setTimeout(() => {
        // 해설: 함수 또는 화살표 함수다. 반복되는 프론트 동작을 묶어 재사용한다.
            item.remove();
            if (toast.children.length === 0) {
            // 해설: 조건 분기다. 현재 화면 상태나 응답 값에 따라 다른 동작을 수행한다.
                toast.classList.remove("show");
                // 해설: HTML 요소의 CSS 클래스를 추가/삭제/토글해 화면 상태를 바꾼다.
            }
        }, 300);
    }, 8000);
}

if (flashToast) {
// 해설: 조건 분기다. 현재 화면 상태나 응답 값에 따라 다른 동작을 수행한다.
    window.setTimeout(() => flashToast.classList.remove("show"), 8000);
    // 해설: 함수 또는 화살표 함수다. 반복되는 프론트 동작을 묶어 재사용한다.
}

function shouldKeepGamePaused() {
// 해설: 함수 또는 화살표 함수다. 반복되는 프론트 동작을 묶어 재사용한다.
    return document.body.dataset.playerPaused === "true"
        || !!document.querySelector(".event-modal-backdrop:not([hidden])")
        // 해설: DOM에서 특정 HTML 요소를 찾는다. 이후 텍스트, 클래스, 속성 등을 조작한다.
        || !!document.querySelector("#secretaryModal:not([hidden])")
        // 해설: DOM에서 특정 HTML 요소를 찾는다. 이후 텍스트, 클래스, 속성 등을 조작한다.
        || !!document.querySelector("#confirmModal:not([hidden])")
        // 해설: DOM에서 특정 HTML 요소를 찾는다. 이후 텍스트, 클래스, 속성 등을 조작한다.
        || !!document.querySelector(".ability-modal-backdrop:not([hidden])")
        // 해설: DOM에서 특정 HTML 요소를 찾는다. 이후 텍스트, 클래스, 속성 등을 조작한다.
        || !!document.querySelector(".gift-select-popover:not([hidden])");
        // 해설: DOM에서 특정 HTML 요소를 찾는다. 이후 텍스트, 클래스, 속성 등을 조작한다.
}

function syncGamePauseState() {
// 해설: 함수 또는 화살표 함수다. 반복되는 프론트 동작을 묶어 재사용한다.
    if (shouldKeepGamePaused()) {
    // 해설: 조건 분기다. 현재 화면 상태나 응답 값에 따라 다른 동작을 수행한다.
        document.body.classList.add("game-paused");
        // 해설: HTML 요소의 CSS 클래스를 추가/삭제/토글해 화면 상태를 바꾼다.
    } else {
        document.body.classList.remove("game-paused");
        // 해설: HTML 요소의 CSS 클래스를 추가/삭제/토글해 화면 상태를 바꾼다.
    }
}

setupUiInteractions({ syncGamePauseState });

if (auctionTimer) {
// 해설: 조건 분기다. 현재 화면 상태나 응답 값에 따라 다른 동작을 수행한다.
    const timerValue = auctionTimer.querySelector("strong");
    // 해설: DOM에서 특정 HTML 요소를 찾는다. 이후 텍스트, 클래스, 속성 등을 조작한다.
    const cancelUrl = auctionTimer.dataset.auctionCancelUrl;
    let remainingSeconds = Number(auctionTimer.dataset.auctionSeconds) || 0;
    let auctionClosing = false;
    const closeAuction = async () => {
    // 해설: 함수 또는 화살표 함수다. 반복되는 프론트 동작을 묶어 재사용한다.
        if (auctionClosing || !cancelUrl) {
        // 해설: 조건 분기다. 현재 화면 상태나 응답 값에 따라 다른 동작을 수행한다.
            return;
        }
        auctionClosing = true;
        try {
            await fetch(cancelUrl, { method: "POST" });
            // 해설: 서버에 비동기 HTTP 요청을 보낸다. 페이지 전체 새로고침 없이 데이터를 주고받는다.
        } finally {
            saveScrollPosition();
            window.location.reload();
        }
    };
    if (remainingSeconds <= 0) {
    // 해설: 조건 분기다. 현재 화면 상태나 응답 값에 따라 다른 동작을 수행한다.
        closeAuction();
    } else {
        const auctionCountdown = window.setInterval(() => {
        // 해설: 함수 또는 화살표 함수다. 반복되는 프론트 동작을 묶어 재사용한다.
            remainingSeconds -= 1;
            if (timerValue) {
            // 해설: 조건 분기다. 현재 화면 상태나 응답 값에 따라 다른 동작을 수행한다.
                timerValue.textContent = String(Math.max(0, remainingSeconds));
            }
            if (remainingSeconds <= 0) {
            // 해설: 조건 분기다. 현재 화면 상태나 응답 값에 따라 다른 동작을 수행한다.
                window.clearInterval(auctionCountdown);
                closeAuction();
            }
        }, 1000);
    }
}

if (sideJobButton) {
// 해설: 조건 분기다. 현재 화면 상태나 응답 값에 따라 다른 동작을 수행한다.
    sideJobButton.addEventListener("click", async () => {
    // 해설: 함수 또는 화살표 함수다. 반복되는 프론트 동작을 묶어 재사용한다.
        sideJobButton.disabled = true;
        try {
            const response = await fetch("/side-job/quick", { method: "POST" });
            // 해설: 서버에 비동기 HTTP 요청을 보낸다. 페이지 전체 새로고침 없이 데이터를 주고받는다.
            const result = await response.json();
            if (result.redirect) {
            // 해설: 조건 분기다. 현재 화면 상태나 응답 값에 따라 다른 동작을 수행한다.
                window.location.href = result.redirect;
                return;
            }
            if (cashValue) {
            // 해설: 조건 분기다. 현재 화면 상태나 응답 값에 따라 다른 동작을 수행한다.
                cashValue.textContent = result.cash;
            }
            if (totalMonthlyRentValue) {
            // 해설: 조건 분기다. 현재 화면 상태나 응답 값에 따라 다른 동작을 수행한다.
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
    /*
     * 공통 시간 진행 함수다.
     *
     * 도시 화면과 주식 화면이 같은 함수를 사용해야 날짜가 한 번만 흐른다.
     * 주식 화면이면 view=stocks를 보내 서버가 도시 이벤트 표시만 지연시키고,
     * 날짜/정산/주가 갱신은 그대로 진행한다.
     */
    if (navigating || ticking || document.body.classList.contains("game-paused")) {
    // 해설: HTML 요소의 CSS 클래스를 추가/삭제/토글해 화면 상태를 바꾼다.
        return;
    }
    ticking = true;
    try {
        const view = document.querySelector(".stock-panel") ? "stocks" : "city";
        // 해설: DOM에서 특정 HTML 요소를 찾는다. 이후 텍스트, 클래스, 속성 등을 조작한다.
        const response = await fetch(`/tick?view=${view}`, { method: "POST" });
        // 해설: 서버에 비동기 HTTP 요청을 보낸다. 페이지 전체 새로고침 없이 데이터를 주고받는다.
        const result = await response.json();
        if (result.redirect) {
        // 해설: 조건 분기다. 현재 화면 상태나 응답 값에 따라 다른 동작을 수행한다.
            navigating = true;
            window.location.href = result.redirect;
            return;
        }
        if (result.event || result.auction) {
        // 해설: 조건 분기다. 현재 화면 상태나 응답 값에 따라 다른 동작을 수행한다.
            saveScrollPosition();
            navigating = true;
            window.location.reload();
            return;
        }
        if (result.notice) {
        // 해설: 조건 분기다. 현재 화면 상태나 응답 값에 따라 다른 동작을 수행한다.
            showToast(result.notice);
            window.setTimeout(() => {
            // 해설: 함수 또는 화살표 함수다. 반복되는 프론트 동작을 묶어 재사용한다.
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
// 해설: 함수 또는 화살표 함수다. 반복되는 프론트 동작을 묶어 재사용한다.
    // 100ms마다 진행률 막대를 갱신하고, 5초가 지나면 advanceDay()를 호출한다.
    if (navigating) {
    // 해설: 조건 분기다. 현재 화면 상태나 응답 값에 따라 다른 동작을 수행한다.
        return;
    }
    if (!dayProgress || !dayProgressText) {
    // 해설: 조건 분기다. 현재 화면 상태나 응답 값에 따라 다른 동작을 수행한다.
        return;
    }
    if (document.body.classList.contains("game-paused")) {
    // 해설: HTML 요소의 CSS 클래스를 추가/삭제/토글해 화면 상태를 바꾼다.
        dayProgressText.textContent = "일시정지";
        return;
    }
    const elapsed = Date.now() - tickStartedAt;
    const percent = Math.min(100, Math.floor((elapsed / TICK_DURATION_MS) * 100));
    dayProgress.style.width = `${percent}%`;
    dayProgressText.textContent = `다음 날 ${percent}%`;
    if (elapsed >= TICK_DURATION_MS) {
    // 해설: 조건 분기다. 현재 화면 상태나 응답 값에 따라 다른 동작을 수행한다.
        advanceDay();
    }
}

if (document.querySelector(".city-panel") || document.querySelector(".stock-panel")) {
// 해설: DOM에서 특정 HTML 요소를 찾는다. 이후 텍스트, 클래스, 속성 등을 조작한다.
    window.setInterval(updateDayProgress, 100);
    // 해설: 일정 시간 뒤 또는 일정 간격마다 함수를 실행한다. 자동 진행이나 지연 처리에 쓰인다.
}
```
