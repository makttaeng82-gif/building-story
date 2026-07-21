package com.game.buildingstory.service;

import com.game.buildingstory.domain.Player;

/** 게임 달력을 상장기업의 회계연도와 분기로 변환한다. */
public final class FiscalQuarter {
    private FiscalQuarter() {
    }

    public static int currentPeriodIndex(Player player) {
        int completedYears = Math.max(0, player.getElapsedDays() - 1) / 365;
        return completedYears * 4 + (player.getMonth() - 1) / 3;
    }

    public static boolean isQuarterOpeningDay(Player player) {
        return player.getDay() == 1 && (player.getMonth() == 1
                || player.getMonth() == 4
                || player.getMonth() == 7
                || player.getMonth() == 10);
    }

    public static int fiscalYear(int periodIndex) {
        return Math.floorDiv(periodIndex, 4) + 1;
    }

    public static int quarter(int periodIndex) {
        return Math.floorMod(periodIndex, 4) + 1;
    }

    public static int nextQuarterStartElapsedDay(Player player) {
        int quarterEndMonth = ((player.getMonth() - 1) / 3 + 1) * 3;
        int daysUntilNextQuarter = daysInMonth(player.getMonth()) - player.getDay() + 1;
        for (int month = player.getMonth() + 1; month <= quarterEndMonth; month++) {
            daysUntilNextQuarter += daysInMonth(month);
        }
        return player.getElapsedDays() + daysUntilNextQuarter;
    }

    private static int daysInMonth(int month) {
        return switch (month) {
            case 2 -> 28;
            case 4, 6, 9, 11 -> 30;
            default -> 31;
        };
    }
}
