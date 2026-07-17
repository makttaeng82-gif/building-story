package com.game.buildingstory.service.time;

/** 게임 날짜가 하루 증가한 뒤 실행되는 처리 단계의 공통 규약이다. */
public interface DailyGameProcessor {
    /** 숫자가 작은 단계부터 실행된다. */
    int order();

    /** 현재 하루에 필요한 작업을 수행하고 계속 진행할지 결정한다. */
    DailyProcessResult process(DailyProcessContext context);
}
