package com.game.buildingstory.domain;

/** 경과일을 1년 1월 1일부터 시작하는 게임 달력으로 변환한다. */
public final class GameCalendar {
    public static final int DAYS_PER_YEAR = 365;

    private static final int[] DAYS_IN_MONTH = {31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31};

    private GameCalendar() {
    }

    public static int year(int elapsedDay) {
        return Math.floorDiv(elapsedDay - 1, DAYS_PER_YEAR) + 1;
    }

    public static int month(int elapsedDay) {
        int dayOfYear = Math.floorMod(elapsedDay - 1, DAYS_PER_YEAR) + 1;
        int month = 1;
        while (dayOfYear > DAYS_IN_MONTH[month - 1]) {
            dayOfYear -= DAYS_IN_MONTH[month - 1];
            month++;
        }
        return month;
    }

    public static int day(int elapsedDay) {
        int dayOfYear = Math.floorMod(elapsedDay - 1, DAYS_PER_YEAR) + 1;
        int month = 1;
        while (dayOfYear > DAYS_IN_MONTH[month - 1]) {
            dayOfYear -= DAYS_IN_MONTH[month - 1];
            month++;
        }
        return dayOfYear;
    }

    /** 게임 연·월·일을 1년 1월 1일부터 흐른 절대 일수로 바꾼다. */
    public static int elapsedDay(int year, int month, int day) {
        if (year < 1 || month < 1 || month > 12 || day < 1 || day > DAYS_IN_MONTH[month - 1]) {
            throw new IllegalArgumentException("유효하지 않은 게임 날짜입니다.");
        }
        int result = Math.multiplyExact(year - 1, DAYS_PER_YEAR) + day;
        for (int index = 0; index < month - 1; index++) {
            result += DAYS_IN_MONTH[index];
        }
        return result;
    }

    public static int daysInMonth(int month) {
        if (month < 1 || month > 12) {
            throw new IllegalArgumentException("월은 1부터 12 사이여야 합니다.");
        }
        return DAYS_IN_MONTH[month - 1];
    }

    public static String dateText(int elapsedDay) {
        int year = year(elapsedDay);
        String prefix = year >= 1 ? year + "년" : "게임 시작 " + (1 - year) + "년 전";
        return prefix + " " + month(elapsedDay) + "월 " + day(elapsedDay) + "일";
    }

    public static String shortDateText(int elapsedDay) {
        int year = year(elapsedDay);
        String prefix = year >= 1 ? year + "년" : "시작 " + (1 - year) + "년 전";
        return prefix + " " + month(elapsedDay) + "/" + day(elapsedDay);
    }

    public static String quarterTextFromMonthIndex(int monthIndex) {
        int safeIndex = Math.max(0, monthIndex);
        return (safeIndex / 12 + 1) + "년 " + (safeIndex % 12 / 3 + 1) + "분기";
    }

    public static String quarterTextFromElapsedDay(int elapsedDay) {
        return year(elapsedDay) + "년 " + ((month(elapsedDay) - 1) / 3 + 1) + "분기";
    }
}
