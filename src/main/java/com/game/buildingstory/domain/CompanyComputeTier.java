package com.game.buildingstory.domain;

/** 자체 연산망의 규모별 네 공사 단계와 누적 운영 수치를 정의한다. */
public enum CompanyComputeTier {
    SMALL("소형", new long[]{250_000L, 500_000L, 750_000L, 1_000_000L},
            new long[]{1_800_000_000L, 3_500_000_000L, 5_300_000_000L, 7_000_000_000L},
            new long[]{60_000_000_000L, 33_000_000_000L, 30_000_000_000L, 27_000_000_000L},
            new int[]{2, 1, 1, 1}),
    MEDIUM("중형", new long[]{2_500_000L, 5_000_000L, 7_500_000L, 10_000_000L},
            new long[]{10_000_000_000L, 20_000_000_000L, 30_000_000_000L, 40_000_000_000L},
            new long[]{480_000_000_000L, 264_000_000_000L, 240_000_000_000L, 216_000_000_000L},
            new int[]{3, 2, 2, 2}),
    LARGE("대형", new long[]{20_000_000L, 30_000_000L, 40_000_000L, 50_000_000L},
            new long[]{66_000_000_000L, 99_000_000_000L, 132_000_000_000L, 165_000_000_000L},
            new long[]{2_400_000_000_000L, 1_320_000_000_000L, 1_200_000_000_000L, 1_080_000_000_000L},
            new int[]{4, 3, 3, 3}),
    HYPERSCALE("하이퍼스케일", new long[]{75_000_000L, 100_000_000L, 125_000_000L, 150_000_000L},
            new long[]{250_000_000_000L, 333_000_000_000L, 417_000_000_000L, 500_000_000_000L},
            new long[]{7_200_000_000_000L, 3_960_000_000_000L, 3_600_000_000_000L, 3_240_000_000_000L},
            new int[]{6, 4, 4, 4}),
    GLOBAL("글로벌", new long[]{250_000_000L, 350_000_000L, 450_000_000L, 600_000_000L},
            new long[]{750_000_000_000L, 1_050_000_000_000L, 1_350_000_000_000L, 1_800_000_000_000L},
            new long[]{28_000_000_000_000L, 15_400_000_000_000L, 14_000_000_000_000L, 12_600_000_000_000L},
            new int[]{8, 6, 6, 6});

    private final String displayName;
    private final long[] capacities;
    private final long[] monthlyCosts;
    private final long[] constructionCosts;
    private final int[] constructionMonths;

    CompanyComputeTier(String displayName, long[] capacities, long[] monthlyCosts,
                       long[] constructionCosts, int[] constructionMonths) {
        this.displayName = displayName;
        this.capacities = capacities;
        this.monthlyCosts = monthlyCosts;
        this.constructionCosts = constructionCosts;
        this.constructionMonths = constructionMonths;
    }

    public String getDisplayName() { return displayName; }
    public long capacity(int phase) { return capacities[phase]; }
    public long monthlyCost(int phase) { return monthlyCosts[phase]; }
    public long constructionCost(int phase) { return constructionCosts[phase]; }
    public int constructionMonths(int phase) { return constructionMonths[phase]; }
    public String phaseName(int phase) { return phase == 0 ? "기반 구축" : phase + "차 증설"; }

    public CompanyComputeTier nextTier() {
        int next = ordinal() + 1;
        return next < values().length ? values()[next] : null;
    }
}
