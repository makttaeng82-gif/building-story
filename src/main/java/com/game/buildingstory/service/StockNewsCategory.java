package com.game.buildingstory.service;

/** 시장 뉴스 피드에서 기사의 원인 범위를 구분한다. */
public enum StockNewsCategory {
    MARKET("market", "시장"),
    INDUSTRY("industry", "업종"),
    COMPANY("company", "기업"),
    IPO("ipo", "IPO");

    private final String cssClass;
    private final String label;

    StockNewsCategory(String cssClass, String label) {
        this.cssClass = cssClass;
        this.label = label;
    }

    public String cssClass() { return cssClass; }
    public String label() { return label; }
}
