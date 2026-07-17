# styles.css 코드 주석형 해설

원본 파일: `src/main/resources/static/styles.css`

형식:
- 원본 코드를 그대로 배치한다.
- 필요한 코드 바로 아래에 해설 주석을 붙인다.
- import/package/단순 선언은 과하게 설명하지 않는다.

```css
/*
 * 전역 스타일 파일이다.
 *
 * 이 프로젝트는 서버가 HTML을 렌더링하고, CSS가 화면 배치와 상태 색상을 담당한다.
 * :root의 CSS 변수는 색상과 그림자를 한 곳에서 관리하기 위한 값이다.
 * 이후 섹션들은 대략 공통 레이아웃 -> 메인 화면 -> 주식/비서/모달 -> 반응형 순서로 구성된다.
 */
:root {
/* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
    --bg: #eef6fb;
    --sky: #bfe8ff;
    --panel: #ffffff;
    --ink: #1f2a37;
    --muted: #6b7280;
    --line: #d9e4ea;
    --blue: #2f80ed;
    --green: #2fb36d;
    --yellow: #f4b740;
    --red: #e25b4b;
    --shadow: 0 12px 30px rgba(46, 74, 95, 0.12);
}

* { box-sizing: border-box; }

html.restore-scroll-pending body {
/* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
    visibility: hidden;
}

body {
/* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
    margin: 0;
    background: linear-gradient(180deg, #dff3ff 0%, var(--bg) 42%, #f7fbfd 100%);
    color: var(--ink);
    font-family: Arial, "Malgun Gothic", sans-serif;
}

button, input {
/* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
    min-height: 38px;
    border-radius: 8px;
    font: inherit;
}

button {
/* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
    border: 0;
    background: var(--blue);
    color: white;
    padding: 0 14px;
    font-weight: 700;
    cursor: pointer;
    box-shadow: 0 4px 10px rgba(47, 128, 237, 0.22);
}

button:disabled, button.locked {
/* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
    cursor: not-allowed;
    box-shadow: none;
}

input {
/* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
    width: 100%;
    border: 1px solid var(--line);
    padding: 0 12px;
    background: white;
}

.secondary {
/* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
    background: #f4f7fa;
    color: var(--ink);
    border: 1px solid var(--line);
    box-shadow: none;
}

.danger-button {
/* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
    background: var(--red);
}

/* 전체 앱 폭과 상단바는 모든 주요 화면이 공유한다. */
.app {
/* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
    width: min(1480px, 100%);
    margin: 0 auto;
    padding: 20px;
}

.topbar {
/* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
    position: sticky;
    /* 해설: 요소의 위치 기준을 정한다. fixed/absolute/relative 등이 겹침과 배치에 영향을 준다. */
    top: 0;
    z-index: 30;
    display: grid;
    /* 해설: 요소의 배치 방식을 정한다. flex/grid/block 등이 화면 구조를 결정한다. */
    grid-template-columns: minmax(150px, 0.42fr) minmax(760px, 1.58fr);
    /* 해설: 레이아웃 정렬과 크기 배분을 설정한다. */
    gap: 12px;
    align-items: center;
    margin-bottom: 14px;
    padding: 10px 0;
    background: rgba(238, 246, 251, 0.94);
    backdrop-filter: blur(10px);
}

.brand, .story-head {
/* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
    display: flex;
    /* 해설: 요소의 배치 방식을 정한다. flex/grid/block 등이 화면 구조를 결정한다. */
    align-items: center;
    gap: 12px;
}

.logo {
/* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
    display: grid;
    /* 해설: 요소의 배치 방식을 정한다. flex/grid/block 등이 화면 구조를 결정한다. */
    place-items: center;
    width: 40px;
    height: 40px;
    border-radius: 10px;
    background: linear-gradient(135deg, #3ca2ff, #68d391);
    color: white;
    font-size: 22px;
    font-weight: 900;
}

h1, h2, h3, p { margin: 0; }
h1 { font-size: 22px; }
h2 { font-size: 22px; }
h3 { font-size: 17px; }
p, span, dt, li, small { color: var(--muted); }

.status-grid {
/* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
    display: grid;
    /* 해설: 요소의 배치 방식을 정한다. flex/grid/block 등이 화면 구조를 결정한다. */
    grid-template-columns: repeat(auto-fit, minmax(118px, 1fr));
    /* 해설: 레이아웃 정렬과 크기 배분을 설정한다. */
    gap: 8px;
}

.status-card, .panel, .city-panel, .story-card, .auth-card {
/* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
    background: rgba(255, 255, 255, 0.92);
    border: 1px solid var(--line);
    border-radius: 14px;
    box-shadow: var(--shadow);
}

.status-card { padding: 8px 10px; min-width: 0; }
.status-card span, .status-card strong { display: block; }
.status-card strong { margin-top: 3px; font-size: 15px; overflow-wrap: anywhere; }
.status-subline {
/* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
    display: block;
    /* 해설: 요소의 배치 방식을 정한다. flex/grid/block 등이 화면 구조를 결정한다. */
    margin-top: 5px;
    font-size: 11px;
    line-height: 1.2;
    overflow-wrap: anywhere;
}
.status-subline b {
/* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
    color: var(--ink);
}
.pause-card form { margin: 4px 0 0; }
.pause-card button { width: 100%; min-height: 30px; padding: 0 8px; }
.date-card strong { color: var(--blue); }

.tick-progress {
/* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
    overflow: hidden;
    width: 100%;
    height: 8px;
    margin-top: 8px;
    border-radius: 999px;
    background: #dbe8f0;
}

.tick-progress span {
/* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
    display: block;
    /* 해설: 요소의 배치 방식을 정한다. flex/grid/block 등이 화면 구조를 결정한다. */
    width: 0;
    height: 100%;
    border-radius: inherit;
    background: linear-gradient(90deg, var(--green), var(--blue));
}

.date-card small {
/* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
    display: block;
    /* 해설: 요소의 배치 방식을 정한다. flex/grid/block 등이 화면 구조를 결정한다. */
    margin-top: 4px;
    font-size: 11px;
}

.money {
/* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
    display: block;
    /* 해설: 요소의 배치 방식을 정한다. flex/grid/block 등이 화면 구조를 결정한다. */
    max-width: 100%;
    overflow-wrap: anywhere;
    line-height: 1.15;
}

.notice, .error {
/* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
    margin: 0 0 14px;
    padding: 12px 14px;
    border-radius: 10px;
    font-weight: 800;
}

.notice { background: #e4f8ec; color: #197a45; }
.error { background: #ffe9e6; color: #b93d32; }

.city-tabs {
/* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
    display: flex;
    /* 해설: 요소의 배치 방식을 정한다. flex/grid/block 등이 화면 구조를 결정한다. */
    gap: 8px;
    margin-bottom: 14px;
    overflow-x: auto;
    padding-bottom: 3px;
}

.city-tabs form { margin: 0; }
.city-tabs button, .info-tab {
/* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
    position: relative;
    /* 해설: 요소의 위치 기준을 정한다. fixed/absolute/relative 등이 겹침과 배치에 영향을 준다. */
    display: grid;
    /* 해설: 요소의 배치 방식을 정한다. flex/grid/block 등이 화면 구조를 결정한다. */
    place-items: center;
    min-width: 92px;
    min-height: 38px;
    background: white;
    color: var(--ink);
    border: 1px solid var(--line);
    border-radius: 8px;
    box-shadow: none;
    padding: 0 14px;
    font-weight: 700;
    text-decoration: none;
}

.tab-pause-form {
/* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
    margin: 0 0 0 auto;
}

.tab-pause-button {
/* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
    min-width: 104px;
    min-height: 38px;
    border-radius: 8px;
    background: var(--ink);
    color: white;
    border: 1px solid var(--ink);
    padding: 0 14px;
    font-weight: 800;
}

.repair-count-badge {
/* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
    position: absolute;
    /* 해설: 요소의 위치 기준을 정한다. fixed/absolute/relative 등이 겹침과 배치에 영향을 준다. */
    top: -7px;
    right: -7px;
    display: grid;
    /* 해설: 요소의 배치 방식을 정한다. flex/grid/block 등이 화면 구조를 결정한다. */
    place-items: center;
    min-width: 22px;
    height: 22px;
    border-radius: 999px;
    border: 2px solid white;
    background: var(--red);
    color: white;
    font-size: 12px;
    font-weight: 900;
    line-height: 1;
}

.city-tabs .active, .info-tab.active { background: var(--blue); color: white; }
.city-tabs .locked {
/* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
    background: linear-gradient(135deg, #6b7280, #374151);
    color: #e5e7eb;
}
.city-tabs .locked span { display: block; color: #d1d5db; font-size: 11px; }

.layout {
/* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
    display: grid;
    /* 해설: 요소의 배치 방식을 정한다. flex/grid/block 등이 화면 구조를 결정한다. */
    grid-template-columns: minmax(0, 1fr) 380px;
    /* 해설: 레이아웃 정렬과 크기 배분을 설정한다. */
    gap: 16px;
}

.city-panel { overflow: hidden; }

.stock-layout {
/* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
    display: grid;
    /* 해설: 요소의 배치 방식을 정한다. flex/grid/block 등이 화면 구조를 결정한다. */
    grid-template-columns: minmax(0, 1fr) 320px;
    /* 해설: 레이아웃 정렬과 크기 배분을 설정한다. */
    gap: 16px;
    align-items: start;
}

.stock-panel {
/* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
    padding: 0;
    min-width: 0;
}

.stock-record-panel {
/* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
    position: sticky;
    /* 해설: 요소의 위치 기준을 정한다. fixed/absolute/relative 등이 겹침과 배치에 영향을 준다. */
    top: 174px;
    padding: 18px;
    max-height: calc(100vh - 196px);
    overflow-y: auto;
    display: grid;
    /* 해설: 요소의 배치 방식을 정한다. flex/grid/block 등이 화면 구조를 결정한다. */
    gap: 14px;
    align-content: start;
}

.stock-record-panel h2 {
/* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
    margin: 0 0 10px;
    font-size: 18px;
}

.stock-holding-summary,
.stock-side-records {
/* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
    min-width: 0;
}

.stock-holding-summary dl {
/* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
    display: grid;
    /* 해설: 요소의 배치 방식을 정한다. flex/grid/block 등이 화면 구조를 결정한다. */
    gap: 8px;
    margin: 0;
}

.stock-holding-summary dl div {
/* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
    display: flex;
    /* 해설: 요소의 배치 방식을 정한다. flex/grid/block 등이 화면 구조를 결정한다. */
    justify-content: space-between;
    gap: 10px;
    padding: 8px 0;
    border-bottom: 1px solid #edf2f7;
}

.stock-holding-summary dt {
/* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
    color: var(--muted);
    font-size: 12px;
}

.stock-holding-summary dd {
/* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
    margin: 0;
    font-weight: 800;
    text-align: right;
    overflow-wrap: anywhere;
}

.stock-news-status {
/* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
    display: inline-flex;
    /* 해설: 요소의 배치 방식을 정한다. flex/grid/block 등이 화면 구조를 결정한다. */
    margin-top: 8px;
    padding: 5px 9px;
    border: 1px solid var(--line);
    border-radius: 999px;
    background: #f7fafc;
    color: var(--muted);
    font-size: 12px;
    font-weight: 800;
}

.stock-news-status.up {
/* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
    border-color: rgba(226, 91, 75, 0.35);
    background: #fff1ef;
    color: #c84232;
}

.stock-news-status.down {
/* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
    border-color: rgba(31, 122, 224, 0.35);
    background: #eef6ff;
    color: #1f65bf;
}

.stock-app {
/* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
    display: grid;
    /* 해설: 요소의 배치 방식을 정한다. flex/grid/block 등이 화면 구조를 결정한다. */
    grid-template-columns: 320px minmax(0, 1fr);
    /* 해설: 레이아웃 정렬과 크기 배분을 설정한다. */
    gap: 16px;
    padding: 0 18px 18px;
}

.stock-sidebar {
/* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
    display: grid;
    /* 해설: 요소의 배치 방식을 정한다. flex/grid/block 등이 화면 구조를 결정한다. */
    grid-template-rows: auto auto minmax(0, 1fr);
    /* 해설: 레이아웃 정렬과 크기 배분을 설정한다. */
    align-content: start;
    gap: 10px;
    min-width: 0;
    height: 100%;
    max-height: 794px;
    overflow: hidden;
}

.stock-filter-tabs {
/* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
    display: grid;
    /* 해설: 요소의 배치 방식을 정한다. flex/grid/block 등이 화면 구조를 결정한다. */
    grid-template-columns: 1fr 1fr;
    /* 해설: 레이아웃 정렬과 크기 배분을 설정한다. */
    gap: 6px;
}

.stock-filter-tabs button {
/* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
    min-height: 36px;
    border: 1px solid var(--line);
    background: #f7fafc;
    color: var(--muted);
    box-shadow: none;
}

.stock-filter-tabs button.active {
/* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
    border-color: var(--blue);
    background: var(--blue);
    color: white;
}

.stock-empty-message {
/* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
    margin: 0;
    padding: 14px;
    border: 1px dashed var(--line);
    border-radius: 10px;
    color: var(--muted);
    text-align: center;
}

.stock-company-list {
/* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
    display: grid;
    /* 해설: 요소의 배치 방식을 정한다. flex/grid/block 등이 화면 구조를 결정한다. */
    align-content: start;
    gap: 8px;
    max-height: none;
    min-height: 0;
    overflow-y: auto;
    padding-right: 4px;
}

.stock-company-button {
/* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
    display: grid;
    /* 해설: 요소의 배치 방식을 정한다. flex/grid/block 등이 화면 구조를 결정한다. */
    grid-template-columns: minmax(0, 1fr) auto;
    /* 해설: 레이아웃 정렬과 크기 배분을 설정한다. */
    gap: 4px 8px;
    min-height: 104px;
    min-width: 0;
    overflow: hidden;
    border: 1px solid var(--line);
    background: #fbfdff;
    color: var(--ink);
    text-align: left;
    box-shadow: none;
}

.stock-company-button[hidden] {
/* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
    display: none;
    /* 해설: 요소의 배치 방식을 정한다. flex/grid/block 등이 화면 구조를 결정한다. */
}

.stock-company-button.selected {
/* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
    border-color: var(--blue);
    background: #eef6ff;
    box-shadow: 0 0 0 3px rgba(47, 128, 237, 0.12);
}

.stock-company-button span,
.stock-company-button small {
/* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
    font-size: 12px;
}

.stock-company-button small {
/* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
    grid-column: 1 / -1;
    /* 해설: 레이아웃 정렬과 크기 배분을 설정한다. */
    display: flex;
    /* 해설: 요소의 배치 방식을 정한다. flex/grid/block 등이 화면 구조를 결정한다. */
    justify-content: space-between;
    gap: 8px;
    min-width: 0;
}

.stock-company-button small b {
/* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
    min-width: 0;
    color: var(--ink);
    text-align: right;
    overflow-wrap: anywhere;
}

.stock-company-button strong {
/* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
    grid-column: 1 / -1;
    /* 해설: 레이아웃 정렬과 크기 배분을 설정한다. */
    color: var(--ink);
    min-width: 0;
    overflow-wrap: anywhere;
}

.stock-company-button em {
/* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
    font-style: normal;
    font-size: 12px;
    font-weight: 800;
    min-width: 0;
    max-width: 92px;
    text-align: right;
    white-space: nowrap;
    overflow-wrap: anywhere;
}

.up { color: #e25b4b; }
.down { color: #1f7ae0; }
.flat { color: var(--muted); }

.stock-detail-list {
/* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
    min-width: 0;
}

.stock-detail {
/* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
    display: none;
    /* 해설: 요소의 배치 방식을 정한다. flex/grid/block 등이 화면 구조를 결정한다. */
    min-height: 620px;
    border: 1px solid var(--line);
    border-radius: 12px;
    background: #fbfdff;
    padding: 18px;
}

.stock-detail.active {
/* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
    display: grid;
    /* 해설: 요소의 배치 방식을 정한다. flex/grid/block 등이 화면 구조를 결정한다. */
    align-content: start;
    gap: 14px;
}

.stock-detail-head {
/* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
    display: flex;
    /* 해설: 요소의 배치 방식을 정한다. flex/grid/block 등이 화면 구조를 결정한다. */
    align-items: start;
    justify-content: space-between;
    gap: 16px;
}

.stock-price {
/* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
    color: var(--ink);
    font-size: 36px;
    line-height: 1.1;
    overflow-wrap: anywhere;
}

.stock-chart-shell {
/* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
    position: relative;
    /* 해설: 요소의 위치 기준을 정한다. fixed/absolute/relative 등이 겹침과 배치에 영향을 준다. */
    min-height: 280px;
    overflow: hidden;
    border: 1px solid #d7e4ee;
    border-radius: 12px;
    background:
        linear-gradient(180deg, rgba(255,255,255,0.92), rgba(247,251,255,0.96)),
        linear-gradient(90deg, rgba(47,128,237,0.08) 1px, transparent 1px),
        linear-gradient(180deg, rgba(47,128,237,0.08) 1px, transparent 1px);
    background-size: auto, 64px 100%, 100% 48px;
}

.stock-candle-chart {
/* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
    display: block;
    /* 해설: 요소의 배치 방식을 정한다. flex/grid/block 등이 화면 구조를 결정한다. */
    width: 100%;
    height: 300px;
}

.chart-grid-line {
/* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
    stroke: #d9e4ea;
    stroke-width: 1;
}

.candle-wick {
/* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
    stroke-width: 1.4;
}

.candle-body {
/* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
    rx: 1;
}

.candle-up .candle-wick,
.candle-up .candle-body {
/* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
    stroke: #e25b4b;
    fill: #e25b4b;
}

.candle-down .candle-wick,
.candle-down .candle-body {
/* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
    stroke: #1f7ae0;
    fill: #1f7ae0;
}

.current-price-line {
/* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
    stroke: #2f80ed;
    stroke-width: 1;
    stroke-dasharray: 4 4;
}

.chart-price-label,
.chart-axis-label {
/* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
    fill: #102033;
    stroke: rgba(255, 255, 255, 0.9);
    stroke-width: 4px;
    paint-order: stroke;
    font-size: 12px;
    font-weight: 800;
    dominant-baseline: middle;
}

.stock-summary-grid {
/* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
    display: grid;
    /* 해설: 요소의 배치 방식을 정한다. flex/grid/block 등이 화면 구조를 결정한다. */
    grid-template-columns: repeat(2, minmax(0, 1fr));
    /* 해설: 레이아웃 정렬과 크기 배분을 설정한다. */
    gap: 0 18px;
}

.stock-summary-grid dd {
/* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
    overflow-wrap: anywhere;
}

.stock-trade-panel,
.stock-bottom-grid {
/* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
    display: grid;
    /* 해설: 요소의 배치 방식을 정한다. flex/grid/block 등이 화면 구조를 결정한다. */
    gap: 12px;
}

.stock-trade-panel {
/* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
    grid-template-columns: repeat(2, minmax(0, 1fr));
    /* 해설: 레이아웃 정렬과 크기 배분을 설정한다. */
    align-items: stretch;
}

.stock-trade-panel form,
.stock-exchange-panel form {
/* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
    display: grid;
    /* 해설: 요소의 배치 방식을 정한다. flex/grid/block 등이 화면 구조를 결정한다. */
    grid-template-columns: minmax(0, 1fr);
    /* 해설: 레이아웃 정렬과 크기 배분을 설정한다. */
    gap: 8px;
}

.stock-trade-panel form {
/* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
    align-self: end;
    padding: 10px;
    border: 1px solid #dfeaf2;
    border-radius: 10px;
    background: #f9fcff;
}

.stock-trade-panel label,
.stock-exchange-panel label {
/* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
    display: grid;
    /* 해설: 요소의 배치 방식을 정한다. flex/grid/block 등이 화면 구조를 결정한다. */
    gap: 4px;
    color: var(--muted);
    font-size: 12px;
}

.stock-trade-panel button,
.stock-exchange-panel button {
/* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
    width: 100%;
}

.stock-trade-panel button {
/* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
    min-height: 36px;
    padding: 0 12px;
}

.stock-quick-buttons {
/* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
    display: grid;
    /* 해설: 요소의 배치 방식을 정한다. flex/grid/block 등이 화면 구조를 결정한다. */
    grid-template-columns: repeat(5, minmax(0, 1fr));
    /* 해설: 레이아웃 정렬과 크기 배분을 설정한다. */
    gap: 6px;
}

.stock-quick-buttons button {
/* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
    min-height: 30px;
    padding: 0 6px;
    font-size: 12px;
    box-shadow: none;
}

.stock-order-preview {
/* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
    min-height: 30px;
    padding: 6px 8px;
    border: 1px solid #dfeaf2;
    border-radius: 8px;
    background: #f6f9fc;
    color: var(--muted);
    font-size: 11px;
    font-weight: 700;
    line-height: 1.25;
    overflow-wrap: anywhere;
}

.stock-exchange-panel {
/* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
    display: grid;
    /* 해설: 요소의 배치 방식을 정한다. flex/grid/block 등이 화면 구조를 결정한다. */
    gap: 8px;
}

.stock-exchange-panel form {
/* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
    grid-template-columns: minmax(0, 1fr) 120px;
    /* 해설: 레이아웃 정렬과 크기 배분을 설정한다. */
    align-items: end;
}

.stock-exchange-quick-buttons {
/* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
    display: grid;
    /* 해설: 요소의 배치 방식을 정한다. flex/grid/block 등이 화면 구조를 결정한다. */
    grid-template-columns: repeat(4, minmax(0, 1fr));
    /* 해설: 레이아웃 정렬과 크기 배분을 설정한다. */
    gap: 6px;
}

.stock-exchange-quick-buttons button {
/* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
    min-height: 30px;
    padding: 0 6px;
    font-size: 12px;
    box-shadow: none;
}

.stock-exchange-panel form .stock-exchange-quick-buttons,
.stock-exchange-panel form .stock-order-preview {
/* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
    grid-column: 1 / -1;
    /* 해설: 레이아웃 정렬과 크기 배분을 설정한다. */
}

.stock-exchange-panel form button {
/* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
    min-height: 40px;
}

.stock-bottom-grid {
/* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
    grid-template-columns: 320px minmax(0, 1fr);
    /* 해설: 레이아웃 정렬과 크기 배분을 설정한다. */
    align-items: stretch;
    padding: 0 18px 18px;
}

.stock-exchange-panel,
.stock-history-panel {
/* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
    border: 1px solid var(--line);
    border-radius: 12px;
    background: #fbfdff;
    padding: 14px;
}

.stock-history-panel {
/* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
    display: grid;
    /* 해설: 요소의 배치 방식을 정한다. flex/grid/block 등이 화면 구조를 결정한다. */
    grid-template-rows: auto minmax(0, 1fr) auto;
    /* 해설: 레이아웃 정렬과 크기 배분을 설정한다. */
    height: 100%;
    min-height: 500px;
    max-height: 500px;
    overflow: hidden;
}

.stock-exchange-panel p {
/* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
    margin: 4px 0 10px;
}

.stock-trade-history {
/* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
    display: grid;
    /* 해설: 요소의 배치 방식을 정한다. flex/grid/block 등이 화면 구조를 결정한다. */
    align-content: start;
    gap: 8px;
    min-height: 0;
    max-height: none;
    overflow-y: auto;
    margin: 10px 0 0;
    padding-left: 18px;
}

.stock-trade-history li {
/* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
    line-height: 1.35;
}

.stock-trade-history small {
/* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
    display: block;
    /* 해설: 요소의 배치 방식을 정한다. flex/grid/block 등이 화면 구조를 결정한다. */
}

.stock-history-head {
/* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
    display: grid;
    /* 해설: 요소의 배치 방식을 정한다. flex/grid/block 등이 화면 구조를 결정한다. */
    grid-template-columns: auto minmax(180px, 260px) minmax(160px, 220px);
    /* 해설: 레이아웃 정렬과 크기 배분을 설정한다. */
    gap: 10px;
    align-items: end;
}

.stock-history-head h3 {
/* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
    margin: 0;
}

.stock-history-head label {
/* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
    display: grid;
    /* 해설: 요소의 배치 방식을 정한다. flex/grid/block 등이 화면 구조를 결정한다. */
    gap: 4px;
    color: var(--muted);
    font-size: 12px;
}

.stock-history-filter-tabs {
/* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
    display: grid;
    /* 해설: 요소의 배치 방식을 정한다. flex/grid/block 등이 화면 구조를 결정한다. */
    grid-template-columns: repeat(3, minmax(0, 1fr));
    /* 해설: 레이아웃 정렬과 크기 배분을 설정한다. */
    gap: 6px;
}

.stock-history-filter-tabs button {
/* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
    min-height: 34px;
    padding: 0 10px;
    border: 1px solid var(--line);
    background: #f7fafc;
    color: var(--muted);
    box-shadow: none;
}

.stock-history-filter-tabs button.active {
/* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
    border-color: var(--blue);
    background: var(--blue);
    color: white;
}

.stock-trade-empty-message {
/* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
    margin: 12px 0 0;
    color: var(--muted);
    font-size: 13px;
}

.section-head {
/* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
    display: flex;
    /* 해설: 요소의 배치 방식을 정한다. flex/grid/block 등이 화면 구조를 결정한다. */
    align-items: center;
    justify-content: space-between;
    gap: 16px;
    padding: 18px;
}

.section-head.compact { padding: 0; }
.quick-actions, .buy-actions { display: flex; gap: 8px; }
.quick-actions form { margin: 0; }
.quick-actions button:first-child { background: var(--green); }
.quick-actions button:last-child { background: var(--yellow); color: #503b00; }

.skyline {
/* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
    position: relative;
    /* 해설: 요소의 위치 기준을 정한다. fixed/absolute/relative 등이 겹침과 배치에 영향을 준다. */
    display: grid;
    /* 해설: 요소의 배치 방식을 정한다. flex/grid/block 등이 화면 구조를 결정한다. */
    grid-template-columns: repeat(4, minmax(130px, 1fr));
    /* 해설: 레이아웃 정렬과 크기 배분을 설정한다. */
    gap: 14px;
    min-height: 520px;
    padding: 80px 18px 22px;
    background:
        linear-gradient(180deg, rgba(255,255,255,0.12), rgba(255,255,255,0.28)),
        linear-gradient(180deg, var(--sky), #dff7d6 82%, #8dd470 82%);
    background-position: center bottom;
    background-size: cover;
    background-repeat: no-repeat;
    overflow: hidden;
}

.city-bg-cheongju {
/* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
    background-image:
        linear-gradient(180deg, rgba(255,255,255,0.1), rgba(255,255,255,0.22)),
        url("/assets/cities/cheongju.svg");
}

.city-bg-sejong {
/* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
    background-image:
        linear-gradient(180deg, rgba(255,255,255,0.1), rgba(255,255,255,0.22)),
        url("/assets/cities/sejong.svg");
}

.city-bg-daejeon {
/* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
    background-image:
        linear-gradient(180deg, rgba(255,255,255,0.1), rgba(255,255,255,0.22)),
        url("/assets/cities/daejeon.svg");
}

.city-bg-busan {
/* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
    background-image:
        linear-gradient(180deg, rgba(255,255,255,0.1), rgba(255,255,255,0.2)),
        url("/assets/cities/busan.svg");
}

.city-bg-incheon {
/* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
    background-image:
        linear-gradient(180deg, rgba(255,255,255,0.1), rgba(255,255,255,0.22)),
        url("/assets/cities/incheon.svg");
}

.city-bg-seoul {
/* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
    background-image:
        linear-gradient(180deg, rgba(255,255,255,0.1), rgba(255,255,255,0.22)),
        url("/assets/cities/seoul.svg");
}

.sun {
/* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
    position: absolute;
    /* 해설: 요소의 위치 기준을 정한다. fixed/absolute/relative 등이 겹침과 배치에 영향을 준다. */
    top: 30px;
    right: 70px;
    width: 64px;
    height: 64px;
    border-radius: 50%;
    background: #ffd166;
    box-shadow: 0 0 34px rgba(255, 209, 102, 0.75);
    z-index: 2;
}

.cloud {
/* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
    position: absolute;
    /* 해설: 요소의 위치 기준을 정한다. fixed/absolute/relative 등이 겹침과 배치에 영향을 준다. */
    width: 110px;
    height: 34px;
    border-radius: 999px;
    background: rgba(255, 255, 255, 0.82);
    z-index: 2;
}

.cloud::before, .cloud::after {
/* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
    content: "";
    position: absolute;
    /* 해설: 요소의 위치 기준을 정한다. fixed/absolute/relative 등이 겹침과 배치에 영향을 준다. */
    border-radius: 50%;
    background: inherit;
}

.cloud::before { width: 44px; height: 44px; left: 18px; top: -20px; }
.cloud::after { width: 54px; height: 54px; left: 48px; top: -28px; }
.cloud-a { top: 42px; left: 90px; }
.cloud-b { top: 88px; left: 360px; transform: scale(0.75); }

.skyline .building-slot {
/* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
    z-index: 3;
}

.building-slot {
/* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
    position: relative;
    /* 해설: 요소의 위치 기준을 정한다. fixed/absolute/relative 등이 겹침과 배치에 영향을 준다. */
    display: grid;
    /* 해설: 요소의 배치 방식을 정한다. flex/grid/block 등이 화면 구조를 결정한다. */
    align-content: end;
    justify-items: center;
    min-height: 190px;
    border: 2px solid rgba(255, 255, 255, 0.8);
    border-radius: 14px;
    background: rgba(255, 255, 255, 0.5);
    padding: 12px;
    text-align: center;
    color: var(--ink);
    box-shadow: none;
}

article.building-slot {
/* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
    cursor: pointer;
}

.building-slot.selected {
/* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
    border-color: var(--blue);
    box-shadow: 0 0 0 4px rgba(47, 128, 237, 0.15);
}

.building-slot strong { margin-top: 10px; }

.image-placeholder {
/* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
    display: grid;
    /* 해설: 요소의 배치 방식을 정한다. flex/grid/block 등이 화면 구조를 결정한다. */
    place-items: center;
    border: 2px dashed #90a7b9;
    color: #597083;
    background: rgba(255, 255, 255, 0.62);
    font-weight: 800;
}

.building-image {
/* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
    display: block;
    /* 해설: 요소의 배치 방식을 정한다. flex/grid/block 등이 화면 구조를 결정한다. */
    width: min(100%, 150px);
    aspect-ratio: 16 / 9;
    border: 1px solid var(--line);
    border-radius: 10px;
    background: rgba(255, 255, 255, 0.76);
    object-fit: contain;
}

.market-image {
/* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
    display: block;
    /* 해설: 요소의 배치 방식을 정한다. flex/grid/block 등이 화면 구조를 결정한다. */
    width: 100%;
    aspect-ratio: 16 / 9;
    border: 1px solid var(--line);
    border-radius: 12px;
    background: #fff;
    object-fit: contain;
    font-size: 14px;
}

.empty-plus {
/* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
    display: grid;
    /* 해설: 요소의 배치 방식을 정한다. flex/grid/block 등이 화면 구조를 결정한다. */
    place-items: center;
    width: 78px;
    height: 78px;
    border-radius: 18px;
    border: 2px dashed #8aa2b4;
    color: #5e788b;
    font-size: 44px;
    background: rgba(255, 255, 255, 0.52);
}

.repair {
/* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
    position: absolute;
    /* 해설: 요소의 위치 기준을 정한다. fixed/absolute/relative 등이 겹침과 배치에 영향을 준다. */
    top: 10px;
    right: 10px;
    min-height: 30px;
    border-radius: 8px;
    background: var(--red);
    color: white;
    padding: 7px 10px;
    font-size: 12px;
    font-weight: 800;
}

.protection {
/* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
    position: absolute;
    /* 해설: 요소의 위치 기준을 정한다. fixed/absolute/relative 등이 겹침과 배치에 영향을 준다. */
    top: 10px;
    left: 10px;
    min-height: 30px;
    border-radius: 8px;
    background: #253858;
    color: white;
    padding: 7px 10px;
    font-size: 12px;
    font-weight: 800;
}

.sale-lock-badge {
/* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
    position: absolute;
    /* 해설: 요소의 위치 기준을 정한다. fixed/absolute/relative 등이 겹침과 배치에 영향을 준다. */
    top: 10px;
    left: 10px;
    min-height: 30px;
    border-radius: 8px;
    background: #1f2937;
    color: white;
    padding: 7px 10px;
    font-size: 12px;
    font-weight: 800;
}

button.repair {
/* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
    border: 0;
    cursor: pointer;
}

.secretary-focus { display: grid; gap: 16px; min-height: 100%; }
.secretary-focus .section-head h2 {
/* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
    font-size: 19px;
    line-height: 1.18;
}
.secretary-focus .section-head p {
/* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
    font-size: 11px;
    line-height: 1.2;
    white-space: nowrap;
}
.secretary-focus .secretary-info h3 {
/* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
    font-size: 15px;
    line-height: 1.22;
}
.secretary-focus .secretary-info > p {
/* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
    font-size: 11px;
    line-height: 1.2;
    white-space: nowrap;
}
.secretary-visual {
/* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
    display: grid;
    /* 해설: 요소의 배치 방식을 정한다. flex/grid/block 등이 화면 구조를 결정한다. */
    place-items: center;
    min-height: 440px;
    border-radius: 18px;
    background: linear-gradient(180deg, rgba(255,255,255,0.12), rgba(255,255,255,0.84)), linear-gradient(135deg, #ffe4c7, #d8ecff);
    padding: 16px;
}

.secretary-image, .story-image {
/* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
    display: grid;
    /* 해설: 요소의 배치 방식을 정한다. flex/grid/block 등이 화면 구조를 결정한다. */
    place-items: center;
    border: 2px dashed #b58b78;
    border-radius: 16px;
    color: #8b5d4a;
    font-weight: 900;
    background: rgba(255,255,255,0.42);
}

.secretary-empty {
/* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
    display: grid;
    /* 해설: 요소의 배치 방식을 정한다. flex/grid/block 등이 화면 구조를 결정한다. */
    place-items: center;
    width: min(310px, 100%);
    height: 440px;
    min-height: 0;
    border: 1px dashed #90a7b9;
    border-radius: 16px;
    color: var(--muted);
    background: rgba(255,255,255,0.58);
    font-weight: 800;
}

img.story-image,
img.secretary-thumb,
img.secretary-image,
img.event-image {
/* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
    display: block;
    /* 해설: 요소의 배치 방식을 정한다. flex/grid/block 등이 화면 구조를 결정한다. */
    object-fit: cover;
    border-style: solid;
}

img.story-image {
/* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
    border: 0;
}

img.secretary-thumb,
img.secretary-image,
img.event-image {
/* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
    border: 1px solid var(--line);
}

.event-image-empty {
/* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
    border: 1px dashed var(--line);
    background: rgba(255, 255, 255, 0.62);
}

.secretary-image {
/* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
    width: min(310px, 100%);
    height: 440px;
    min-height: 0;
    border-radius: 16px;
    font-size: 24px;
    object-fit: cover;
    object-position: top center;
}
.secretary-info { display: grid; gap: 8px; }
.secretary-ability-actions {
/* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
    display: grid;
    /* 해설: 요소의 배치 방식을 정한다. flex/grid/block 등이 화면 구조를 결정한다. */
    grid-template-columns: minmax(0, 1fr) 92px;
    /* 해설: 레이아웃 정렬과 크기 배분을 설정한다. */
    gap: 8px;
    align-items: center;
}
.secretary-ability-actions .badge,
.secretary-ability-actions button {
/* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
    width: 100%;
}
.ability-modal {
/* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
    width: min(520px, calc(100vw - 32px));
    border-radius: 8px;
    background: #fff;
    box-shadow: var(--shadow);
    padding: 18px;
}
.ability-list {
/* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
    display: grid;
    /* 해설: 요소의 배치 방식을 정한다. flex/grid/block 등이 화면 구조를 결정한다. */
    gap: 8px;
    margin: 14px 0 0;
    padding: 0;
    list-style: none;
}
.ability-list li {
/* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
    border: 1px solid var(--line);
    border-radius: 8px;
    background: #f8fbfd;
    padding: 10px 12px;
    font-weight: 700;
    color: var(--ink);
}
.secretary-exp-grid {
/* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
    display: grid;
    /* 해설: 요소의 배치 방식을 정한다. flex/grid/block 등이 화면 구조를 결정한다. */
    grid-template-columns: 1fr 1fr;
    /* 해설: 레이아웃 정렬과 크기 배분을 설정한다. */
    gap: 8px;
}
.secretary-exp-card {
/* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
    display: grid;
    /* 해설: 요소의 배치 방식을 정한다. flex/grid/block 등이 화면 구조를 결정한다. */
    gap: 6px;
    min-width: 0;
    border: 1px solid var(--line);
    border-radius: 8px;
    background: rgba(255,255,255,0.7);
    padding: 8px;
}
.secretary-exp-card > div:first-child {
/* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
    display: flex;
    /* 해설: 요소의 배치 방식을 정한다. flex/grid/block 등이 화면 구조를 결정한다. */
    justify-content: space-between;
    gap: 8px;
}
.secretary-exp-card span,
.secretary-exp-card small {
/* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
    font-size: 12px;
}
.secretary-exp-card strong {
/* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
    color: var(--ink);
    font-size: 12px;
}
.skill-meter { overflow: hidden; height: 12px; border-radius: 999px; background: #edf2f7; }
.skill-meter span { display: block; height: 100%; background: linear-gradient(90deg, #68d391, #2f80ed); }
.secretary-exp-card.affinity .skill-meter span { background: linear-gradient(90deg, #f6ad55, #ed64a6); }
.secretary-panel-actions {
/* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
    display: grid;
    /* 해설: 요소의 배치 방식을 정한다. flex/grid/block 등이 화면 구조를 결정한다. */
    grid-template-columns: minmax(0, 1fr) 76px;
    /* 해설: 레이아웃 정렬과 크기 배분을 설정한다. */
    gap: 8px;
}
.secretary-panel-actions button {
/* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
    width: 100%;
}

.panel { padding: 18px; }

.badge {
/* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
    display: inline-flex;
    /* 해설: 요소의 배치 방식을 정한다. flex/grid/block 등이 화면 구조를 결정한다. */
    align-items: center;
    min-height: 26px;
    border-radius: 999px;
    padding: 0 10px;
    background: #eef2f7;
    color: var(--ink);
    font-weight: 700;
    font-size: 12px;
}

.badge.good { background: #e4f8ec; color: #197a45; }
.badge.danger { background: #ffe9e6; color: #b93d32; }

.market-panel, .info-grid, .info-page { margin-top: 16px; }
.market-title-row {
/* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
    display: flex;
    /* 해설: 요소의 배치 방식을 정한다. flex/grid/block 등이 화면 구조를 결정한다. */
    align-items: center;
    flex-wrap: wrap;
    /* 해설: 레이아웃 정렬과 크기 배분을 설정한다. */
    gap: 8px;
}
.market-news-badge {
/* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
    font-size: 12px;
    white-space: nowrap;
}
.refresh-progress-card {
/* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
    min-width: 190px;
    border: 1px solid var(--line);
    border-radius: 8px;
    background: #fbfdff;
    padding: 10px 12px;
}

.refresh-progress-card span,
.refresh-progress-card strong,
.refresh-progress-card small {
/* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
    display: block;
    /* 해설: 요소의 배치 방식을 정한다. flex/grid/block 등이 화면 구조를 결정한다. */
}

.refresh-progress-card > span {
/* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
    color: var(--muted);
    font-size: 12px;
    font-weight: 800;
}

.refresh-progress-card strong {
/* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
    margin-top: 3px;
    color: var(--ink);
    font-size: 14px;
}

.refresh-progress-card small {
/* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
    margin-top: 4px;
    color: var(--muted);
    font-size: 11px;
    text-align: right;
}

.refresh-progress {
/* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
    overflow: hidden;
    width: 100%;
    height: 8px;
    margin-top: 8px;
    border-radius: 999px;
    background: #dbe8f0;
}

.refresh-progress span {
/* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
    height: 100%;
    border-radius: inherit;
    background: linear-gradient(90deg, var(--yellow), var(--green));
}

.market-list {
/* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
    display: grid;
    /* 해설: 요소의 배치 방식을 정한다. flex/grid/block 등이 화면 구조를 결정한다. */
    grid-template-columns: repeat(4, minmax(0, 1fr));
    /* 해설: 레이아웃 정렬과 크기 배분을 설정한다. */
    gap: 12px;
}

.market-card {
/* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
    position: relative;
    /* 해설: 요소의 위치 기준을 정한다. fixed/absolute/relative 등이 겹침과 배치에 영향을 준다. */
    overflow: hidden;
    display: grid;
    /* 해설: 요소의 배치 방식을 정한다. flex/grid/block 등이 화면 구조를 결정한다. */
    gap: 8px;
    align-content: start;
    min-width: 0;
    border: 1px solid var(--line);
    border-radius: 12px;
    background: #fbfdff;
    padding: 14px;
}

.market-card strong { font-size: clamp(16px, 1.35vw, 20px); }
.market-card h3, .market-card p, .market-card small { min-width: 0; overflow-wrap: anywhere; }
.market-card.low { border-color: #bfe9ce; }
.market-card.high { border-color: #ffd1cc; }
.market-card.locked-offer, .market-card.waiting-offer {
/* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
    border-color: #4b5563;
}

.market-card.locked-offer > :not(.market-lock-overlay),
.market-card.waiting-offer > :not(.market-lock-overlay) {
/* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
    filter: brightness(0.56);
}

.market-lock-overlay {
/* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
    position: absolute;
    /* 해설: 요소의 위치 기준을 정한다. fixed/absolute/relative 등이 겹침과 배치에 영향을 준다. */
    inset: 0;
    z-index: 5;
    display: grid;
    /* 해설: 요소의 배치 방식을 정한다. flex/grid/block 등이 화면 구조를 결정한다. */
    place-items: center;
    align-content: center;
    gap: 8px;
    background: rgba(0, 0, 0, 0.62);
    color: white;
    text-align: center;
    padding: 16px;
}

.market-lock-overlay.waiting {
/* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
    background: rgba(0, 0, 0, 0.48);
}

.market-lock-overlay strong,
.market-lock-overlay span {
/* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
    color: white;
}

.lock-icon {
/* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
    position: relative;
    /* 해설: 요소의 위치 기준을 정한다. fixed/absolute/relative 등이 겹침과 배치에 영향을 준다. */
    display: block;
    /* 해설: 요소의 배치 방식을 정한다. flex/grid/block 등이 화면 구조를 결정한다. */
    width: 34px;
    height: 28px;
    border-radius: 7px;
    background: #f4b740;
}

.lock-icon::before {
/* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
    content: "";
    position: absolute;
    /* 해설: 요소의 위치 기준을 정한다. fixed/absolute/relative 등이 겹침과 배치에 영향을 준다. */
    left: 50%;
    top: -18px;
    width: 20px;
    height: 24px;
    border: 5px solid #f4b740;
    border-bottom: 0;
    border-radius: 14px 14px 0 0;
    transform: translateX(-50%);
}
.buy-actions { display: grid; grid-template-columns: 1fr 1fr; }
.buy-actions form { margin: 0; }
.buy-actions button { width: 100%; }

.info-grid {
/* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
    display: grid;
    /* 해설: 요소의 배치 방식을 정한다. flex/grid/block 등이 화면 구조를 결정한다. */
    grid-template-columns: 1fr 1fr 0.8fr;
    /* 해설: 레이아웃 정렬과 크기 배분을 설정한다. */
    gap: 16px;
}

.building-detail {
/* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
    display: none;
    /* 해설: 요소의 배치 방식을 정한다. flex/grid/block 등이 화면 구조를 결정한다. */
}

.building-detail.active {
/* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
    display: block;
    /* 해설: 요소의 배치 방식을 정한다. flex/grid/block 등이 화면 구조를 결정한다. */
}

dl { display: grid; gap: 10px; margin: 14px 0; }
dl div, .loan-box div {
/* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
    display: flex;
    /* 해설: 요소의 배치 방식을 정한다. flex/grid/block 등이 화면 구조를 결정한다. */
    justify-content: space-between;
    gap: 10px;
    border-bottom: 1px solid #eef3f7;
    padding-bottom: 8px;
}

dd { margin: 0; font-weight: 800; }
.help-dot {
/* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
    border: 1px solid #c7d0da;
    display: inline-grid;
    /* 해설: 요소의 배치 방식을 정한다. flex/grid/block 등이 화면 구조를 결정한다. */
    place-items: center;
    width: 12px;
    height: 12px;
    min-width: 12px;
    min-height: 12px;
    margin-left: 3px;
    border-radius: 50%;
    background: #f8fafc;
    color: #64748b;
    font-size: 8px;
    font-weight: 900;
    cursor: pointer;
    line-height: 1;
    vertical-align: 2px;
    padding: 0;
    box-shadow: none;
    appearance: none;
}

.help-dot:hover {
/* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
    border-color: #94a3b8;
    background: #eef2f7;
    color: #334155;
}

.help-dot.active {
/* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
    background: #64748b;
    border-color: #64748b;
    color: white;
}

.help-detail {
/* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
    position: absolute;
    /* 해설: 요소의 위치 기준을 정한다. fixed/absolute/relative 등이 겹침과 배치에 영향을 준다. */
    z-index: 25;
    width: min(240px, calc(100% - 24px));
    border: 1px solid #d7e4ee;
    border-radius: 8px;
    background: #f7fbff;
    padding: 8px 10px;
    box-shadow: 0 12px 28px rgba(31, 42, 55, 0.18);
}

.help-detail p {
/* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
    font-size: 12px;
    line-height: 1.45;
}

.help-popover[hidden] {
/* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
    display: none !important;
    /* 해설: 요소의 배치 방식을 정한다. flex/grid/block 등이 화면 구조를 결정한다. */
}

.help-detail p + p {
/* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
    margin-top: 4px;
}

.dl-detail {
/* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
    display: block;
    /* 해설: 요소의 배치 방식을 정한다. flex/grid/block 등이 화면 구조를 결정한다. */
}

.secretary-status-row {
/* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
    display: flex;
    /* 해설: 요소의 배치 방식을 정한다. flex/grid/block 등이 화면 구조를 결정한다. */
    flex-wrap: wrap;
    /* 해설: 레이아웃 정렬과 크기 배분을 설정한다. */
    gap: 6px;
    align-items: center;
}
.price-split-row {
/* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
    align-items: flex-start;
}

.price-split {
/* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
    display: grid;
    /* 해설: 요소의 배치 방식을 정한다. flex/grid/block 등이 화면 구조를 결정한다. */
    grid-template-columns: 1fr 1fr;
    /* 해설: 레이아웃 정렬과 크기 배분을 설정한다. */
    gap: 8px;
    min-width: min(260px, 100%);
}

.price-split span {
/* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
    display: grid;
    /* 해설: 요소의 배치 방식을 정한다. flex/grid/block 등이 화면 구조를 결정한다. */
    gap: 2px;
    min-width: 0;
    text-align: right;
}

.price-split b {
/* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
    color: var(--muted);
    font-size: 12px;
}

.price-split em {
/* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
    color: var(--ink);
    font-style: normal;
    overflow-wrap: anywhere;
}
.button-row { display: grid; grid-template-columns: 1fr 1fr; gap: 8px; }
.button-row form { margin: 0; }
.button-row button { width: 100%; }
.wide { width: 100%; }
.record-panel-head {
/* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
    display: flex;
    /* 해설: 요소의 배치 방식을 정한다. flex/grid/block 등이 화면 구조를 결정한다. */
    align-items: center;
    justify-content: space-between;
    gap: 10px;
}
.record-panel-head button {
/* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
    min-height: 30px;
    padding: 0 10px;
    font-size: 12px;
    white-space: nowrap;
}
.recent-record-panel:not(.docked) {
/* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
    position: fixed;
    /* 해설: 요소의 위치 기준을 정한다. fixed/absolute/relative 등이 겹침과 배치에 영향을 준다. */
    top: 174px;
    right: 20px;
    z-index: 24;
    width: min(320px, calc(100vw - 32px));
    max-height: calc(100vh - 196px);
    overflow: hidden;
}
.recent-record-panel:not(.docked) .record-list {
/* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
    max-height: calc(100vh - 286px);
}
.event-list { display: grid; gap: 10px; margin: 14px 0 0; padding-left: 18px; }
.event-list b { color: var(--blue); }
.record-list li { line-height: 1.45; }
.record-list {
/* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
    max-height: 320px;
    overflow-y: auto;
    padding-right: 8px;
}
.record-list small { display: block; color: var(--muted); }
.money-in { color: #197a45; font-weight: 800; }
.money-out { color: #b93d32; font-weight: 800; }
.test-panel {
/* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
    display: grid;
    /* 해설: 요소의 배치 방식을 정한다. flex/grid/block 등이 화면 구조를 결정한다. */
    grid-column: 1 / -1;
    /* 해설: 레이아웃 정렬과 크기 배분을 설정한다. */
    order: 20;
    gap: 12px;
}
.test-panel form { margin: 0; }
.test-controls { display: grid; gap: 10px; }
.test-controls label { display: grid; gap: 4px; font-weight: 800; }
.test-controls input { width: 100%; }
.test-divider {
/* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
    width: 100%;
    height: 1px;
    background: var(--line);
    margin: 8px 0;
}
.reputation-shop-panel {
/* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
    display: grid;
    /* 해설: 요소의 배치 방식을 정한다. flex/grid/block 등이 화면 구조를 결정한다. */
    grid-column: 1 / -1;
    /* 해설: 레이아웃 정렬과 크기 배분을 설정한다. */
    gap: 12px;
}
.shop-split-grid {
/* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
    display: grid;
    /* 해설: 요소의 배치 방식을 정한다. flex/grid/block 등이 화면 구조를 결정한다. */
    grid-column: 1 / -1;
    /* 해설: 레이아웃 정렬과 크기 배분을 설정한다. */
    grid-template-columns: 1fr 1fr;
    /* 해설: 레이아웃 정렬과 크기 배분을 설정한다. */
    gap: 16px;
}
.split-shop-panel {
/* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
    grid-column: auto;
    /* 해설: 레이아웃 정렬과 크기 배분을 설정한다. */
    min-width: 0;
}
.donation-actions {
/* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
    display: grid;
    /* 해설: 요소의 배치 방식을 정한다. flex/grid/block 등이 화면 구조를 결정한다. */
    grid-template-columns: 1fr;
    /* 해설: 레이아웃 정렬과 크기 배분을 설정한다. */
    gap: 8px;
}
.donation-card {
/* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
    display: grid;
    /* 해설: 요소의 배치 방식을 정한다. flex/grid/block 등이 화면 구조를 결정한다. */
    grid-template-columns: 52px 80px minmax(110px, 1fr) minmax(100px, 0.8fr) 76px;
    /* 해설: 레이아웃 정렬과 크기 배분을 설정한다. */
    gap: 10px;
    align-items: center;
    margin: 0;
    min-width: 0;
    min-height: 68px;
    border: 1px solid var(--line);
    border-radius: 8px;
    background: #fbfdff;
    padding: 8px;
}
.donation-card strong,
.donation-card span,
.luxury-row strong,
.luxury-row span,
.gift-row strong,
.gift-row span {
/* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
    min-width: 0;
    overflow: hidden;
    white-space: nowrap;
    text-overflow: ellipsis;
}
.donation-card button,
.luxury-row button,
.gift-row button {
/* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
    min-height: 32px;
    padding: 0 10px;
    width: 100%;
}
.luxury-list,
.gift-list {
/* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
    display: grid;
    /* 해설: 요소의 배치 방식을 정한다. flex/grid/block 등이 화면 구조를 결정한다. */
    gap: 8px;
    max-height: 330px;
    overflow-y: auto;
    overflow-x: hidden;
    padding-right: 4px;
}
.luxury-row,
.gift-row {
/* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
    display: grid;
    /* 해설: 요소의 배치 방식을 정한다. flex/grid/block 등이 화면 구조를 결정한다. */
    grid-template-columns: 52px minmax(130px, 1.2fr) minmax(94px, 0.8fr) minmax(72px, 0.65fr) 58px minmax(54px, 0.55fr) 68px;
    /* 해설: 레이아웃 정렬과 크기 배분을 설정한다. */
    gap: 10px;
    align-items: center;
    min-height: 68px;
    border: 1px solid var(--line);
    border-radius: 8px;
    background: #fbfdff;
    padding: 8px;
}
.luxury-row form {
/* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
    margin: 0;
}
.luxury-row {
/* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
    grid-template-columns: 52px minmax(170px, 1.4fr) minmax(120px, 0.8fr) minmax(110px, 0.7fr) 76px;
    /* 해설: 레이아웃 정렬과 크기 배분을 설정한다. */
}
.gift-row {
/* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
    margin: 0;
}
.quantity-input {
/* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
    width: 100%;
    min-width: 0;
    min-height: 32px;
    padding: 0 6px;
    text-align: center;
}
.gift-owned {
/* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
    font-size: 12px;
    font-weight: 800;
}
.shop-image {
/* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
    display: block;
    /* 해설: 요소의 배치 방식을 정한다. flex/grid/block 등이 화면 구조를 결정한다. */
    width: 52px;
    height: 52px;
    border-radius: 8px;
    border: 1px solid #d7e4ee;
    object-fit: cover;
    background: #f7fbff;
}
.donation-image { background: #f0fff4; }
.luxury-image { background: #fff7ec; }
.gift-image { background: #f4f8fb; }
.secretary-modal {
/* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
    width: min(1120px, calc(100vw - 36px));
    max-height: calc(100vh - 48px);
    overflow-y: auto;
    border-radius: 18px;
    background: white;
    padding: 20px;
}
.secretary-modal .secretary-list {
/* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
    margin-top: 16px;
}
.secretary-modal .secretary-card {
/* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
    position: relative;
    /* 해설: 요소의 위치 기준을 정한다. fixed/absolute/relative 등이 겹침과 배치에 영향을 준다. */
    overflow: visible;
}
.secretary-modal .secretary-card dl dt,
.secretary-modal .secretary-card dl dd,
.secretary-modal .secretary-action-row select,
.secretary-modal .secretary-action-row button,
.secretary-modal .secretary-assignment button {
/* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
    font-size: 12px;
}
.secretary-modal .secretary-card dl dd {
/* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
    white-space: nowrap;
}
.secretary-modal .secretary-card dl {
/* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
    gap: 6px;
}
.secretary-card.locked-secretary > :not(.secretary-lock-overlay) {
/* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
    filter: brightness(0.58);
}
.secretary-lock-overlay {
/* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
    position: absolute;
    /* 해설: 요소의 위치 기준을 정한다. fixed/absolute/relative 등이 겹침과 배치에 영향을 준다. */
    inset: 0;
    z-index: 4;
    display: grid;
    /* 해설: 요소의 배치 방식을 정한다. flex/grid/block 등이 화면 구조를 결정한다. */
    place-items: center;
    align-content: center;
    gap: 8px;
    background: rgba(0, 0, 0, 0.54);
    color: white;
    text-align: center;
}
.secretary-lock-overlay strong,
.secretary-lock-overlay span {
/* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
    color: white;
}
.secretary-assignment {
/* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
    display: grid;
    /* 해설: 요소의 배치 방식을 정한다. flex/grid/block 등이 화면 구조를 결정한다. */
    gap: 8px;
}
.secretary-assignment form {
/* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
    display: grid;
    /* 해설: 요소의 배치 방식을 정한다. flex/grid/block 등이 화면 구조를 결정한다. */
    grid-template-columns: minmax(0, 1fr) auto;
    /* 해설: 레이아웃 정렬과 크기 배분을 설정한다. */
    gap: 8px;
    margin: 0;
}
.secretary-assignment form + form {
/* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
    grid-template-columns: 1fr;
    /* 해설: 레이아웃 정렬과 크기 배분을 설정한다. */
}
.secretary-action-row {
/* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
    display: grid;
    /* 해설: 요소의 배치 방식을 정한다. flex/grid/block 등이 화면 구조를 결정한다. */
    grid-template-columns: minmax(0, 1fr) 86px;
    /* 해설: 레이아웃 정렬과 크기 배분을 설정한다. */
    gap: 8px;
}
.secretary-action-row form {
/* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
    display: grid;
    /* 해설: 요소의 배치 방식을 정한다. flex/grid/block 등이 화면 구조를 결정한다. */
    grid-template-columns: minmax(0, 1fr) 58px;
    /* 해설: 레이아웃 정렬과 크기 배분을 설정한다. */
    gap: 8px;
    margin: 0;
}
.gift-select-popover {
/* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
    position: fixed;
    /* 해설: 요소의 위치 기준을 정한다. fixed/absolute/relative 등이 겹침과 배치에 영향을 준다. */
    z-index: 80;
    left: 50%;
    top: 50%;
    display: grid;
    /* 해설: 요소의 배치 방식을 정한다. flex/grid/block 등이 화면 구조를 결정한다. */
    width: min(720px, calc(100vw - 32px));
    max-height: calc(100vh - 72px);
    overflow-y: auto;
    gap: 10px;
    border: 1px solid var(--line);
    border-radius: 14px;
    background: white;
    padding: 16px;
    box-shadow: 0 22px 70px rgba(31, 42, 55, 0.26);
    transform: translate(-50%, -50%);
}
.gift-select-popover[hidden] {
/* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
    display: none !important;
    /* 해설: 요소의 배치 방식을 정한다. flex/grid/block 등이 화면 구조를 결정한다. */
}
.shop-confirm-backdrop {
/* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
    position: fixed;
    /* 해설: 요소의 위치 기준을 정한다. fixed/absolute/relative 등이 겹침과 배치에 영향을 준다. */
    inset: 0;
    z-index: 120;
    display: grid;
    /* 해설: 요소의 배치 방식을 정한다. flex/grid/block 등이 화면 구조를 결정한다. */
    place-items: center;
    background: rgba(31, 42, 55, 0.48);
    padding: 18px;
}
.shop-confirm-backdrop[hidden] {
/* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
    display: none !important;
    /* 해설: 요소의 배치 방식을 정한다. flex/grid/block 등이 화면 구조를 결정한다. */
}
.shop-confirm-modal {
/* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
    display: grid;
    /* 해설: 요소의 배치 방식을 정한다. flex/grid/block 등이 화면 구조를 결정한다. */
    gap: 12px;
    width: min(420px, 100%);
    border: 1px solid var(--line);
    border-radius: 14px;
    background: white;
    padding: 18px;
    box-shadow: 0 22px 70px rgba(31, 42, 55, 0.28);
}
.shop-confirm-modal p {
/* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
    color: var(--ink);
    line-height: 1.5;
}
.confirm-actions {
/* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
    display: grid;
    /* 해설: 요소의 배치 방식을 정한다. flex/grid/block 등이 화면 구조를 결정한다. */
    grid-template-columns: 1fr 1fr;
    /* 해설: 레이아웃 정렬과 크기 배분을 설정한다. */
    gap: 8px;
}
.confirm-actions button {
/* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
    width: 100%;
}
.gift-give-row {
/* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
    display: grid;
    /* 해설: 요소의 배치 방식을 정한다. flex/grid/block 등이 화면 구조를 결정한다. */
    grid-template-columns: 52px minmax(120px, 1fr) 72px 76px 58px 68px;
    /* 해설: 레이아웃 정렬과 크기 배분을 설정한다. */
    gap: 8px;
    align-items: center;
    margin: 0;
    border: 1px solid var(--line);
    border-radius: 8px;
    background: #fbfdff;
    padding: 8px;
}
.gift-give-row strong,
.gift-give-row span {
/* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
    min-width: 0;
    overflow: hidden;
    white-space: nowrap;
    text-overflow: ellipsis;
}
.secretary-modal select {
/* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
    width: 100%;
    min-height: 40px;
    border: 1px solid var(--line);
    border-radius: 8px;
    padding: 0 10px;
    font: inherit;
}
.loan-box { display: grid; gap: 10px; margin: 16px 0; }
.loan-list {
/* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
    display: grid;
    /* 해설: 요소의 배치 방식을 정한다. flex/grid/block 등이 화면 구조를 결정한다. */
    gap: 8px;
    margin-top: 12px;
}

.loan-row {
/* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
    display: grid;
    /* 해설: 요소의 배치 방식을 정한다. flex/grid/block 등이 화면 구조를 결정한다. */
    grid-template-columns: 1fr 1fr 0.8fr 1fr;
    /* 해설: 레이아웃 정렬과 크기 배분을 설정한다. */
    gap: 8px;
    align-items: center;
    border: 1px solid var(--line);
    border-radius: 10px;
    background: #fbfdff;
    padding: 10px;
}

.loan-row span {
/* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
    overflow-wrap: anywhere;
}
.logout-form { margin-top: 16px; text-align: right; }

.info-page {
/* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
    display: grid;
    /* 해설: 요소의 배치 방식을 정한다. flex/grid/block 등이 화면 구조를 결정한다. */
    gap: 16px;
}

.secretary-list {
/* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
    display: grid;
    /* 해설: 요소의 배치 방식을 정한다. flex/grid/block 등이 화면 구조를 결정한다. */
    grid-template-columns: repeat(2, minmax(0, 1fr));
    /* 해설: 레이아웃 정렬과 크기 배분을 설정한다. */
    gap: 14px;
}

.secretary-card {
/* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
    position: relative;
    /* 해설: 요소의 위치 기준을 정한다. fixed/absolute/relative 등이 겹침과 배치에 영향을 준다. */
    display: grid;
    /* 해설: 요소의 배치 방식을 정한다. flex/grid/block 등이 화면 구조를 결정한다. */
    grid-template-columns: 170px minmax(0, 1fr);
    /* 해설: 레이아웃 정렬과 크기 배분을 설정한다. */
    gap: 14px;
    min-width: 0;
    border: 1px solid var(--line);
    border-radius: 12px;
    background: #fbfdff;
    padding: 14px;
}

.image-open-button {
/* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
    display: block;
    /* 해설: 요소의 배치 방식을 정한다. flex/grid/block 등이 화면 구조를 결정한다. */
    min-height: 0;
    border: 0;
    background: transparent;
    padding: 0;
    box-shadow: none;
    cursor: zoom-in;
}

.secretary-thumb {
/* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
    display: block;
    /* 해설: 요소의 배치 방식을 정한다. flex/grid/block 등이 화면 구조를 결정한다. */
    width: 100%;
    min-height: 220px;
    height: 280px;
    border: 2px dashed #b58b78;
    border-radius: 14px;
    background: linear-gradient(135deg, #fff7ec, #eef8ff);
    color: #8b5d4a;
    font-weight: 900;
    text-align: center;
    object-fit: cover;
    object-position: top center;
}

.image-modal-backdrop {
/* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
    position: fixed;
    /* 해설: 요소의 위치 기준을 정한다. fixed/absolute/relative 등이 겹침과 배치에 영향을 준다. */
    inset: 0;
    z-index: 70;
    display: grid;
    /* 해설: 요소의 배치 방식을 정한다. flex/grid/block 등이 화면 구조를 결정한다. */
    place-items: center;
    padding: 20px;
    background: rgba(15, 23, 42, 0.58);
}

.image-modal-backdrop[hidden] {
/* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
    display: none;
    /* 해설: 요소의 배치 방식을 정한다. flex/grid/block 등이 화면 구조를 결정한다. */
}

.image-modal {
/* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
    display: grid;
    /* 해설: 요소의 배치 방식을 정한다. flex/grid/block 등이 화면 구조를 결정한다. */
    gap: 14px;
    width: min(960px, 100%);
    max-height: calc(100vh - 40px);
    border: 1px solid var(--line);
    border-radius: 16px;
    background: white;
    padding: 16px;
    box-shadow: 0 24px 70px rgba(15, 23, 42, 0.28);
}

.image-modal img {
/* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
    width: 100%;
    max-height: calc(100vh - 140px);
    object-fit: contain;
    border-radius: 12px;
    background: #f8fafc;
}

.table-wrap {
/* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
    overflow-x: auto;
}

table {
/* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
    width: 100%;
    min-width: 760px;
    border-collapse: collapse;
}

th, td {
/* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
    border-bottom: 1px solid #e7eef4;
    padding: 10px;
    text-align: left;
    vertical-align: top;
}

th {
/* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
    background: #f4f8fb;
    color: var(--ink);
    font-size: 13px;
}

td {
/* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
    color: var(--ink);
    overflow-wrap: anywhere;
}

.rule-grid {
/* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
    display: grid;
    /* 해설: 요소의 배치 방식을 정한다. flex/grid/block 등이 화면 구조를 결정한다. */
    grid-template-columns: repeat(4, minmax(0, 1fr));
    /* 해설: 레이아웃 정렬과 크기 배분을 설정한다. */
    gap: 12px;
    margin-top: 14px;
}

.rule-grid article {
/* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
    border: 1px solid var(--line);
    border-radius: 12px;
    background: #fbfdff;
    padding: 14px;
}

.rule-grid p {
/* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
    margin-top: 8px;
    line-height: 1.5;
}

.auth-page, .story-page {
/* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
    width: min(1360px, 100%);
    margin: 0 auto;
    padding: 36px 20px;
}

.auth-hero {
/* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
    display: grid;
    /* 해설: 요소의 배치 방식을 정한다. flex/grid/block 등이 화면 구조를 결정한다. */
    justify-items: center;
    gap: 8px;
    margin-bottom: 24px;
    text-align: center;
}

.auth-grid {
/* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
    display: grid;
    /* 해설: 요소의 배치 방식을 정한다. flex/grid/block 등이 화면 구조를 결정한다. */
    grid-template-columns: repeat(2, minmax(0, 1fr));
    /* 해설: 레이아웃 정렬과 크기 배분을 설정한다. */
    gap: 16px;
}

.auth-card {
/* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
    display: grid;
    /* 해설: 요소의 배치 방식을 정한다. flex/grid/block 등이 화면 구조를 결정한다. */
    gap: 14px;
    padding: 22px;
}

.auth-card label {
/* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
    display: grid;
    /* 해설: 요소의 배치 방식을 정한다. flex/grid/block 등이 화면 구조를 결정한다. */
    gap: 6px;
    color: var(--muted);
    font-weight: 800;
}

.story-head { margin-bottom: 18px; }
.story-grid {
/* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
    display: grid;
    /* 해설: 요소의 배치 방식을 정한다. flex/grid/block 등이 화면 구조를 결정한다. */
    grid-template-columns: 1fr;
    /* 해설: 레이아웃 정렬과 크기 배분을 설정한다. */
    gap: 20px;
}

.story-card {
/* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
    display: grid;
    /* 해설: 요소의 배치 방식을 정한다. flex/grid/block 등이 화면 구조를 결정한다. */
    grid-template-columns: 1fr;
    /* 해설: 레이아웃 정렬과 크기 배분을 설정한다. */
    gap: 12px;
    padding: 18px;
}

.story-image {
/* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
    width: 100%;
    height: auto;
    min-height: 0;
    aspect-ratio: auto;
    font-size: 32px;
    object-fit: contain;
    background: white;
}

.story-action {
/* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
    margin-top: 18px;
    text-align: center;
}

.story-action button {
/* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
    min-width: 260px;
    min-height: 48px;
    background: var(--green);
}

.toast {
/* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
    position: fixed;
    /* 해설: 요소의 위치 기준을 정한다. fixed/absolute/relative 등이 겹침과 배치에 영향을 준다. */
    left: 50%;
    bottom: 96px;
    z-index: 1000;
    transform: translate(-50%, 20px);
    opacity: 0;
    pointer-events: none;
    transition: 0.2s ease;
}

#toast.toast {
/* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
    display: grid;
    /* 해설: 요소의 배치 방식을 정한다. flex/grid/block 등이 화면 구조를 결정한다. */
    gap: 8px;
    width: min(560px, calc(100vw - 32px));
}

#flashToast.toast,
.toast-item {
/* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
    justify-self: center;
    padding: 12px 18px;
    border-radius: 999px;
    background: #1f2a37;
    color: white;
    font-weight: 800;
    box-shadow: var(--shadow);
}

.toast.show { transform: translate(-50%, 0); opacity: 1; }
.toast-item {
/* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
    max-width: 100%;
    opacity: 0;
    transform: translateY(8px);
    transition: 0.2s ease;
}
.toast-item.show {
/* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
    opacity: 1;
    transform: translateY(0);
}

.collapsible-panel {
/* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
    display: block;
    /* 해설: 요소의 배치 방식을 정한다. flex/grid/block 등이 화면 구조를 결정한다. */
}
.collapsible-panel > summary {
/* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
    cursor: pointer;
    color: var(--ink);
    font-size: 20px;
    font-weight: 900;
    list-style: none;
}
.collapsible-panel > summary::-webkit-details-marker {
/* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
    display: none;
    /* 해설: 요소의 배치 방식을 정한다. flex/grid/block 등이 화면 구조를 결정한다. */
}
.collapsible-panel > summary::after {
/* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
    content: "접기";
    float: right;
    min-height: 26px;
    border-radius: 999px;
    background: #eef2f7;
    color: var(--muted);
    padding: 4px 10px;
    font-size: 12px;
    font-weight: 800;
}
.collapsible-panel:not([open]) > summary::after {
/* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
    content: "펼치기";
}
.collapsible-panel > summary + * {
/* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
    margin-top: 10px;
}

.event-modal-backdrop {
/* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
    position: fixed;
    /* 해설: 요소의 위치 기준을 정한다. fixed/absolute/relative 등이 겹침과 배치에 영향을 준다. */
    inset: 0;
    z-index: 50;
    display: grid;
    /* 해설: 요소의 배치 방식을 정한다. flex/grid/block 등이 화면 구조를 결정한다. */
    place-items: center;
    padding: 22px;
    background: rgba(15, 23, 42, 0.48);
}

.event-modal {
/* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
    display: grid;
    /* 해설: 요소의 배치 방식을 정한다. flex/grid/block 등이 화면 구조를 결정한다. */
    grid-template-columns: minmax(320px, 0.9fr) minmax(300px, 1fr);
    /* 해설: 레이아웃 정렬과 크기 배분을 설정한다. */
    gap: 20px;
    width: min(980px, 100%);
    border-radius: 18px;
    border: 1px solid var(--line);
    background: rgba(255, 255, 255, 0.96);
    box-shadow: 0 24px 70px rgba(15, 23, 42, 0.28);
    padding: 20px;
}

.event-image {
/* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
    display: grid;
    /* 해설: 요소의 배치 방식을 정한다. flex/grid/block 등이 화면 구조를 결정한다. */
    place-items: center;
    width: 100%;
    min-height: 420px;
    max-height: 70vh;
    border: 2px dashed #90a7b9;
    border-radius: 16px;
    background: linear-gradient(135deg, #eef8ff, #fff6e5);
    color: #597083;
    font-size: 28px;
    font-weight: 900;
    object-fit: cover;
    object-position: center;
}

.event-copy {
/* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
    display: grid;
    /* 해설: 요소의 배치 방식을 정한다. flex/grid/block 등이 화면 구조를 결정한다. */
    align-content: center;
    gap: 14px;
}

.event-copy h2 {
/* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
    font-size: 28px;
}

.event-copy p {
/* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
    font-size: 17px;
    line-height: 1.7;
}

.event-copy form {
/* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
    margin-top: 6px;
}

.event-copy button {
/* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
    width: 100%;
    min-height: 48px;
}

.event-button-row {
/* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
    display: grid;
    /* 해설: 요소의 배치 방식을 정한다. flex/grid/block 등이 화면 구조를 결정한다. */
    gap: 10px;
}

.event-button-row.split {
/* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
    grid-template-columns: 1fr 1fr;
    /* 해설: 레이아웃 정렬과 크기 배분을 설정한다. */
}

.event-button-row form {
/* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
    margin: 0;
}

.event-button-row .secondary-button {
/* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
    border-color: #cbd5e1;
    background: #f8fafc;
    color: var(--ink);
}

.auction-modal {
/* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
    width: min(720px, calc(100vw - 32px));
    grid-template-columns: 1fr;
    /* 해설: 레이아웃 정렬과 크기 배분을 설정한다. */
}

.auction-image {
/* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
    width: 100%;
    max-height: 320px;
    object-fit: cover;
}

.auction-copy {
/* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
    text-align: center;
}

.auction-bid-grid {
/* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
    display: grid;
    /* 해설: 요소의 배치 방식을 정한다. flex/grid/block 등이 화면 구조를 결정한다. */
    grid-template-columns: repeat(3, minmax(0, 1fr));
    /* 해설: 레이아웃 정렬과 크기 배분을 설정한다. */
    gap: 12px;
}

.auction-bid-grid form,
.auction-cancel-form {
/* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
    margin: 0;
}

.auction-bid-grid button {
/* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
    display: grid;
    /* 해설: 요소의 배치 방식을 정한다. flex/grid/block 등이 화면 구조를 결정한다. */
    gap: 4px;
    min-height: 78px;
    border-color: #6b46c1;
    background: linear-gradient(180deg, #8f62cf, #6b46a2);
}

.auction-bid-grid button:disabled {
/* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
    border-color: #cbd5e1;
    background: #e2e8f0;
    color: #64748b;
}

.auction-bid-grid button span,
.auction-bid-grid button strong,
.auction-bid-grid button small {
/* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
    color: inherit;
}

.auction-cancel-button {
/* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
    width: min(280px, 100%);
    margin: 8px auto 0;
}

.auction-timer,
.auction-timer strong {
/* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
    color: var(--ink);
}

.auction-timer {
/* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
    margin-top: 10px;
    border: 1px solid #d7e4ee;
    border-radius: 8px;
    background: #f7fbff;
    padding: 10px 12px;
    font-weight: 800;
}

.auction-result-modal .auction-image {
/* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
    object-fit: contain;
    background: #fbf7ef;
}

@media (max-width: 1700px) {
/* 해설: 반응형 조건이다. 화면 크기나 환경에 따라 다른 스타일을 적용한다. */
    .recent-record-panel:not(.docked) {
    /* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
        position: static;
        /* 해설: 요소의 위치 기준을 정한다. fixed/absolute/relative 등이 겹침과 배치에 영향을 준다. */
        width: auto;
        max-height: none;
    }
    .recent-record-panel:not(.docked) .record-list {
    /* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
        max-height: 320px;
    }
}

@media (max-width: 1180px) {
/* 해설: 반응형 조건이다. 화면 크기나 환경에 따라 다른 스타일을 적용한다. */
    .topbar, .layout, .info-grid { grid-template-columns: 1fr; }
    .stock-layout, .stock-app, .stock-bottom-grid { grid-template-columns: 1fr; }
    .stock-detail-head { display: grid; }
    .stock-record-panel { position: static; max-height: none; }
    .status-grid { grid-template-columns: repeat(3, minmax(0, 1fr)); }
    .secretary-visual { min-height: 280px; }
    .market-list { grid-template-columns: repeat(2, minmax(0, 1fr)); }
    .secretary-list, .rule-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); }
    .story-card, .event-modal { grid-template-columns: 1fr; }
}

@media (max-width: 760px) {
/* 해설: 반응형 조건이다. 화면 크기나 환경에 따라 다른 스타일을 적용한다. */
    .app { padding: 12px; }
    .section-head, .quick-actions { flex-direction: column; align-items: stretch; }
    .status-grid, .skyline, .market-list, .auth-grid, .story-grid, .secretary-list, .rule-grid, .secretary-card { grid-template-columns: 1fr; }
    .skyline { padding-top: 120px; }
    .story-image, .event-image { min-height: 240px; }
    .loan-row { grid-template-columns: 1fr; }
    .donation-card {
    /* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
        grid-template-columns: 44px 48px minmax(72px, 1fr) minmax(68px, 0.8fr) 60px;
        /* 해설: 레이아웃 정렬과 크기 배분을 설정한다. */
        gap: 6px;
    }
    .luxury-row {
    /* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
        grid-template-columns: 52px minmax(92px, 1fr) minmax(88px, 0.8fr) minmax(78px, 0.7fr) 68px;
        /* 해설: 레이아웃 정렬과 크기 배분을 설정한다. */
        gap: 8px;
    }
    .shop-split-grid { grid-template-columns: 1fr; }
    .gift-row {
    /* 해설: CSS 선택자 블록이다. 선택된 HTML 요소에 아래 스타일을 적용한다. */
        grid-template-columns: 52px minmax(92px, 1fr) minmax(78px, 0.8fr) minmax(54px, 0.6fr) 52px 54px 64px;
        /* 해설: 레이아웃 정렬과 크기 배분을 설정한다. */
        gap: 8px;
    }
    .gift-give-row { grid-template-columns: 52px minmax(92px, 1fr) 58px 64px 52px 64px; }
    .stock-summary-grid { grid-template-columns: 1fr; }
    .stock-trade-panel { grid-template-columns: 1fr; }
    .stock-history-head { grid-template-columns: 1fr; }
    .stock-history-panel { min-height: 420px; max-height: 420px; }
    .stock-trade-panel form,
    .stock-exchange-panel form { grid-template-columns: 1fr; }
    .auction-bid-grid { grid-template-columns: 1fr; }
    .auction-image { max-height: 260px; }
}
```