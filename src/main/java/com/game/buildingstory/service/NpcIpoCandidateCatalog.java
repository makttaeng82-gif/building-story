package com.game.buildingstory.service;

import com.game.buildingstory.domain.GameCalendar;
import org.springframework.stereotype.Component;

import java.util.List;

/** 16개 NPC 후보의 등장 순서와 기준 상장 시점을 관리한다. */
@Component
public class NpcIpoCandidateCatalog {
    private final List<NpcIpoCandidate> candidates = List.of(
            candidate("corevision", 6, 1_200),
            candidate("greentable", 11, 800),
            candidate("logibridge", 16, 1_200),
            candidate("hanbitrobotics", 22, 1_600),
            candidate("orbitalnet", 27, 1_800),
            candidate("dataforge", 31, 800),
            candidate("wellnessfood", 35, 1_200),
            candidate("metrocommerce", 41, 700),
            candidate("solidcell", 47, 1_200),
            candidate("skylink", 51, 700),
            candidate("cubelogic", 55, 1_800),
            candidate("tastelab", 59, 1_800),
            candidate("pickandgo", 65, 1_800),
            candidate("ecometal", 71, 700),
            candidate("nexusmobile", 76, 1_200),
            candidate("auroralabs", 82, 1_800)
    );

    public List<NpcIpoCandidate> all() {
        return candidates;
    }

    public NpcIpoCandidate require(String stockKey) {
        return candidates.stream().filter(candidate -> candidate.stockKey().equals(stockKey))
                .findFirst().orElseThrow(() -> new IllegalArgumentException("신규상장 후보가 아닙니다: " + stockKey));
    }

    public int targetElapsedDay(int stockUnlockDay, NpcIpoCandidate candidate) {
        int startYear = GameCalendar.year(stockUnlockDay);
        int startMonthIndex = GameCalendar.month(stockUnlockDay) - 1;
        int targetMonthIndex = startMonthIndex + candidate.targetMonthAfterUnlock();
        int targetYear = startYear + targetMonthIndex / 12;
        int targetMonth = targetMonthIndex % 12 + 1;
        int targetDay = Math.min(GameCalendar.day(stockUnlockDay), GameCalendar.daysInMonth(targetMonth));
        return GameCalendar.elapsedDay(targetYear, targetMonth, targetDay);
    }

    private NpcIpoCandidate candidate(String stockKey, int targetMonth, int discountBasisPoints) {
        return new NpcIpoCandidate(stockKey, targetMonth, discountBasisPoints);
    }
}
