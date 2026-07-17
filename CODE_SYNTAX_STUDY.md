# 코드 문법 공부 정리

이 프로젝트에서 실제로 사용한 문법과 패턴을 정리한 파일이다.

## Java 기본

### 클래스

```java
public class GameService {
}
```

- `class`: 객체 설계도.
- `public`: 다른 패키지에서도 접근 가능.
- 서비스, 컨트롤러, 엔티티 대부분이 클래스다.

### 필드

```java
private final PlayerRepository playerRepository;
```

- `private`: 클래스 내부에서만 접근.
- `final`: 생성자에서 한 번 대입 후 변경 불가.
- Repository, Service 의존성은 보통 `final`로 둔다.

### 생성자 주입

```java
public GameService(PlayerRepository playerRepository) {
    this.playerRepository = playerRepository;
}
```

- Spring이 필요한 객체를 생성자에 넣어준다.
- `this.playerRepository`는 현재 객체의 필드.
- 오른쪽 `playerRepository`는 매개변수.

### 메서드

```java
public String sideJob(long playerId) {
    return "부업 수익 10,000원 획득";
}
```

- `public`: 외부 호출 가능.
- `String`: 반환 타입.
- `sideJob`: 메서드 이름.
- `(long playerId)`: 입력값.

### 조건문

```java
if (player.isPaused()) {
    return "";
}
```

- 조건이 참이면 블록 실행.
- `return`은 즉시 메서드 종료.

### Optional

```java
Optional<GameEvent> activeEvent = activeEvent(player);
if (activeEvent.isPresent()) {
    return "EVENT:" + activeEvent.get().getId();
}
```

- `Optional<T>`: 값이 있을 수도, 없을 수도 있음을 표현.
- `isPresent()`: 값 존재 여부.
- `get()`: 값 꺼내기.

### record

```java
public record StockSpec(
        String key,
        String industry,
        String name
) {
}
```

- 불변 데이터 묶음.
- 생성자, getter, `equals`, `hashCode`가 자동 생성된다.
- `stock.key()`처럼 접근한다.

### enum

```java
public enum AuctionStatus {
    ACTIVE,
    RESULT,
    COMPLETED
}
```

- 정해진 값 중 하나만 허용.
- 상태값 표현에 적합.

## Spring Boot

### 애플리케이션 시작

```java
@SpringBootApplication
public class BuildingStoryApplication {
    public static void main(String[] args) {
        SpringApplication.run(BuildingStoryApplication.class, args);
    }
}
```

- `@SpringBootApplication`: Spring Boot 시작 클래스 표시.
- `main`: Java 프로그램 시작점.

### Service

```java
@Service
public class StockService {
}
```

- `@Service`: 비즈니스 로직 담당 Bean.
- Spring이 객체를 생성하고 관리한다.

### Controller

```java
@Controller
public class GameController {
}
```

- `@Controller`: HTTP 요청을 받는 클래스.
- URL과 서비스 호출을 연결한다.

### URL 매핑

```java
@GetMapping("/main")
public String main() {
    return "main";
}
```

- `@GetMapping`: GET 요청 처리.
- 반환 문자열 `"main"`은 `templates/main.html`을 의미한다.

```java
@PostMapping("/tick")
@ResponseBody
public Map<String, String> tick() {
    return Map.of("notice", "");
}
```

- `@PostMapping`: POST 요청 처리.
- `@ResponseBody`: HTML이 아니라 JSON 형태로 응답.

### 요청 파라미터

```java
public String main(@RequestParam(defaultValue = "city") String view) {
}
```

- URL의 `?view=stocks` 값을 받는다.
- 값이 없으면 `"city"` 사용.

### 경로 변수

```java
@PostMapping("/stocks/{stockKey}/buy")
public String buyStock(@PathVariable String stockKey) {
}
```

- `/stocks/bytecore/buy`에서 `bytecore`를 `stockKey`로 받는다.

### 세션

```java
Long playerId = (Long) session.getAttribute(SessionKeys.PLAYER_ID);
```

- 세션은 브라우저별 서버 저장 공간.
- 로그인한 플레이어 ID 저장에 사용.

### 트랜잭션

```java
@Transactional
public String buyStock(...) {
}
```

- 메서드 실행 중 DB 변경을 하나의 작업 단위로 묶는다.
- 중간에 예외가 나면 롤백된다.

```java
@Transactional(readOnly = true)
public List<StockSpec> stocks() {
}
```

- 읽기 전용 트랜잭션.
- DB 변경 의도가 없음을 표시.

## JPA 엔티티

### Entity

```java
@Entity
public class Player {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
}
```

- `@Entity`: DB 테이블과 연결되는 클래스.
- `@Id`: 기본키.
- `@GeneratedValue`: DB가 ID 자동 생성.

### Column

```java
@Column(nullable = false, unique = true, length = 40)
private String username;
```

- DB 컬럼 제약 조건.
- `nullable = false`: null 불가.
- `unique = true`: 중복 불가.
- `length = 40`: 최대 길이.

### ManyToOne

```java
@ManyToOne(fetch = FetchType.LAZY, optional = false)
private Player player;
```

- 여러 엔티티가 한 Player에 연결됨.
- 예: 여러 건물, 여러 주식 기록이 한 플레이어 소유.
- `LAZY`: 실제 필요할 때 Player 조회.

## Spring Data JPA Repository

### 기본 Repository

```java
public interface PlayerRepository extends JpaRepository<Player, Long> {
}
```

- `JpaRepository<Entity, IdType>`.
- 기본 CRUD 메서드 자동 제공.

### 메서드 이름 쿼리

```java
Optional<Player> findByUsername(String username);
```

- 메서드 이름을 분석해서 SQL 생성.
- `findByUsername`: username 컬럼으로 조회.

```java
List<OwnedBuilding> findByPlayerAndCityOrderById(Player player, String city);
```

- `player`와 `city` 조건으로 조회.
- `OrderById`: id 순 정렬.

## Thymeleaf

### 변수 출력

```html
<span th:text="${player.cash}">0</span>
```

- `${player.cash}` 값을 HTML 텍스트로 출력.
- 태그 안의 `0`은 서버 렌더 전 기본 표시.

### 조건부 표시

```html
<span th:if="${stockMarketStatus.hasActiveNews()}">뉴스</span>
```

- 조건이 참일 때만 태그 렌더링.

### 반복

```html
<button th:each="quote : ${stockQuotes}">
</button>
```

- `stockQuotes` 리스트를 순회.
- 각 항목은 `quote`.

### 속성 설정

```html
<section th:attr="data-player-cash=${player.cash}">
</section>
```

- HTML 속성을 서버 값으로 설정.
- JS에서 `dataset.playerCash`로 읽을 수 있다.

### URL 생성

```html
<form th:action="@{/stocks/{key}/buy(key=${stock.key()})}">
</form>
```

- `{key}` 자리에 `stock.key()` 값 삽입.

### Fragment

```html
<section th:fragment="stockPanel">
</section>
```

- 재사용 가능한 HTML 조각.
- `main.html`에서 끼워 넣을 수 있다.

## JavaScript

### DOM 선택

```js
const buttons = document.querySelectorAll(".stock-company-button");
```

- CSS 선택자로 HTML 요소를 찾는다.
- `querySelectorAll`: 여러 개 반환.

### 이벤트 등록

```js
button.addEventListener("click", () => {
    selectStock(button.dataset.stockKey);
});
```

- 버튼 클릭 시 함수 실행.
- `dataset.stockKey`는 `data-stock-key` 속성 값.

### class 토글

```js
item.classList.toggle("selected", item.dataset.stockKey === selectedKey);
```

- 조건이 참이면 class 추가.
- 거짓이면 class 제거.

### fetch

```js
const response = await fetch(`/tick?view=${view}`, { method: "POST" });
const result = await response.json();
```

- 서버 API 호출.
- `await`: 응답이 올 때까지 기다림.
- `.json()`: JSON 응답을 JS 객체로 변환.

### localStorage / sessionStorage

```js
window.localStorage.setItem("key", value);
window.sessionStorage.getItem("key");
```

- `localStorage`: 브라우저를 닫아도 유지.
- `sessionStorage`: 탭 세션 동안 유지.
- 선택 종목, 스크롤 위치, 입력값 복원에 사용.

### 중복 요청 방지

```js
if (navigating || ticking || document.body.classList.contains("game-paused")) {
    return;
}
ticking = true;
```

- 자동 시간 진행 중 `/tick`이 중복 호출되지 않게 막는다.
- 없으면 하루가 2번 지날 수 있다.

## CSS

### CSS 변수

```css
:root {
    --blue: #2f80ed;
}
```

- 공통 색상 값을 변수로 저장.
- `var(--blue)`로 사용.

### Grid

```css
.stock-app {
    display: grid;
    grid-template-columns: 320px minmax(0, 1fr);
}
```

- 2열 레이아웃.
- 왼쪽 320px, 오른쪽 남은 공간.

### Flex

```css
.section-head {
    display: flex;
    justify-content: space-between;
}
```

- 한 줄 배치에 적합.
- 양 끝 정렬 가능.

### 상태 클래스

```css
.up { color: #e25b4b; }
.down { color: #1f7ae0; }
```

- 값 상승/하락을 색상으로 표시.
- HTML에서 class를 바꿔 상태 표현.

## 테스트

### SpringBootTest

```java
@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:building-story-test;DB_CLOSE_DELAY=-1"
})
class BuildingStoryApplicationTests {
}
```

- 실제 Spring Bean과 테스트 DB를 띄워 통합 테스트.
- H2 인메모리 DB 사용.

### BeforeEach

```java
@BeforeEach
void cleanDatabase() {
    playerRepository.deleteAll();
}
```

- 각 테스트 실행 전 호출.
- 테스트끼리 데이터가 섞이지 않게 DB 정리.

### AssertJ

```java
assertThat(player.getCash()).isEqualTo(0L);
```

- 테스트 검증 문법.
- 실제 값이 기대 값과 같은지 확인.

```java
assertThat(gameService.activeEvent(player)).isPresent();
```

- Optional에 값이 있는지 확인.

## 이 프로젝트 핵심 패턴

### Controller -> Service -> Repository -> Entity

```text
브라우저 요청
  -> Controller
  -> Service
  -> Repository
  -> Entity 상태 변경
  -> DB 저장
```

- Controller: 요청/응답.
- Service: 게임 규칙.
- Repository: DB 접근.
- Entity: 저장되는 상태.

### 화면 표시용 View record

```text
Entity + 계산
  -> StockQuoteView
  -> Thymeleaf 출력
```

- 엔티티를 그대로 화면에 넘기지 않는다.
- 화면에 필요한 텍스트와 좌표를 서비스에서 미리 계산한다.

### 시간 진행

```text
app.js setInterval
  -> POST /tick
  -> GameService.tick
  -> Player.advanceDay
  -> 정산/이벤트/주식 갱신
  -> 페이지 reload
```

- 날짜 증가는 서버에서만 한다.
- 프론트는 `/tick` 호출만 한다.

