package com.game.buildingstory.service.time;

/**
 * 한 처리 단계의 결과를 일반 알림과 화면 전환 신호로 구분한다.
 *
 * <p>notice는 정산 결과처럼 다음 단계와 합칠 수 있는 문장이다. terminalSignal은
 * EVENT 또는 AUCTION 화면을 즉시 열어야 하는 신호이므로 이후 단계를 실행하지 않는다.</p>
 */
public record DailyProcessResult(String notice, String terminalSignal) {
    public static DailyProcessResult continueWith(String notice) {
        return new DailyProcessResult(notice == null ? "" : notice, "");
    }

    public static DailyProcessResult continueWithoutNotice() {
        return continueWith("");
    }

    public static DailyProcessResult stop(String terminalSignal) {
        return new DailyProcessResult("", terminalSignal);
    }

    public boolean shouldStop() {
        return terminalSignal != null && !terminalSignal.isBlank();
    }
}
