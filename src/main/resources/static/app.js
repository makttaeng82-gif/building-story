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
const buildingDetails = document.querySelectorAll(".building-detail[data-building-detail-id]");
const toast = document.querySelector("#toast");
const flashToast = document.querySelector("#flashToast");
const cashValue = document.querySelector("#cashValue");
const securitiesCashValue = document.querySelector("#securitiesCashValue");
const totalMonthlyRentValue = document.querySelector("#totalMonthlyRentValue");
const dayProgress = document.querySelector("#dayProgress");
const dayProgressText = document.querySelector("#dayProgressText");
const auctionTimer = document.querySelector(".auction-timer[data-auction-seconds]");
const TICK_DURATION_MS = 5000;
const STOCK_FEE_RATE = 0.0025;
const SCROLL_RESTORE_KEY = "buildingStory.scrollY";
const SELECTED_BUILDING_KEY = "buildingStory.selectedBuildingId";
const SELECTED_STOCK_KEY = "buildingStory.selectedStockKey";
const STOCK_LIST_SCROLL_KEY = "buildingStory.stockListScrollTop";
const STOCK_ORDER_PERCENT_KEY = "buildingStory.stockOrderPercents";
const STOCK_EXCHANGE_QUANTITY_KEY = "buildingStory.stockExchangeQuantities";
const STOCK_CHART_PERIOD_KEY = "buildingStory.stockChartPeriod";
const STOCK_CHART_TOGGLES_KEY = "buildingStory.stockChartToggles";
const STOCK_CHART_LATEST_CANDLE_KEY = "buildingStory.stockChartLatestCandle";
const WATCHED_STOCKS_KEY = "buildingStory.watchedStocks";
const STOCK_THEME_KEY = "buildingStory.stockTheme";
const COMPANY_THEME_KEY = "buildingStory.companyTheme";
const STOCK_SUMMARY_MORE_KEY = "buildingStory.stockSummaryMoreOpen";
const RECORD_PANEL_DOCKED_KEY = "buildingStory.recordPanelDocked";
const COLLAPSIBLE_PANEL_STATE_KEY = "buildingStory.collapsiblePanels";
const GAME_TICK_PROGRESS_KEY = "buildingStory.gameTickProgress";
const PENDING_TICK_TOAST_KEY = "buildingStory.pendingTickToast";

function restoredTickProgress() {
    try {
        const saved = JSON.parse(window.sessionStorage.getItem(GAME_TICK_PROGRESS_KEY) || "null");
        const elapsedDays = Number(document.body.dataset.elapsedDays);
        if (saved?.elapsedDays === elapsedDays) {
            return Math.max(0, Math.min(TICK_DURATION_MS, Number(saved.progressMs) || 0));
        }
    } catch {
        // 손상된 브라우저 임시값은 버리고 현재 날짜를 0%부터 시작한다.
    }
    return 0;
}

let accumulatedTickMs = restoredTickProgress();
let tickSegmentStartedAt = Date.now();
let tickWasPaused = document.body.classList.contains("game-paused");
let lastTickProgressSaveAt = 0;
// ticking은 /tick 중복 호출을 막는 플래그다. 이 값이 없으면 느린 네트워크에서 하루가 2번 지날 수 있다.
let ticking = false;
// navigating은 reload/redirect가 예정된 상태다. 화면 전환 중 새 tick이나 UI 갱신이 끼어드는 것을 막는다.
let navigating = false;

function setupStockTheme() {
    const buttons = document.querySelectorAll("[data-stock-theme-option]");
    if (buttons.length === 0) {
        return;
    }

    function applyTheme(theme, persist) {
        const selectedTheme = theme === "light" ? "light" : "dark";
        document.documentElement.dataset.stockTheme = selectedTheme;
        buttons.forEach((button) => {
            const selected = button.dataset.stockThemeOption === selectedTheme;
            button.classList.toggle("active", selected);
            button.setAttribute("aria-pressed", String(selected));
        });
        if (persist) {
            window.localStorage.setItem(STOCK_THEME_KEY, selectedTheme);
        }
    }

    applyTheme(document.documentElement.dataset.stockTheme, false);
    buttons.forEach((button) => {
        button.addEventListener("click", () => applyTheme(button.dataset.stockThemeOption, true));
    });
}

setupStockTheme();

function setupCompanyTheme() {
    const buttons = document.querySelectorAll("[data-company-theme-option]");
    if (buttons.length === 0) {
        return;
    }

    function applyTheme(theme, persist) {
        const selectedTheme = theme === "light" ? "light" : "dark";
        document.documentElement.dataset.companyTheme = selectedTheme;
        buttons.forEach((button) => {
            const selected = button.dataset.companyThemeOption === selectedTheme;
            button.classList.toggle("active", selected);
            button.setAttribute("aria-pressed", String(selected));
        });
        if (persist) {
            window.localStorage.setItem(COMPANY_THEME_KEY, selectedTheme);
        }
    }

    applyTheme(document.documentElement.dataset.companyTheme, false);
    buttons.forEach((button) => {
        button.addEventListener("click", () => applyTheme(button.dataset.companyThemeOption, true));
    });
}

setupCompanyTheme();

function setupStockSummaryMore() {
    const details = document.querySelector(".stock-summary-more");
    if (!details) {
        return;
    }
    details.open = window.sessionStorage.getItem(STOCK_SUMMARY_MORE_KEY) === "true";
    details.addEventListener("toggle", () => {
        window.sessionStorage.setItem(STOCK_SUMMARY_MORE_KEY, String(details.open));
    });
}

setupStockSummaryMore();

const pauseForm = document.querySelector(".tab-pause-form");
if (pauseForm) {
    pauseForm.addEventListener("submit", () => {
        // 서버가 일시정지 상태를 저장하고 화면을 다시 그리는 동안 자동 날짜 진행 요청이 끼어들지 못하게 한다.
        navigating = true;
        document.body.classList.add("game-paused");
        const submitButton = pauseForm.querySelector("button[type='submit']");
        if (submitButton) {
            submitButton.disabled = true;
        }
    });
}

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
    /*
     * 종목 목록은 모든 종목의 경량 시세만 가진다. 종목을 고르면 stockKey를 URL에 넣어
     * 서버가 선택 종목 하나의 차트와 주문 폼만 다시 렌더링한다.
     */
    const panel = document.querySelector(".stock-panel[data-selected-stock-key]");
    const buttons = document.querySelectorAll(".stock-company-button[data-stock-key]");
    const rows = document.querySelectorAll(".stock-company-row[data-stock-row]");
    const holdingButtons = document.querySelectorAll("[data-stock-holding-key]");
    const list = document.querySelector(".stock-company-list");
    const filterButtons = document.querySelectorAll("[data-stock-filter]");
    const searchInput = document.querySelector("[data-stock-search]");
    const watchButtons = document.querySelectorAll("[data-stock-watch]");
    const emptyMessage = document.querySelector(".stock-empty-message");
    if (!panel || buttons.length === 0) {
        return;
    }

    let currentFilter = window.sessionStorage.getItem("buildingStory.stockFilter") || "all";
    let watchedStocks = new Set();
    try {
        watchedStocks = new Set(JSON.parse(window.localStorage.getItem(WATCHED_STOCKS_KEY) || "[]"));
    } catch {
        watchedStocks = new Set();
    }

    function selectStock(key) {
        const target = Array.from(buttons).find((button) => button.dataset.stockKey === key);
        if (!target) {
            return;
        }
        const selectedKey = target.dataset.stockKey;
        window.localStorage.setItem(SELECTED_STOCK_KEY, selectedKey);
        buttons.forEach((item) => item.classList.toggle("selected", item.dataset.stockKey === selectedKey));
        if (selectedKey !== panel.dataset.selectedStockKey) {
            saveScrollPosition();
            const url = new URL(window.location.href);
            url.searchParams.set("view", "stocks");
            url.searchParams.set("stockKey", selectedKey);
            window.location.assign(url);
        }
    }

    function applyFilter(filter) {
        currentFilter = ["owned", "watched"].includes(filter) ? filter : "all";
        window.sessionStorage.setItem("buildingStory.stockFilter", currentFilter);
        filterButtons.forEach((button) => {
            button.classList.toggle("active", button.dataset.stockFilter === currentFilter);
        });
        const keyword = (searchInput?.value || "").trim().toLocaleLowerCase("ko-KR");
        rows.forEach((row) => {
            const owned = Number(row.dataset.ownedQuantity || 0) > 0;
            const watched = watchedStocks.has(row.dataset.stockRow);
            const filterMatches = currentFilter === "all" || (currentFilter === "owned" && owned) || (currentFilter === "watched" && watched);
            const searchMatches = !keyword || (row.dataset.stockName || "").toLocaleLowerCase("ko-KR").includes(keyword);
            row.hidden = !(filterMatches && searchMatches);
        });
        const hasVisibleStock = Array.from(rows).some((row) => !row.hidden);
        if (emptyMessage) {
            emptyMessage.hidden = hasVisibleStock;
        }
    }

    buttons.forEach((button) => {
        button.addEventListener("click", () => selectStock(button.dataset.stockKey));
    });
    holdingButtons.forEach((button) => {
        button.addEventListener("click", () => {
            applyFilter("all");
            selectStock(button.dataset.stockHoldingKey);
        });
    });
    filterButtons.forEach((button) => {
        button.addEventListener("click", () => applyFilter(button.dataset.stockFilter));
    });
    searchInput?.addEventListener("input", () => applyFilter(currentFilter));
    watchButtons.forEach((button) => {
        const key = button.dataset.stockWatchKey;
        const applyWatchState = () => {
            const watched = watchedStocks.has(key);
            button.classList.toggle("active", watched);
            button.textContent = watched ? "★" : "☆";
            button.setAttribute("aria-label", `${button.closest("[data-stock-row]")?.dataset.stockName || "종목"} 관심종목 ${watched ? "해제" : "추가"}`);
        };
        applyWatchState();
        button.addEventListener("click", () => {
            if (watchedStocks.has(key)) {
                watchedStocks.delete(key);
            } else {
                watchedStocks.add(key);
            }
            window.localStorage.setItem(WATCHED_STOCKS_KEY, JSON.stringify(Array.from(watchedStocks)));
            applyWatchState();
            applyFilter(currentFilter);
        });
    });
    window.localStorage.setItem(SELECTED_STOCK_KEY, panel.dataset.selectedStockKey);
    applyFilter(currentFilter);
    if (list) {
        list.scrollTop = Number(window.sessionStorage.getItem(STOCK_LIST_SCROLL_KEY)) || 0;
        list.addEventListener("scroll", () => {
            window.sessionStorage.setItem(STOCK_LIST_SCROLL_KEY, String(list.scrollTop));
        }, { passive: true });
    }
}

setupStockPanel();

function setupStockChart() {
    const shell = document.querySelector("[data-stock-chart]");
    const chart = shell?.querySelector(".stock-candle-chart");
    const detail = shell?.closest(".stock-detail");
    const candleElements = chart ? Array.from(chart.querySelectorAll(".chart-candle")) : [];
    if (!shell || !chart || !detail || candleElements.length === 0) {
        return;
    }

    const points = candleElements.map((element) => ({
        element,
        elapsedDays: Number(element.dataset.elapsedDays),
        date: element.dataset.date || "",
        open: Number(element.dataset.open),
        high: Number(element.dataset.high),
        low: Number(element.dataset.low),
        close: Number(element.dataset.close),
        openText: element.dataset.openText || "0원",
        highText: element.dataset.highText || "0원",
        lowText: element.dataset.lowText || "0원",
        closeText: element.dataset.closeText || "0원",
        changeText: element.dataset.changeText || "0%",
        factorText: element.dataset.factorText || "",
        index: Number(element.dataset.index),
        newsId: element.dataset.newsId || "",
        newsTitle: element.dataset.newsTitle || "",
        newsCount: Number(element.dataset.newsCount || 0),
        earnings: element.dataset.earnings === "true",
        dividend: element.dataset.dividend === "true",
        x: 0,
        closeY: 0,
        highY: 0
    }));
    const periodButtons = detail.querySelectorAll("[data-chart-period]");
    const toggleButtons = detail.querySelectorAll("[data-chart-toggle]");
    const rangeLabel = detail.querySelector("[data-chart-range-label]");
    const inspector = shell.querySelector("[data-chart-inspector]");
    const eventLayer = chart.querySelector(".chart-event-layer");
    const fairBand = chart.querySelector(".chart-fair-value-band");
    const indexLine = chart.querySelector(".chart-index-line");
    const shortAverageLine = chart.querySelector(".chart-average-short");
    const longAverageLine = chart.querySelector(".chart-average-long");
    const gridLayer = chart.querySelector(".chart-grid-layer");
    const axisLayer = chart.querySelector(".chart-axis-layer");
    const dateAxisLayer = chart.querySelector(".chart-date-axis-layer");
    const currentPriceLine = chart.querySelector(".current-price-line");
    const currentPriceLabel = chart.querySelector(".chart-price-label");
    const highLine = chart.querySelector(".chart-high-line");
    const lowLine = chart.querySelector(".chart-low-line");
    const highAxisLabel = chart.querySelector(".chart-high-axis-label");
    const lowAxisLabel = chart.querySelector(".chart-low-axis-label");
    const fairUpperIndicator = chart.querySelector(".chart-fair-upper");
    const fairLowerIndicator = chart.querySelector(".chart-fair-lower");
    const crosshairX = chart.querySelector(".chart-crosshair-x");
    const crosshairY = chart.querySelector(".chart-crosshair-y");
    const crosshairPriceBackground = chart.querySelector(".chart-crosshair-price-bg");
    const crosshairPrice = chart.querySelector(".chart-crosshair-price");
    const crosshairDate = chart.querySelector(".chart-crosshair-date");
    const legend = shell.querySelector("[data-chart-legend]");
    const fairLegend = legend?.querySelector("[data-chart-legend-fair]");
    const indexLegend = legend?.querySelector("[data-chart-legend-index]");
    const shortLegend = legend?.querySelector("[data-chart-legend-short]");
    const longLegend = legend?.querySelector("[data-chart-legend-long]");
    const fairLow = Number(shell.dataset.fairLow || 0);
    const fairHigh = Number(shell.dataset.fairHigh || 0);
    const validPeriods = new Set(["18", "73", "219", "all"]);
    const validToggles = new Set(["events", "fair", "index", "average"]);
    let period = "73";
    const enabled = new Set(["events"]);
    let visiblePoints = [];
    let currentScale = { min: 0, max: 1 };

    try {
        const savedPeriod = window.localStorage.getItem(STOCK_CHART_PERIOD_KEY);
        if (validPeriods.has(savedPeriod)) {
            period = savedPeriod;
        }
        const savedToggles = JSON.parse(window.localStorage.getItem(STOCK_CHART_TOGGLES_KEY) || "null");
        if (Array.isArray(savedToggles)) {
            enabled.clear();
            savedToggles.filter((key) => validToggles.has(key)).forEach((key) => enabled.add(key));
        }
    } catch {
        // 저장소를 사용할 수 없는 환경에서는 기본 차트 설정을 사용한다.
    }

    periodButtons.forEach((button) => button.classList.toggle("active", button.dataset.chartPeriod === period));
    toggleButtons.forEach((button) => {
        const active = enabled.has(button.dataset.chartToggle);
        button.classList.toggle("active", active);
        button.setAttribute("aria-pressed", String(active));
    });

    function setSvgHidden(element, hidden) {
        element?.toggleAttribute("hidden", hidden);
    }

    function formatChartPrice(value) {
        const amount = Math.max(0, Math.round(value));
        const jo = Math.floor(amount / 1_000_000_000_000);
        const eok = Math.floor((amount % 1_000_000_000_000) / 100_000_000);
        const man = Math.floor((amount % 100_000_000) / 10_000);
        const won = amount % 10_000;
        if (jo > 0) return `${jo}조${eok > 0 ? ` ${eok}억` : ""}원`;
        if (eok > 0) return `${eok}억${man > 0 ? ` ${man}만` : ""}원`;
        if (man > 0) return `${man}만${won > 0 ? won : ""}원`;
        return `${won}원`;
    }

    function formatPercent(value) {
        const rounded = Math.round(value * 100) / 100;
        return `${rounded > 0 ? "+" : ""}${rounded.toFixed(2)}%`;
    }

    const chartY = (value, min, max) => 24 + (max - value) * 220 / Math.max(1, max - min);
    const clippedChartY = (value, min, max) => Math.max(24, Math.min(244, chartY(value, min, max)));

    function aggregateMonthly(source) {
        const grouped = [];
        let previousMonth = null;
        let yearOffset = 0;
        source.forEach((point) => {
            const month = Number(point.date.split("/")[0]);
            if (previousMonth !== null && month < previousMonth) yearOffset += 1;
            previousMonth = month;
            const key = `${yearOffset}-${month}`;
            let group = grouped[grouped.length - 1];
            if (!group || group.key !== key) {
                group = { key, points: [] };
                grouped.push(group);
            }
            group.points.push(point);
        });
        return grouped.map((group) => {
            const first = group.points[0];
            const last = group.points[group.points.length - 1];
            const high = Math.max(...group.points.map((point) => point.high));
            const low = Math.min(...group.points.map((point) => point.low));
            const headline = [...group.points].reverse().find((point) => point.newsId);
            return {
                ...last,
                open: first.open,
                high,
                low,
                openText: formatChartPrice(first.open),
                highText: formatChartPrice(high),
                lowText: formatChartPrice(low),
                closeText: formatChartPrice(last.close),
                changeText: formatPercent(first.open === 0 ? 0 : (last.close - first.open) * 100 / first.open),
                newsId: headline?.newsId || "",
                newsTitle: headline?.newsTitle || "",
                newsCount: group.points.reduce((sum, point) => sum + point.newsCount, 0),
                earnings: group.points.some((point) => point.earnings),
                dividend: group.points.some((point) => point.dividend)
            };
        });
    }

    function selectedSeries() {
        if (period === "18" || period === "73") {
            const count = Number(period);
            return {
                calculationPoints: points,
                points: points.slice(Math.max(0, points.length - count)),
                interval: "5일봉"
            };
        }
        const monthlyPoints = aggregateMonthly(points);
        return {
            calculationPoints: monthlyPoints,
            points: period === "all" ? monthlyPoints : monthlyPoints.slice(Math.max(0, monthlyPoints.length - 36)),
            interval: "월봉"
        };
    }

    function priceScale(source) {
        const rawMin = Math.min(...source.map((point) => point.low));
        const rawMax = Math.max(...source.map((point) => point.high));
        const latest = source[source.length - 1];
        const paddingRatio = period === "18" ? 0.10 : period === "73" ? 0.08 : 0.05;
        const minimumSpanRatio = period === "18" ? 0.025 : period === "73" ? 0.06 : 0.12;
        const rawSpan = Math.max(1, rawMax - rawMin);
        const targetSpan = Math.max(rawSpan * (1 + paddingRatio * 2), latest.close * minimumSpanRatio, 1);
        const midpoint = (rawMin + rawMax) / 2;
        const preliminaryMin = Math.max(0, midpoint - targetSpan / 2);
        const preliminaryMax = midpoint + targetSpan / 2;
        const step = nicePriceStep((preliminaryMax - preliminaryMin) / 4);
        let max = Math.ceil(preliminaryMax / step) * step;
        let min = max - step * 4;
        if (min > preliminaryMin) {
            min = Math.floor(preliminaryMin / step) * step;
            max = min + step * 4;
        }
        if (min < 0) {
            min = 0;
            max = step * 4;
        }
        return { min, max, step };
    }

    function nicePriceStep(rawStep) {
        const safeStep = Math.max(1, rawStep);
        const magnitude = 10 ** Math.floor(Math.log10(safeStep));
        const fraction = safeStep / magnitude;
        const niceFraction = fraction <= 1 ? 1
                : fraction <= 2 ? 2
                : fraction <= 2.5 ? 2.5
                : fraction <= 5 ? 5 : 10;
        return niceFraction * magnitude;
    }

    function averageValues(source, windowSize) {
        let sum = 0;
        source.forEach((point, index) => {
            sum += point.close;
            if (index >= windowSize) sum -= source[index - windowSize].close;
            point[`average${windowSize}`] = index >= windowSize - 1 ? sum / windowSize : null;
        });
    }

    function averagePoints(source, windowSize, min, max) {
        return source.filter((point) => point[`average${windowSize}`] !== null)
                .map((point) => `${point.x},${clippedChartY(point[`average${windowSize}`], min, max).toFixed(1)}`)
                .join(" ");
    }

    function updateLegend(point) {
        if (!legend || !point) return;
        const baseIndex = Math.max(1, visiblePoints[0].index);
        const monthly = period === "219" || period === "all";
        const showShort = enabled.has("average") && point.average5 !== null;
        const showLong = enabled.has("average") && point.average20 !== null;
        fairLegend.toggleAttribute("hidden", !enabled.has("fair"));
        indexLegend.toggleAttribute("hidden", !enabled.has("index"));
        shortLegend.toggleAttribute("hidden", !showShort);
        longLegend.toggleAttribute("hidden", !showLong);
        fairLegend.textContent = `적정가 ${formatChartPrice(fairLow)} - ${formatChartPrice(fairHigh)}`;
        indexLegend.textContent = `시장 ${formatPercent((point.index - baseIndex) * 100 / baseIndex)}`;
        shortLegend.textContent = `MA5${monthly ? "월" : ""} ${formatChartPrice(point.average5 || 0)}`;
        longLegend.textContent = `MA20${monthly ? "월" : ""} ${formatChartPrice(point.average20 || 0)}`;
        legend.toggleAttribute("hidden", !enabled.has("fair") && !enabled.has("index") && !showShort && !showLong);
    }

    function updateInspector(point) {
        if (!inspector || !point) return;
        inspector.querySelector("[data-chart-date]").textContent = point.date;
        const change = inspector.querySelector("[data-chart-change]");
        change.textContent = point.changeText;
        change.className = point.close > point.open ? "up" : point.close < point.open ? "down" : "flat";
        inspector.querySelector("[data-chart-open]").textContent = point.openText;
        inspector.querySelector("[data-chart-high]").textContent = point.highText;
        inspector.querySelector("[data-chart-low]").textContent = point.lowText;
        inspector.querySelector("[data-chart-close]").textContent = point.closeText;
        inspector.querySelector("[data-chart-factors]").textContent = point.factorText;
        const newsButton = inspector.querySelector("[data-chart-news-open]");
        newsButton.hidden = !point.newsId;
        newsButton.dataset.newsId = point.newsId;
        newsButton.textContent = point.newsCount > 1 ? `연결 뉴스 ${point.newsCount}건 보기` : "연결 뉴스 보기";
        newsButton.title = point.newsTitle;
        updateLegend(point);
    }

    function openLinkedNews(newsId) {
        if (!newsId) return;
        const newsItem = document.querySelector(`[data-stock-news-open="${CSS.escape(newsId)}"]`);
        newsItem?.click();
    }

    function drawEvents() {
        eventLayer.replaceChildren();
        if (!enabled.has("events")) return;
        const namespace = "http://www.w3.org/2000/svg";
        visiblePoints.forEach((point) => {
            const eventCodes = [];
            if (point.newsCount > 0) eventCodes.push("N");
            if (point.earnings) eventCodes.push("E");
            if (point.dividend) eventCodes.push("D");
            eventCodes.forEach((code, index) => {
                const marker = document.createElementNS(namespace, "g");
                marker.classList.add("chart-event-marker");
                marker.setAttribute("transform", `translate(${point.x + (index - (eventCodes.length - 1) / 2) * 14},${Math.max(16, point.highY - 13)})`);
                const circle = document.createElementNS(namespace, "circle");
                circle.setAttribute("r", "6");
                const text = document.createElementNS(namespace, "text");
                text.setAttribute("y", "3");
                text.textContent = code;
                const title = document.createElementNS(namespace, "title");
                title.textContent = code === "N" ? point.newsTitle || "뉴스"
                        : code === "E" ? "분기 실적 발표" : "배당 지급";
                marker.append(title, circle, text);
                marker.addEventListener("mouseenter", () => updateInspector(point));
                marker.addEventListener("click", (event) => {
                    event.stopPropagation();
                    updateInspector(point);
                    if (code === "N") openLinkedNews(point.newsId);
                });
                eventLayer.append(marker);
            });
        });
    }

    function drawAxes(min, max, occupiedLabelYs) {
        const namespace = "http://www.w3.org/2000/svg";
        gridLayer.replaceChildren();
        axisLayer.replaceChildren();
        for (let index = 0; index < 5; index += 1) {
            const ratio = index / 4;
            const y = 24 + ratio * 220;
            const value = max - (max - min) * ratio;
            const line = document.createElementNS(namespace, "line");
            line.classList.add("chart-grid-line");
            line.setAttribute("x1", "20");
            line.setAttribute("x2", "700");
            line.setAttribute("y1", y.toFixed(1));
            line.setAttribute("y2", y.toFixed(1));
            gridLayer.append(line);
            if (!occupiedLabelYs.some((occupiedY) => Math.abs(occupiedY - y) < 13)) {
                const label = document.createElementNS(namespace, "text");
                label.classList.add("chart-axis-label");
                label.setAttribute("x", "710");
                label.setAttribute("y", (y + 4).toFixed(1));
                label.textContent = formatChartPrice(value);
                axisLayer.append(label);
            }
        }
    }

    function drawDateAxis(source) {
        const namespace = "http://www.w3.org/2000/svg";
        const tickCount = period === "18" ? 4 : 5;
        const indices = new Set();
        for (let index = 0; index < tickCount; index += 1) {
            indices.add(Math.round(index * (source.length - 1) / Math.max(1, tickCount - 1)));
        }
        dateAxisLayer.replaceChildren();
        indices.forEach((index) => {
            const point = source[index];
            const label = document.createElementNS(namespace, "text");
            label.classList.add("chart-date-label");
            label.setAttribute("x", point.x.toFixed(1));
            label.setAttribute("y", "274");
            label.setAttribute("text-anchor", "middle");
            if (period === "219" || period === "all") {
                const daysAgo = source[source.length - 1].elapsedDays - point.elapsedDays;
                const month = point.date.split("/")[0];
                label.textContent = daysAgo >= 330 ? `${Math.round(daysAgo / 365)}년 전` : `${month}월`;
            } else {
                label.textContent = point.date;
            }
            dateAxisLayer.append(label);
        });
    }

    function drawExtremes(source, currentY) {
        const highest = source.reduce((result, point) => point.high > result.high ? point : result, source[0]);
        const lowest = source.reduce((result, point) => point.low < result.low ? point : result, source[0]);
        const highY = clippedChartY(highest.high, currentScale.min, currentScale.max);
        const lowY = clippedChartY(lowest.low, currentScale.min, currentScale.max);
        highLine.setAttribute("y1", highY.toFixed(1));
        highLine.setAttribute("y2", highY.toFixed(1));
        lowLine.setAttribute("y1", lowY.toFixed(1));
        lowLine.setAttribute("y2", lowY.toFixed(1));

        const occupied = [currentY];
        const place = (group, valueY, value, prefix, collisionDirection) => {
            let labelY = Math.max(24, Math.min(244, valueY));
            while (occupied.some((occupiedY) => Math.abs(labelY - occupiedY) < 17)) {
                labelY = Math.max(24, Math.min(244, labelY + collisionDirection * 18));
                if (labelY === 24 || labelY === 244) break;
            }
            const background = group.querySelector("rect");
            const text = group.querySelector("text");
            background.setAttribute("y", (labelY - 8).toFixed(1));
            text.setAttribute("y", (labelY + 4).toFixed(1));
            text.textContent = `${prefix} ${formatChartPrice(value)}`;
            occupied.push(labelY);
            return labelY;
        };
        const highLabelY = place(highAxisLabel, highY, highest.high, "고", 1);
        const lowLabelY = place(lowAxisLabel, lowY, lowest.low, "저", -1);
        return { highLabelY, lowLabelY };
    }

    function render() {
        const selection = selectedSeries();
        visiblePoints = selection.points;
        currentScale = priceScale(visiblePoints);
        const { min, max } = currentScale;
        averageValues(selection.calculationPoints, 5);
        averageValues(selection.calculationPoints, 20);
        const spacing = visiblePoints.length <= 1 ? 0 : 630 / (visiblePoints.length - 1);
        const candleWidth = Math.max(2, Math.min(8, spacing * 0.55));
        const visibleElements = new Set(visiblePoints.map((point) => point.element));
        points.forEach((point) => point.element.classList.toggle("chart-candle-hidden", !visibleElements.has(point.element)));
        visiblePoints.forEach((point, index) => {
            point.element.classList.toggle("candle-up", point.close >= point.open);
            point.element.classList.toggle("candle-down", point.close < point.open);
            point.x = 36 + spacing * index;
            point.highY = chartY(point.high, min, max);
            const lowY = chartY(point.low, min, max);
            const openY = chartY(point.open, min, max);
            point.closeY = chartY(point.close, min, max);
            const wick = point.element.querySelector(".candle-wick");
            const body = point.element.querySelector(".candle-body");
            wick.setAttribute("x1", point.x.toFixed(1));
            wick.setAttribute("x2", point.x.toFixed(1));
            wick.setAttribute("y1", point.highY.toFixed(1));
            wick.setAttribute("y2", lowY.toFixed(1));
            body.setAttribute("x", (point.x - candleWidth / 2).toFixed(1));
            body.setAttribute("width", candleWidth.toFixed(1));
            body.setAttribute("y", Math.min(openY, point.closeY).toFixed(1));
            body.setAttribute("height", Math.max(2, Math.abs(openY - point.closeY)).toFixed(1));
        });

        const latest = visiblePoints[visiblePoints.length - 1];
        const currentY = chartY(latest.close, min, max);
        currentPriceLine.setAttribute("y1", currentY.toFixed(1));
        currentPriceLine.setAttribute("y2", currentY.toFixed(1));
        currentPriceLabel.setAttribute("y", currentY.toFixed(1));
        currentPriceLabel.textContent = latest.closeText;
        rangeLabel.textContent = `최근 ${visiblePoints.length}개 · ${selection.interval}`;
        drawDateAxis(visiblePoints);
        const extremeLabels = drawExtremes(visiblePoints, currentY);
        drawAxes(min, max, [currentY, extremeLabels.highLabelY, extremeLabels.lowLabelY]);

        if (enabled.has("fair") && fairLow > 0 && fairHigh > 0) {
            const visibleFairLow = Math.max(min, fairLow);
            const visibleFairHigh = Math.min(max, fairHigh);
            const intersects = visibleFairLow <= visibleFairHigh;
            setSvgHidden(fairBand, !intersects);
            if (intersects) {
                const top = chartY(visibleFairHigh, min, max);
                const bottom = chartY(visibleFairLow, min, max);
                fairBand.setAttribute("y", top.toFixed(1));
                fairBand.setAttribute("height", Math.max(1, bottom - top).toFixed(1));
            }
            setSvgHidden(fairUpperIndicator, fairHigh <= max);
            setSvgHidden(fairLowerIndicator, fairLow >= min);
            fairUpperIndicator.textContent = `적정가 상단 ${formatChartPrice(fairHigh)} ↑`;
            fairLowerIndicator.textContent = `적정가 하단 ${formatChartPrice(fairLow)} ↓`;
        } else {
            setSvgHidden(fairBand, true);
            setSvgHidden(fairUpperIndicator, true);
            setSvgHidden(fairLowerIndicator, true);
        }

        if (enabled.has("index") && visiblePoints.length > 1) {
            const baseClose = visiblePoints[0].close;
            const baseIndex = Math.max(1, visiblePoints[0].index);
            setSvgHidden(indexLine, false);
            indexLine.setAttribute("points", visiblePoints.map((point) => {
                const comparablePrice = baseClose * point.index / baseIndex;
                return `${point.x.toFixed(1)},${clippedChartY(comparablePrice, min, max).toFixed(1)}`;
            }).join(" "));
        } else {
            setSvgHidden(indexLine, true);
        }

        const averagesVisible = enabled.has("average");
        setSvgHidden(shortAverageLine, !averagesVisible);
        setSvgHidden(longAverageLine, !averagesVisible);
        if (averagesVisible) {
            shortAverageLine.setAttribute("points", averagePoints(visiblePoints, 5, min, max));
            longAverageLine.setAttribute("points", averagePoints(visiblePoints, 20, min, max));
        }
        drawEvents();
        updateInspector(latest);
    }

    function saveChartSettings() {
        try {
            window.localStorage.setItem(STOCK_CHART_PERIOD_KEY, period);
            window.localStorage.setItem(STOCK_CHART_TOGGLES_KEY, JSON.stringify([...enabled]));
        } catch {
            // 저장 실패는 현재 화면의 차트 조작을 막지 않는다.
        }
    }

    function animateNewCandle() {
        const stockKey = detail.dataset.stockDetail;
        const latest = points[points.length - 1];
        if (!stockKey || !latest) return;
        try {
            const saved = JSON.parse(window.sessionStorage.getItem(STOCK_CHART_LATEST_CANDLE_KEY) || "{}");
            const previousDay = Number(saved[stockKey]);
            if (previousDay > 0 && latest.elapsedDays > previousDay) {
                latest.element.classList.add("chart-candle-new");
                window.setTimeout(() => latest.element.classList.remove("chart-candle-new"), 320);
            }
            saved[stockKey] = latest.elapsedDays;
            window.sessionStorage.setItem(STOCK_CHART_LATEST_CANDLE_KEY, JSON.stringify(saved));
        } catch {
            // 저장소를 사용할 수 없으면 등장 효과만 생략한다.
        }
    }

    periodButtons.forEach((button) => {
        button.addEventListener("click", () => {
            period = button.dataset.chartPeriod;
            periodButtons.forEach((item) => item.classList.toggle("active", item === button));
            saveChartSettings();
            render();
        });
    });
    toggleButtons.forEach((button) => {
        button.addEventListener("click", () => {
            const key = button.dataset.chartToggle;
            if (enabled.has(key)) enabled.delete(key); else enabled.add(key);
            const active = enabled.has(key);
            button.classList.toggle("active", active);
            button.setAttribute("aria-pressed", String(active));
            saveChartSettings();
            render();
        });
    });
    inspector?.querySelector("[data-chart-news-open]")?.addEventListener("click", (event) => openLinkedNews(event.currentTarget.dataset.newsId));
    chart.addEventListener("mousemove", (event) => {
        const bounds = chart.getBoundingClientRect();
        const chartX = (event.clientX - bounds.left) * 860 / Math.max(1, bounds.width);
        const pointerY = Math.max(24, Math.min(244, (event.clientY - bounds.top) * 300 / Math.max(1, bounds.height)));
        const nearest = visiblePoints.reduce((best, point) =>
            Math.abs(point.x - chartX) < Math.abs(best.x - chartX) ? point : best, visiblePoints[0]);
        updateInspector(nearest);
        setSvgHidden(crosshairX, false);
        setSvgHidden(crosshairY, false);
        setSvgHidden(crosshairPriceBackground, false);
        setSvgHidden(crosshairPrice, false);
        setSvgHidden(crosshairDate, false);
        crosshairX.setAttribute("y1", pointerY.toFixed(1));
        crosshairX.setAttribute("y2", pointerY.toFixed(1));
        crosshairY.setAttribute("x1", nearest.x.toFixed(1));
        crosshairY.setAttribute("x2", nearest.x.toFixed(1));
        const pointerPrice = currentScale.max - (pointerY - 24) * (currentScale.max - currentScale.min) / 220;
        crosshairPriceBackground.setAttribute("y", (pointerY - 8).toFixed(1));
        crosshairPrice.setAttribute("y", (pointerY + 4).toFixed(1));
        crosshairPrice.textContent = formatChartPrice(pointerPrice);
        crosshairDate.setAttribute("x", nearest.x.toFixed(1));
        crosshairDate.textContent = nearest.date;
    });
    chart.addEventListener("mouseleave", () => {
        setSvgHidden(crosshairX, true);
        setSvgHidden(crosshairY, true);
        setSvgHidden(crosshairPriceBackground, true);
        setSvgHidden(crosshairPrice, true);
        setSvgHidden(crosshairDate, true);
        updateInspector(visiblePoints[visiblePoints.length - 1]);
    });
    render();
    animateNewCandle();
}

setupStockChart();

function setupStockOrderTabs() {
    document.querySelectorAll("[data-stock-order-context]").forEach((context) => {
        const tabs = context.querySelectorAll("[data-stock-order-tab]");
        const forms = context.querySelectorAll("[data-stock-order-form]");
        tabs.forEach((tab) => {
            tab.addEventListener("click", () => {
                const selectedSide = tab.dataset.stockOrderTab;
                tabs.forEach((item) => item.classList.toggle("active", item === tab));
                forms.forEach((form) => {
                    form.classList.toggle("active", form.dataset.stockOrderForm === selectedSide);
                });
            });
        });
    });
}

setupStockOrderTabs();

function setupStockAccountDialog() {
    const dialog = document.querySelector("[data-stock-account-dialog]");
    const openButton = document.querySelector("[data-stock-account-open]");
    const closeButton = dialog?.querySelector("[data-stock-account-close]");
    if (!dialog || !openButton || !closeButton) {
        return;
    }

    openButton.addEventListener("click", () => {
        dialog.showModal();
        syncGamePauseState();
    });
    closeButton.addEventListener("click", () => dialog.close());
    dialog.addEventListener("click", (event) => {
        if (event.target === dialog) {
            dialog.close();
        }
    });
    dialog.addEventListener("close", () => {
        syncGamePauseState();
    });
}

setupStockAccountDialog();

function setupStockNewsPanel() {
    const panel = document.querySelector("[data-stock-news-panel]");
    const dialog = document.querySelector("[data-stock-news-dialog]");
    if (!panel || !dialog) {
        return;
    }

    const filterButtons = panel.querySelectorAll("[data-stock-news-filter]");
    const newsItems = panel.querySelectorAll("[data-stock-news-category]");
    const emptyMessage = panel.querySelector(".stock-news-empty");
    const detailArticles = dialog.querySelectorAll("[data-stock-news-detail]");
    const closeButtons = dialog.querySelectorAll("[data-stock-news-close]");

    function applyNewsFilter(filter) {
        let visibleCount = 0;
        newsItems.forEach((item) => {
            const visible = filter === "all" || item.dataset.stockNewsCategory === filter;
            item.hidden = !visible;
            if (visible) {
                visibleCount++;
            }
        });
        filterButtons.forEach((button) => {
            const selected = button.dataset.stockNewsFilter === filter;
            button.classList.toggle("active", selected);
            button.setAttribute("aria-selected", String(selected));
        });
        if (emptyMessage) {
            emptyMessage.hidden = visibleCount > 0;
        }
    }

    filterButtons.forEach((button) => {
        button.addEventListener("click", () => applyNewsFilter(button.dataset.stockNewsFilter));
    });

    newsItems.forEach((item) => {
        item.addEventListener("click", () => {
            const detailKey = item.dataset.stockNewsOpen;
            let selectedDetail = null;
            detailArticles.forEach((detail) => {
                const selected = detail.dataset.stockNewsDetail === detailKey;
                detail.hidden = !selected;
                if (selected) {
                    selectedDetail = detail;
                }
            });
            if (!selectedDetail) {
                return;
            }
            if (item.classList.contains("unread") && item.dataset.stockNewsReadUrl) {
                fetch(item.dataset.stockNewsReadUrl, { method: "POST" }).then((response) => response.json()).then((result) => {
                    if (result.read) {
                        item.classList.remove("unread");
                        item.querySelector(".stock-news-new")?.remove();
                    }
                }).catch(() => {});
            }
            dialog.showModal();
            syncGamePauseState();
            dialog.querySelector("[data-stock-news-close]")?.focus();
        });
    });

    closeButtons.forEach((button) => button.addEventListener("click", () => dialog.close()));
    dialog.addEventListener("click", (event) => {
        if (event.target === dialog) {
            dialog.close();
        }
    });
    dialog.addEventListener("close", syncGamePauseState);
    applyNewsFilter("all");
}

setupStockNewsPanel();

function setupCompanyInformationDialog(kind) {
    const dialog = document.querySelector(`[data-company-${kind}-dialog]`);
    const openButtons = document.querySelectorAll(`[data-company-${kind}-open]`);
    if (!dialog || openButtons.length === 0) {
        return;
    }
    const detailArticles = dialog.querySelectorAll(`[data-company-${kind}-detail]`);
    const closeButtons = dialog.querySelectorAll("[data-company-dialog-close]");

    openButtons.forEach((button) => {
        button.addEventListener("click", () => {
            const detailKey = button.dataset[`company${kind[0].toUpperCase()}${kind.slice(1)}Open`];
            let selected = null;
            detailArticles.forEach((article) => {
                const articleKey = article.dataset[`company${kind[0].toUpperCase()}${kind.slice(1)}Detail`];
                article.hidden = articleKey !== detailKey;
                if (!article.hidden) {
                    selected = article;
                }
            });
            if (!selected) {
                return;
            }
            const completesCompanyTutorial = kind === "report"
                && detailKey === "1"
                && document.querySelector("[data-company-tutorial-notice]");
            if ((button.classList.contains("unread") || completesCompanyTutorial) && button.dataset.companyReadUrl) {
                fetch(button.dataset.companyReadUrl, { method: "POST" })
                    .then((response) => response.json())
                    .then((result) => {
                        if (result.read) {
                            button.classList.remove("unread");
                            button.querySelector(".company-new-mark")?.remove();
                            const unreadCount = document.querySelector(`[data-company-unread-count="${kind}"]`);
                            if (unreadCount) {
                                const currentCount = Number(unreadCount.textContent.replace(/[^0-9]/g, "")) || 0;
                                const nextCount = Math.max(0, currentCount - 1);
                                unreadCount.textContent = `미확인 ${nextCount}`;
                                unreadCount.hidden = nextCount === 0;
                            }
                            if (kind === "report" && detailKey === "1") {
                                document.querySelector("[data-company-tutorial-notice]")?.remove();
                            }
                        }
                    })
                    .catch(() => {});
            }
            dialog.showModal();
            syncGamePauseState();
            dialog.querySelector("[data-company-dialog-close]")?.focus();
        });
    });
    closeButtons.forEach((button) => button.addEventListener("click", () => dialog.close()));
    dialog.addEventListener("click", (event) => {
        if (event.target === dialog) {
            dialog.close();
        }
    });
    dialog.addEventListener("close", syncGamePauseState);
}

setupCompanyInformationDialog("news");
setupCompanyInformationDialog("report");

function setupStockExchangeTabs() {
    const dialog = document.querySelector("[data-stock-account-dialog]");
    if (!dialog) {
        return;
    }
    const tabs = dialog.querySelectorAll("[data-stock-exchange-tab]");
    const forms = dialog.querySelectorAll("[data-stock-exchange-form]");
    tabs.forEach((tab) => {
        tab.addEventListener("click", () => {
            const type = tab.dataset.stockExchangeTab;
            tabs.forEach((item) => item.classList.toggle("active", item === tab));
            forms.forEach((form) => form.classList.toggle("active", form.dataset.exchangeType === type));
        });
    });
}

setupStockExchangeTabs();

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

function stockPanelRoot() {
    return document.querySelector(".stock-panel");
}

function playerSecuritiesCashBalance() {
    return Number(stockPanelRoot()?.dataset.securitiesCash || 0);
}

function playerCashBalance() {
    return Number(stockPanelRoot()?.dataset.playerCash || 0);
}

function readStockOrderPercents() {
    try {
        return JSON.parse(window.sessionStorage.getItem(STOCK_ORDER_PERCENT_KEY) || "{}");
    } catch {
        return {};
    }
}

function stockOrderQuantityKey(form) {
    const context = form?.closest("[data-stock-order-context]");
    if (!context || !form) {
        return "";
    }
    const side = form.hasAttribute("data-stock-buy-form") ? "buy" : "sell";
    return `${context.dataset.stockOrderContext}:${side}`;
}

function saveStockOrderPercent(form, value) {
    const key = stockOrderQuantityKey(form);
    if (!key) {
        return;
    }
    const saved = readStockOrderPercents();
    saved[key] = String(Math.max(1, Math.min(100, Math.trunc(Number(value) || 50))));
    window.sessionStorage.setItem(STOCK_ORDER_PERCENT_KEY, JSON.stringify(saved));
}

function restoreStockOrderPercents() {
    const saved = readStockOrderPercents();
    document.querySelectorAll(".stock-order-form").forEach((form) => {
        const key = stockOrderQuantityKey(form);
        const range = form.querySelector("[data-stock-order-percent]");
        if (key && range && saved[key]) {
            range.value = saved[key];
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
        const input = form.querySelector("input[name='amount']");
        const value = saved[form.dataset.exchangeType];
        if (input && value) {
            input.value = value;
        }
    });
}

function updateStockTradeEstimates() {
    // 비율을 현재 주문 가능한 최대 수량으로 환산한다. 서버는 전달된 최종 수량을 다시 검증한다.
    document.querySelectorAll("[data-stock-order-context]").forEach((context) => {
        const price = Number(context.dataset.stockPrice || 0);
        const ownedQuantity = Number(context.dataset.ownedQuantity || 0);
        const marketBuyLimit = Number(context.dataset.marketBuyLimit || 0);
        const marketSellLimit = Number(context.dataset.marketSellLimit || 0);
        const maxBuyQuantity = marketBuyLimit;
        context.querySelectorAll(".stock-order-form").forEach((form) => {
            const isBuy = form.hasAttribute("data-stock-buy-form");
            const maximum = isBuy ? maxBuyQuantity : Math.min(ownedQuantity, marketSellLimit);
            const range = form.querySelector("[data-stock-order-percent]");
            const quantityInput = form.querySelector("[data-stock-order-quantity]");
            if (!range || !quantityInput) {
                return;
            }
            const percent = Math.max(1, Math.min(100, Number(range.value || 50)));
            const quantity = maximum <= 0 ? 0 : (percent >= 100 ? maximum : Math.max(1, Math.floor(maximum * percent / 100)));
            const grossAmount = price * quantity;
            const fee = stockTradeFee(grossAmount);
            const netAmount = isBuy ? grossAmount + fee : Math.max(0, grossAmount - fee);
            quantityInput.value = String(Math.max(1, quantity));
            form.querySelector("[data-stock-order-percent-output]").textContent = `${percent}%`;
            form.querySelector("[data-stock-order-quantity-output]").textContent = `${quantity}주`;
            form.querySelector("[data-stock-order-gross-output]").textContent = formatStockAmount(grossAmount, "원");
            form.querySelector("[data-stock-order-fee-output]").textContent = formatStockAmount(fee, "원");
            form.querySelector("[data-stock-order-net-output]").textContent = formatStockAmount(netAmount, "원");
            const submit = form.querySelector("[data-stock-order-submit]");
            if (submit) {
                const paused = document.body.classList.contains("game-paused");
                submit.textContent = paused ? "일시정지" : `${percent}% ${isBuy ? "매수" : "매도"}`;
                submit.disabled = paused || maximum <= 0;
            }
            form.querySelectorAll("[data-order-percent]").forEach((button) => {
                button.classList.toggle("active", Number(button.dataset.orderPercent) === percent);
            });
        });
    });
}

document.querySelectorAll(".stock-order-form").forEach((form) => {
    form.addEventListener("submit", saveScrollPosition);
});

function updateStockExchangeEstimates() {
    document.querySelectorAll("[data-stock-exchange-form]").forEach((form) => {
        const input = form.querySelector("input[name='amount']");
        const preview = form.querySelector("[data-exchange-preview]");
        if (!input || !preview) {
            return;
        }
        const amount = Math.max(0, Number(input.value || 0));
        if (form.dataset.exchangeType === "deposit") {
            const maximum = playerCashBalance();
            preview.textContent = `입금 가능 ${formatCashAmount(maximum)} / 입금 ${formatCashAmount(amount)}`;
            input.max = String(Math.max(1, maximum));
        } else {
            const maximum = playerSecuritiesCashBalance();
            preview.textContent = `출금 가능 ${formatCashAmount(maximum)} / 출금 ${formatCashAmount(amount)}`;
            input.max = String(Math.max(1, maximum));
        }
    });
}

function setupStockEstimateInputs() {
    restoreStockOrderPercents();
    restoreStockExchangeQuantities();
    document.querySelectorAll("[data-stock-order-percent]").forEach((range) => {
        range.addEventListener("input", () => {
            saveStockOrderPercent(range.closest(".stock-order-form"), range.value);
            updateStockTradeEstimates();
        });
    });
    document.querySelectorAll("[data-stock-exchange-form] input[name='amount']").forEach((input) => {
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
    const button = event.target.closest("[data-order-percent]");
    if (!button) {
        return;
    }
    const form = button.closest(".stock-order-form");
    const range = form?.querySelector("[data-stock-order-percent]");
    if (!form || !range) {
        return;
    }
    event.preventDefault();
    event.stopPropagation();
    range.value = button.dataset.orderPercent;
    saveStockOrderPercent(form, range.value);
    range.dispatchEvent(new Event("input", { bubbles: true }));
    updateStockTradeEstimates();
}, true);

document.addEventListener("click", (event) => {
    const button = event.target.closest("[data-exchange-action]");
    if (!button) {
        return;
    }
    const form = button.closest("[data-stock-exchange-form]");
    const input = form?.querySelector("input[name='amount']");
    if (!form || !input) {
        return;
    }
    event.preventDefault();
    event.stopPropagation();
    const maximum = form.dataset.exchangeType === "deposit"
        ? playerCashBalance()
        : playerSecuritiesCashBalance();
    let nextAmount = Number(input.value || 0);
    if (button.dataset.exchangeAction === "max") {
        nextAmount = maximum;
    } else if (button.dataset.exchangeAction === "set") {
        nextAmount = Number(button.dataset.exchangeValue || 0);
    }
    input.value = String(Math.max(1, Math.min(Math.max(1, maximum), Math.trunc(nextAmount) || 1)));
    form.querySelectorAll("[data-exchange-action]").forEach((item) => item.classList.toggle("active", item === button));
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
    // 증권계좌 입출금은 fetch로 처리해 페이지 전체를 새로고침하지 않고 잔액과 미리보기만 갱신한다.
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
                if (securitiesCashValue && result.securitiesCash) {
                    securitiesCashValue.textContent = result.securitiesCash;
                }
                const railSecuritiesCash = document.querySelector("#stockRailSecuritiesCash");
                const dialogCash = document.querySelector("#stockDialogCash");
                const dialogSecuritiesCash = document.querySelector("#stockDialogSecuritiesCash");
                if (railSecuritiesCash && result.securitiesCash) {
                    railSecuritiesCash.textContent = result.securitiesCash;
                }
                if (dialogCash && result.cash) {
                    dialogCash.textContent = result.cash;
                }
                if (dialogSecuritiesCash && result.securitiesCash) {
                    dialogSecuritiesCash.textContent = result.securitiesCash;
                }
                if (result.cashRaw && stockPanelRoot()) {
                    stockPanelRoot().dataset.playerCash = result.cashRaw;
                }
                if (result.securitiesCashRaw && stockPanelRoot()) {
                    stockPanelRoot().dataset.securitiesCash = result.securitiesCashRaw;
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

function showToast(message, durationMs = 8000) {
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
    }, Math.max(1000, durationMs));
}

function restorePendingTickToast() {
    try {
        const pending = JSON.parse(window.sessionStorage.getItem(PENDING_TICK_TOAST_KEY) || "null");
        window.sessionStorage.removeItem(PENDING_TICK_TOAST_KEY);
        if (pending?.message && pending.expiresAt > Date.now()) {
            showToast(pending.message, pending.expiresAt - Date.now());
        }
    } catch {
        window.sessionStorage.removeItem(PENDING_TICK_TOAST_KEY);
    }
}

if (flashToast) {
    window.setTimeout(() => {
        flashToast.classList.remove("show");
        window.setTimeout(() => flashToast.remove(), 300);
    }, 8000);
}

restorePendingTickToast();

function shouldKeepGamePaused() {
    return document.body.dataset.playerPaused === "true"
        || !!document.querySelector(".event-modal-backdrop:not([hidden])")
        || !!document.querySelector("#secretaryModal:not([hidden])")
        || !!document.querySelector("#confirmModal:not([hidden])")
        || !!document.querySelector(".ability-modal-backdrop:not([hidden])")
        || !!document.querySelector(".gift-select-popover:not([hidden])")
        || !!document.querySelector("[data-stock-account-dialog][open]")
        || !!document.querySelector("[data-stock-news-dialog][open]")
        || !!document.querySelector("[data-company-news-dialog][open]")
        || !!document.querySelector("[data-company-report-dialog][open]")
        || !!document.querySelector("[data-company-command-dialog][open]");
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

function setupCompanyDashboard() {
    /*
     * 가운데 운영 항목을 선택하면 같은 key를 가진 좌측 상세 정보와 우측 비서 정보를 함께 연다.
     * 서버가 렌더링한 데이터를 다시 계산하지 않고 표시 상태만 바꾸므로 페이지 이동이나 날짜 게이지 초기화가 없다.
     */
    const dashboard = document.querySelector("[data-company-dashboard]");
    if (!dashboard) {
        return;
    }

    const operationButtons = [...dashboard.querySelectorAll("[data-company-operation-key]")];
    const executiveCommandButtons = [...dashboard.querySelectorAll("[data-company-executive-command-target]")];
    const workQueueButtons = [...dashboard.querySelectorAll("[data-company-work-target]")];
    const detailViews = [...dashboard.querySelectorAll("[data-company-detail-key]")];
    const secretaryViews = [...dashboard.querySelectorAll("[data-company-secretary-key]")];
    const secretaryDepartment = dashboard.querySelector("[data-company-secretary-department]");
    const secretaryName = dashboard.querySelector("[data-company-secretary-name]");
    const selectedStatus = dashboard.querySelector("[data-company-selected-status]");
    const actionBar = dashboard.querySelector("[data-company-detail-actionbar]");
    const actionLabel = dashboard.querySelector("[data-company-detail-action-label]");
    const actionButton = dashboard.querySelector("[data-company-detail-action]");
    const actionSelect = dashboard.querySelector("[data-company-detail-action-select]");
    let selectedPrimaryActions = [];
    const selectionStorageKey = "company-dashboard-selected-item";
    const detailScrollStorageKey = "company-dashboard-detail-scroll";
    const secretaryScrollStorageKey = "company-dashboard-secretary-scroll";
    const detailSectionStorageKey = "company-dashboard-detail-section";
    const availableKeys = new Set(operationButtons.map((button) => button.dataset.companyOperationKey));
    const detailSectionControllers = new Map();

    function scrollKey(prefix, key) {
        return `${prefix}:${key}`;
    }

    function restorePanelScroll(view, prefix, key) {
        if (!view) {
            return;
        }
        const saved = Number(sessionStorage.getItem(scrollKey(prefix, key))) || 0;
        window.requestAnimationFrame(() => {
            view.scrollTop = saved;
        });
    }

    function setupDetailSections(view) {
        const sections = [...view.querySelectorAll(":scope > [data-company-detail-section]")];
        if (sections.length < 2) {
            return;
        }
        const navigation = document.createElement("nav");
        navigation.className = "company-detail-section-tabs";
        navigation.setAttribute("aria-label", "선택 상세 구역");
        const buttons = sections.map((section, index) => {
            const button = document.createElement("button");
            button.type = "button";
            button.textContent = section.dataset.companyDetailSection;
            button.addEventListener("click", () => selectDetailSection(view, index, true));
            navigation.append(button);
            return button;
        });
        const highlight = view.querySelector(":scope > .company-detail-highlight");
        highlight?.insertAdjacentElement("afterend", navigation);
        detailSectionControllers.set(view, { sections, buttons });
        const savedIndex = Number(sessionStorage.getItem(scrollKey(
            detailSectionStorageKey,
            view.dataset.companyDetailKey
        )));
        selectDetailSection(view, Number.isInteger(savedIndex) ? savedIndex : 0, false);
    }

    function selectDetailSection(view, requestedIndex, focusNavigation) {
        const controller = detailSectionControllers.get(view);
        if (!controller) {
            return;
        }
        const index = Math.min(Math.max(requestedIndex, 0), controller.sections.length - 1);
        controller.sections.forEach((section, sectionIndex) => {
            section.hidden = sectionIndex !== index;
        });
        controller.buttons.forEach((button, buttonIndex) => {
            const selected = buttonIndex === index;
            button.classList.toggle("active", selected);
            button.setAttribute("aria-pressed", String(selected));
        });
        sessionStorage.setItem(scrollKey(detailSectionStorageKey, view.dataset.companyDetailKey), String(index));
        if (focusNavigation) {
            controller.buttons[index].focus({ preventScroll: true });
        }
    }

    function revealActionSection(action) {
        const view = action.closest("[data-company-detail-key]");
        const section = action.closest("[data-company-detail-section]");
        const controller = detailSectionControllers.get(view);
        if (!section || !controller) {
            return;
        }
        selectDetailSection(view, controller.sections.indexOf(section), false);
    }

    detailViews.forEach(setupDetailSections);

    function selectCompanyItem(key) {
        if (!availableKeys.has(key)) {
            return;
        }
        operationButtons.forEach((button) => {
            const selected = button.dataset.companyOperationKey === key;
            button.classList.toggle("active", selected);
            button.setAttribute("aria-pressed", String(selected));
        });
        detailViews.forEach((view) => view.classList.toggle("active", view.dataset.companyDetailKey === key));
        secretaryViews.forEach((view) => view.classList.toggle("active", view.dataset.companySecretaryKey === key));
        const selectedSecretary = secretaryViews.find((view) => view.dataset.companySecretaryKey === key);
        const selectedDetail = detailViews.find((view) => view.dataset.companyDetailKey === key);
        const selectedOperation = operationButtons.find((button) => button.dataset.companyOperationKey === key);
        if (selectedStatus && selectedOperation) {
            selectedStatus.textContent = selectedOperation.dataset.companyOperationStatus || "상태 확인";
            selectedStatus.classList.remove("good", "warn", "danger", "muted");
            if (selectedOperation.dataset.companyOperationTone) {
                selectedStatus.classList.add(selectedOperation.dataset.companyOperationTone);
            }
        }
        if (selectedSecretary && secretaryDepartment && secretaryName) {
            secretaryDepartment.textContent = selectedSecretary.dataset.companySecretaryDepartment;
            secretaryName.textContent = selectedSecretary.dataset.companySecretaryName;
        }
        dashboard.dataset.selectedCompanyKey = key;
        sessionStorage.setItem(selectionStorageKey, key);
        restorePanelScroll(selectedDetail, detailScrollStorageKey, key);
        restorePanelScroll(selectedSecretary, secretaryScrollStorageKey, key);
        if (actionBar && actionLabel && actionButton) {
            selectedPrimaryActions = selectedDetail
                ? [...selectedDetail.querySelectorAll('form button[type="submit"]:not(:disabled)')]
                    .filter((button) => !button.classList.contains("secondary"))
                    .filter((button) => !["거절", "계약 종료"].includes(button.textContent.trim()))
                : [];
            actionBar.hidden = selectedPrimaryActions.length === 0;
            dashboard.querySelector(".company-detail-panel")
                ?.classList.toggle("has-actionbar", selectedPrimaryActions.length > 0);
            if (selectedPrimaryActions.length === 1) {
                const label = selectedPrimaryActions[0].textContent.trim();
                actionLabel.textContent = label;
                actionButton.textContent = label;
                if (actionSelect) {
                    actionSelect.hidden = true;
                    actionSelect.replaceChildren();
                }
            } else if (selectedPrimaryActions.length > 1) {
                actionLabel.textContent = `사용 가능한 명령 ${selectedPrimaryActions.length}개 중 선택`;
                actionButton.textContent = "명령으로 이동";
                if (actionSelect) {
                    actionSelect.replaceChildren();
                    selectedPrimaryActions.forEach((button, index) => {
                        const option = document.createElement("option");
                        const sectionLabel = button.closest("[data-company-detail-section]")
                            ?.dataset.companyDetailSection;
                        const itemLabel = button.closest("article")
                            ?.querySelector(":scope > div strong")
                            ?.textContent.trim();
                        option.value = String(index);
                        option.textContent = [sectionLabel, itemLabel, button.textContent.trim()]
                            .filter(Boolean)
                            .join(" · ");
                        actionSelect.append(option);
                    });
                    actionSelect.hidden = false;
                }
            }
        }
    }

    detailViews.forEach((view) => {
        view.addEventListener("scroll", () => {
            sessionStorage.setItem(scrollKey(detailScrollStorageKey, view.dataset.companyDetailKey), String(view.scrollTop));
        });
    });
    secretaryViews.forEach((view) => {
        view.addEventListener("scroll", () => {
            sessionStorage.setItem(scrollKey(secretaryScrollStorageKey, view.dataset.companySecretaryKey), String(view.scrollTop));
        });
    });
    operationButtons.forEach((button) => {
        button.addEventListener("click", () => selectCompanyItem(button.dataset.companyOperationKey));
    });
    workQueueButtons.forEach((button) => {
        button.addEventListener("click", () => {
            const targetKey = button.dataset.companyWorkTarget;
            selectCompanyItem(targetKey);
            dashboard.querySelector(`[data-company-operation-key="${targetKey}"]`)
                ?.scrollIntoView({ block: "center", behavior: "smooth" });
        });
    });
    executiveCommandButtons.forEach((button) => {
        button.addEventListener("click", () => {
            const targetKey = button.dataset.companyExecutiveCommandTarget;
            selectCompanyItem(targetKey);
            window.requestAnimationFrame(() => {
                const targetDetail = detailViews.find((view) => view.dataset.companyDetailKey === targetKey);
                const firstCommand = targetDetail?.querySelector('form button[type="submit"]:not(:disabled)');
                if (firstCommand) {
                    firstCommand.scrollIntoView({ block: "center", behavior: "smooth" });
                    firstCommand.focus({ preventScroll: true });
                    return;
                }
                targetDetail?.scrollTo({ top: 0, behavior: "smooth" });
                targetDetail?.focus({ preventScroll: true });
            });
        });
    });
    actionButton?.addEventListener("click", () => {
        if (selectedPrimaryActions.length === 1) {
            selectedPrimaryActions[0].click();
            return;
        }
        const selectedAction = selectedPrimaryActions[Number(actionSelect?.value) || 0];
        if (selectedAction) {
            revealActionSection(selectedAction);
            window.requestAnimationFrame(() => {
                selectedAction.scrollIntoView({ block: "center", behavior: "smooth" });
                selectedAction.focus({ preventScroll: true });
            });
        }
    });
    const tutorialReportButton = dashboard.querySelector("[data-company-open-first-report]");
    tutorialReportButton?.addEventListener("click", () => {
        dashboard.querySelector('[data-company-report-open="1"]')?.click();
    });

    const budgetPolicy = dashboard.querySelector("[data-company-budget-policy]");
    if (budgetPolicy) {
        const developmentSelect = budgetPolicy.querySelector('select[name="developmentPolicy"]');
        const marketingSelect = budgetPolicy.querySelector('select[name="marketingPolicy"]');
        const developmentOutput = budgetPolicy.querySelector("[data-company-development-cost-preview]");
        const marketingOutput = budgetPolicy.querySelector("[data-company-marketing-cost-preview]");
        const standardDevelopmentCost = Number(budgetPolicy.dataset.standardDevelopmentCost) || 0;
        const standardMarketingCost = Number(budgetPolicy.dataset.standardMarketingCost) || 0;

        function formatCompanyBudgetMoney(value) {
            let remaining = Math.max(0, Math.trunc(value));
            const jo = Math.floor(remaining / 1_000_000_000_000);
            remaining %= 1_000_000_000_000;
            const eok = Math.floor(remaining / 100_000_000);
            remaining %= 100_000_000;
            const man = Math.floor(remaining / 10_000);
            const parts = [];
            if (jo > 0) parts.push(`${jo}조`);
            if (eok > 0) parts.push(`${eok}억`);
            if (man > 0 && jo === 0) parts.push(`${man}만`);
            if (parts.length === 0) parts.push(String(remaining));
            return `${parts.join("")}원`;
        }

        function selectedSpendingPercent(select) {
            return Number(select?.selectedOptions[0]?.dataset.spendingPercent) || 0;
        }

        function updateBudgetPreview() {
            if (developmentOutput) {
                developmentOutput.textContent = formatCompanyBudgetMoney(
                    Math.trunc(standardDevelopmentCost * selectedSpendingPercent(developmentSelect) / 100)
                );
            }
            if (marketingOutput) {
                marketingOutput.textContent = formatCompanyBudgetMoney(
                    Math.trunc(standardMarketingCost * selectedSpendingPercent(marketingSelect) / 100)
                );
            }
        }

        developmentSelect?.addEventListener("change", updateBudgetPreview);
        marketingSelect?.addEventListener("change", updateBudgetPreview);
    }
    const storedSelection = sessionStorage.getItem(selectionStorageKey);
    selectCompanyItem(availableKeys.has(storedSelection) ? storedSelection : dashboard.dataset.selectedCompanyKey);
}

function setupCompanyPreparation() {
    const preparation = document.querySelector("[data-company-preparation]");
    if (!preparation) {
        return;
    }

    const amountInput = preparation.querySelector("#companyInvestmentAmount");
    const presetButtons = [...preparation.querySelectorAll("[data-company-investment-preset]")];
    const allButton = preparation.querySelector("[data-company-investment-all]");
    const validation = preparation.querySelector("#companyInvestmentValidation");
    const corporateCash = preparation.querySelector("#companyCorporateCashPreview");
    const twelveMonthCash = preparation.querySelector("#companyTwelveMonthCashPreview");
    const runway = preparation.querySelector("#companyRunwayPreview");
    const runwayTone = preparation.querySelector("#companyRunwayTone");
    const personalCash = Number(preparation.dataset.personalCash);
    const minimumInvestment = Number(preparation.dataset.minimumInvestment);
    const trainingCost = Number(preparation.dataset.trainingCost);
    const monthlyFixedCost = Number(preparation.dataset.monthlyFixedCost);

    function formatCompanyMoney(value) {
        const sign = value < 0 ? "-" : "";
        let remaining = Math.abs(Math.trunc(value));
        const jo = Math.floor(remaining / 1_000_000_000_000);
        remaining %= 1_000_000_000_000;
        const eok = Math.floor(remaining / 100_000_000);
        remaining %= 100_000_000;
        const man = Math.floor(remaining / 10_000);
        const parts = [];
        if (jo > 0) parts.push(`${jo}조`);
        if (eok > 0) parts.push(`${eok}억`);
        if (man > 0 && jo === 0) parts.push(`${man}만`);
        if (parts.length === 0) parts.push(`${remaining}`);
        return `${sign}${parts.join("")}원`;
    }

    function renderPreview() {
        const investment = Math.max(0, Math.trunc(Number(amountInput.value) || 0));
        const afterTraining = investment - trainingCost;
        const afterTwelveMonths = afterTraining - monthlyFixedCost * 12;
        const runwayMonths = afterTraining > 0 ? afterTraining / monthlyFixedCost : 0;
        const valid = investment >= minimumInvestment && investment <= personalCash;

        corporateCash.textContent = formatCompanyMoney(afterTraining);
        twelveMonthCash.textContent = formatCompanyMoney(afterTwelveMonths);
        twelveMonthCash.classList.toggle("negative", afterTwelveMonths < 0);
        runway.textContent = `${runwayMonths.toFixed(1)}개월`;
        runwayTone.textContent = runwayMonths < 12 ? "자금 주의" : "12개월 이상";
        runwayTone.classList.toggle("warn", runwayMonths < 12);

        if (investment < minimumInvestment) {
            validation.textContent = "최소 출자금은 1,500억원입니다.";
        } else if (investment > personalCash) {
            validation.textContent = "보유한 개인 현금을 초과할 수 없습니다.";
        } else {
            validation.textContent = "출자 가능한 금액입니다.";
        }
        validation.classList.toggle("valid", valid);
        presetButtons.forEach((button) => button.classList.toggle("active", Number(button.dataset.amount) === investment));
        allButton.classList.toggle("active", investment === personalCash);
    }

    presetButtons.forEach((button) => {
        button.addEventListener("click", () => {
            amountInput.value = button.dataset.amount;
            renderPreview();
        });
    });
    allButton.addEventListener("click", () => {
        amountInput.value = String(personalCash);
        renderPreview();
    });
    amountInput.addEventListener("input", renderPreview);
    renderPreview();
}

function setupCompanyHiring() {
    const hiring = document.querySelector("[data-company-hiring]");
    if (!hiring) return;
    const checkboxes = [...hiring.querySelectorAll('input[name="candidateKeys"]')];
    const submit = hiring.querySelector("[data-company-hiring-submit]");
    const selectedCount = hiring.querySelector("[data-company-selected-count]");
    const signingTotal = hiring.querySelector("[data-company-signing-total]");

    function renderHiring() {
        const selected = checkboxes.filter((checkbox) => checkbox.checked);
        const counts = { AI_DEVELOPMENT: 0, SALES_MARKETING: 0, SERVICE_OPERATIONS: 0 };
        let total = 0;
        selected.forEach((checkbox) => {
            const row = checkbox.closest("[data-department]");
            counts[row.dataset.department] += 1;
            total += Number(row.dataset.signingBonus);
        });
        selectedCount.textContent = String(selected.length);
        Object.entries(counts).forEach(([department, count]) => {
            hiring.querySelector(`[data-company-count="${department}"]`).textContent = String(count);
        });
        signingTotal.textContent = new Intl.NumberFormat("ko-KR").format(total) + "원";
        checkboxes.forEach((checkbox) => {
            checkbox.disabled = !checkbox.checked && selected.length >= 6;
        });
        submit.disabled = !(selected.length === 6 && counts.AI_DEVELOPMENT >= 2
            && counts.SALES_MARKETING >= 1 && counts.SERVICE_OPERATIONS >= 1);
    }
    checkboxes.forEach((checkbox) => checkbox.addEventListener("change", renderHiring));
    renderHiring();
}

function setupCompanyCommandConfirmation() {
    const dialog = document.querySelector("[data-company-command-dialog]");
    if (!dialog) {
        return;
    }
    const importantActions = [
        "/companies/funding",
        "/companies/finance/",
        "/companies/workforce/hire",
        "/companies/workforce/core-hire",
        "/companies/workforce/training",
        "/companies/workforce/retention",
        "/companies/departments",
        "/companies/organization/upgrade",
        "/companies/infrastructure/",
        "/companies/projects/",
        "/companies/contracts/",
        "/companies/incidents/"
    ];
    const confirmedForms = new WeakSet();
    const title = dialog.querySelector("[data-company-command-title]");
    const messageText = dialog.querySelector("[data-company-command-message]");
    const confirmButton = dialog.querySelector("[data-company-command-confirm]");
    const cancelButtons = dialog.querySelectorAll("[data-company-command-cancel]");
    let pendingForm = null;
    let pendingSubmitter = null;

    const closeDialog = () => {
        pendingForm = null;
        pendingSubmitter = null;
        dialog.close();
    };

    cancelButtons.forEach((button) => button.addEventListener("click", closeDialog));
    dialog.addEventListener("cancel", (event) => {
        event.preventDefault();
        closeDialog();
    });
    dialog.addEventListener("click", (event) => {
        if (event.target === dialog) {
            closeDialog();
        }
    });
    dialog.addEventListener("close", syncGamePauseState);
    confirmButton.addEventListener("click", () => {
        if (!pendingForm) {
            return;
        }
        const form = pendingForm;
        const submitter = pendingSubmitter;
        pendingForm = null;
        pendingSubmitter = null;
        confirmedForms.add(form);
        dialog.close();
        if (submitter) {
            form.requestSubmit(submitter);
        } else {
            form.requestSubmit();
        }
    });

    document.querySelectorAll('form[method="post"][action^="/companies"]').forEach((form) => {
        const action = form.getAttribute("action") || "";
        const customMessage = form.dataset.companyConfirm;
        if (!customMessage && !importantActions.some((prefix) => action.startsWith(prefix))) {
            return;
        }
        form.addEventListener("submit", (event) => {
            if (confirmedForms.has(form)) {
                confirmedForms.delete(form);
                const submitButton = event.submitter || form.querySelector('button[type="submit"]');
                if (submitButton) {
                    submitButton.disabled = true;
                }
                return;
            }
            event.preventDefault();
            const message = customMessage
                || "비용, 인력 또는 진행 상태가 즉시 변경됩니다. 이 명령을 실행합니까?";
            pendingForm = form;
            pendingSubmitter = event.submitter || form.querySelector('button[type="submit"]');
            title.textContent = action.startsWith("/companies/incidents/")
                ? "서비스 장애 대응을 확정합니까?"
                : "기업 명령을 실행합니까?";
            messageText.textContent = message;
            dialog.showModal();
            syncGamePauseState();
            confirmButton.focus();
        });
    });
}

setupCompanyDashboard();
setupCompanyPreparation();
setupCompanyHiring();
setupCompanyCommandConfirmation();

async function advanceDay() {
    /*
     * 공통 시간 진행 함수다.
     *
     * 도시 화면과 주식 화면이 같은 함수를 사용해야 날짜가 한 번만 흐른다.
     * 주식 화면이면 view=stocks를 보내 서버가 도시 이벤트 표시만 지연시키고,
     * 날짜/정산/주가 갱신은 그대로 진행한다.
     */
    if (navigating || ticking || document.body.classList.contains("game-paused")) {
        return;
    }
    ticking = true;
    try {
        const view = document.body.dataset.viewMode || "city";
        const expectedElapsedDays = Number(document.body.dataset.elapsedDays);
        const response = await fetch(`/tick?view=${view}&expectedElapsedDays=${expectedElapsedDays}`, { method: "POST" });
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
            window.sessionStorage.setItem(PENDING_TICK_TOAST_KEY, JSON.stringify({
                message: result.notice,
                expiresAt: Date.now() + 8000
            }));
            saveScrollPosition();
            navigating = true;
            window.location.reload();
        } else {
            saveScrollPosition();
            navigating = true;
            window.location.reload();
        }
    } catch (error) {
        console.warn("tick failed", error);
        accumulatedTickMs = 0;
        tickSegmentStartedAt = Date.now();
        persistTickProgress(0);
    } finally {
        ticking = false;
    }
}

function currentTickProgressMs() {
    if (tickWasPaused) {
        return accumulatedTickMs;
    }
    return Math.min(TICK_DURATION_MS, accumulatedTickMs + Date.now() - tickSegmentStartedAt);
}

function persistTickProgress(progressMs = currentTickProgressMs()) {
    window.sessionStorage.setItem(GAME_TICK_PROGRESS_KEY, JSON.stringify({
        elapsedDays: Number(document.body.dataset.elapsedDays),
        progressMs: Math.max(0, Math.min(TICK_DURATION_MS, progressMs))
    }));
}

function syncTickPauseState() {
    const paused = document.body.classList.contains("game-paused");
    if (paused === tickWasPaused) {
        return paused;
    }
    if (paused) {
        accumulatedTickMs = currentTickProgressMs();
        persistTickProgress(accumulatedTickMs);
    } else {
        tickSegmentStartedAt = Date.now();
    }
    tickWasPaused = paused;
    return paused;
}

function updateDayProgress() {
    // 100ms마다 진행률 막대를 갱신하고, 5초가 지나면 advanceDay()를 호출한다.
    if (navigating) {
        return;
    }
    if (!dayProgress || !dayProgressText) {
        return;
    }
    if (syncTickPauseState()) {
        dayProgressText.textContent = "일시정지";
        return;
    }
    const elapsed = currentTickProgressMs();
    const percent = Math.min(100, Math.floor((elapsed / TICK_DURATION_MS) * 100));
    dayProgress.style.width = `${percent}%`;
    dayProgressText.textContent = `다음 날 ${percent}%`;
    if (Date.now() - lastTickProgressSaveAt >= 500) {
        persistTickProgress(elapsed);
        lastTickProgressSaveAt = Date.now();
    }
    if (elapsed >= TICK_DURATION_MS) {
        advanceDay();
    }
}

if (document.querySelector(".city-panel") || document.querySelector(".stock-panel") || document.body.classList.contains("view-company")) {
    window.setInterval(updateDayProgress, 100);
    window.addEventListener("pagehide", () => persistTickProgress());
}
