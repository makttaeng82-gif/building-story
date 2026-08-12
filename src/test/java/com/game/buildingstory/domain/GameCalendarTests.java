package com.game.buildingstory.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GameCalendarTests {
    @Test
    void startsAtYearOneJanuaryFirstAndAdvancesYears() {
        assertThat(GameCalendar.dateText(1)).isEqualTo("1년 1월 1일");
        assertThat(GameCalendar.dateText(365)).isEqualTo("1년 12월 31일");
        assertThat(GameCalendar.dateText(366)).isEqualTo("2년 1월 1일");
    }

    @Test
    void playerMonthDayAndDerivedYearAdvanceTogether() {
        Player player = new Player("calendar", "hash");
        for (int day = 0; day < 365; day++) player.advanceDay();

        assertThat(player.getCurrentDateText()).isEqualTo("2년 1월 1일");
        assertThat(player.getMonth()).isEqualTo(1);
        assertThat(player.getDay()).isEqualTo(1);
    }

    @Test
    void formatsGameQuarterFromZeroBasedMonthIndex() {
        assertThat(GameCalendar.quarterTextFromMonthIndex(0)).isEqualTo("1년 1분기");
        assertThat(GameCalendar.quarterTextFromMonthIndex(11)).isEqualTo("1년 4분기");
        assertThat(GameCalendar.quarterTextFromMonthIndex(12)).isEqualTo("2년 1분기");
        assertThat(GameCalendar.quarterTextFromElapsedDay(1)).isEqualTo("1년 1분기");
        assertThat(GameCalendar.quarterTextFromElapsedDay(366)).isEqualTo("2년 1분기");
    }

    @Test
    void marksGeneratedStockHistoryBeforeGameStartWithoutYearZero() {
        assertThat(GameCalendar.dateText(0)).isEqualTo("게임 시작 1년 전 12월 31일");
    }
}
