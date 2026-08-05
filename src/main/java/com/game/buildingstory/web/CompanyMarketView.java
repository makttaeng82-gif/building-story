package com.game.buildingstory.web;

import java.util.List;

/** 핵심제품 상세에서 보여줄 시장과 경쟁사 비교 정보다. */
public record CompanyMarketView(
        String totalMarketUsers,
        String marketShare,
        String marketBenchmark,
        String benchmarkRank,
        String planMix,
        String normalSubscribers,
        String proSubscribers,
        String maxSubscribers,
        List<Competitor> competitors
) {
    public record Competitor(String name, String strategy, String benchmark, String marketShare,
                             String completeness, String stability, String security) {
    }
}
