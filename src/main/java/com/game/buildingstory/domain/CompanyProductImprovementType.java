package com.game.buildingstory.domain;

/** 최초 상용화 이후 반복할 수 있는 핵심제품 개선 종류다. */
public enum CompanyProductImprovementType {
    MODEL_REFINEMENT("추론 모델 고도화", 300, "AI 벤치마크 상승", "연산효율·기술부채 부담"),
    WORKFLOW_AUTOMATION("문서·업무 자동화 확장", 240, "기능 완성도 상승", "유지보수 부담"),
    SERVICE_STABILIZATION("서비스 안정화", 180, "안정성·보안 상승", "신규 성능 개선 없음"),
    INFERENCE_OPTIMIZATION("추론비용 최적화", 240, "연산효율 상승", "벤치마크 상승 없음"),
    NEXT_GENERATION_MODEL("차세대 모델", 750, "벤치마크·기능 완성도 대폭 상승", "안정성·연산효율·기술부채 부담");

    private final String displayName;
    private final int baseWork;
    private final String result;
    private final String risk;

    CompanyProductImprovementType(String displayName, int baseWork, String result, String risk) {
        this.displayName = displayName;
        this.baseWork = baseWork;
        this.result = result;
        this.risk = risk;
    }

    public String getDisplayName() { return displayName; }
    public int getBaseWork() { return baseWork; }
    public String getResult() { return result; }
    public String getRisk() { return risk; }

    public boolean supportsDevelopmentDirection() {
        return this == MODEL_REFINEMENT || this == NEXT_GENERATION_MODEL;
    }
}
