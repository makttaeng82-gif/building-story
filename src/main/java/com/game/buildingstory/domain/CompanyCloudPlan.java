package com.game.buildingstory.domain;

/** 외부 클라우드의 월 정액 계약 등급과 처리한도를 한곳에서 관리한다. */
public enum CompanyCloudPlan {
    STARTER("스타터", 400_000L, 9_000_000_000L),
    GROWTH("성장", 2_000_000L, 25_000_000_000L),
    SCALE("스케일", 10_000_000L, 70_000_000_000L),
    ENTERPRISE("엔터프라이즈", 50_000_000L, 300_000_000_000L),
    HYPERSCALE("하이퍼스케일", 150_000_000L, 900_000_000_000L),
    GLOBAL("글로벌", 600_000_000L, 3_200_000_000_000L);

    private final String displayName;
    private final long capacity;
    private final long monthlyCost;

    CompanyCloudPlan(String displayName, long capacity, long monthlyCost) {
        this.displayName = displayName;
        this.capacity = capacity;
        this.monthlyCost = monthlyCost;
    }

    public String getDisplayName() { return displayName; }
    public long getCapacity() { return capacity; }
    public long getMonthlyCost() { return monthlyCost; }
}
