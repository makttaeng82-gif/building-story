# DailyGameOrchestrator 설명

파일: `src/main/java/com/game/buildingstory/service/time/DailyGameOrchestrator.java`

## 역할

게임 날짜가 하루 증가한 뒤 실행할 콘텐츠를 순서대로 호출한다. 날짜 증가는 하지 않는다.

```java
this.processors = processors.stream()
        .sorted(Comparator.comparingInt(DailyGameProcessor::order))
        .toList();
```

Spring은 `DailyGameProcessor` 구현체를 목록으로 주입한다. `order()` 값으로 정렬하기 때문에
Bean 발견 순서가 바뀌어도 게임 처리 순서는 바뀌지 않는다.

```java
for (DailyGameProcessor processor : processors) {
    DailyProcessResult result = processor.process(context);
```

각 프로세서에 같은 플레이어와 `deferCityEvents` 값을 전달한다. 프로세서는 날짜를 직접 증가시키지 않는다.

```java
if (result.shouldStop()) {
    return result.terminalSignal();
}
```

`EVENT:`나 `AUCTION:`은 화면 전환이 필요한 종료 신호다. 신호가 나오면 뒤 프로세서를 실행하지 않는다.
일반 정산 문구는 종료 신호가 없을 때만 ` · `로 합쳐 최종 반환한다.

## 확장 방법

같은 게임 시간을 쓰는 콘텐츠는 다음 규약만 구현한다.

```java
public class DailyCompanyProcessor implements DailyGameProcessor {
    public int order() {
        return 350;
    }

    public DailyProcessResult process(DailyProcessContext context) {
        // context.player()의 이미 증가한 날짜를 기준으로 기업 상태를 갱신한다.
        return DailyProcessResult.continueWithoutNotice();
    }
}
```

`@Component`로 등록하면 Spring이 목록에 자동으로 포함한다. `player.advanceDay()`는 추가하지 않는다.
