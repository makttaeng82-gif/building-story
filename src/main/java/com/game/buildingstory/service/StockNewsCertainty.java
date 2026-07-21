package com.game.buildingstory.service;

/** 업종 기사가 실제 실적에 반영되는 정도를 구분한다. */
public enum StockNewsCertainty {
    CONFIRMED("확정", true),
    OUTLOOK("관측", false),
    RUMOR("미확인", false);

    private final String label;
    private final boolean financialEffectImmediate;

    StockNewsCertainty(String label, boolean financialEffectImmediate) {
        this.label = label;
        this.financialEffectImmediate = financialEffectImmediate;
    }

    public String label() {
        return label;
    }

    public boolean financialEffectImmediate() {
        return financialEffectImmediate;
    }
}
