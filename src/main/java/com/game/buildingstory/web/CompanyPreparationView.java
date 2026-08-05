package com.game.buildingstory.web;

import java.util.List;

/** 기업 설립 전 준비 상태와 출자 예상 계산에 필요한 읽기 전용 화면 모델이다. */
public record CompanyPreparationView(
        long personalCash,
        String personalCashText,
        long minimumInvestment,
        long recommendedInvestment,
        long aggressiveInvestment,
        long secretaryTrainingCost,
        long estimatedMonthlyFixedCost,
        int ownedSecretaryCount,
        int readySecretaryCount,
        int propertyManagerCount,
        int satisfiedRequirementCount,
        int enforcedRequirementCount,
        List<Requirement> requirements
) {
    public record Requirement(String label, String currentText, String requirementText, boolean satisfied, boolean enforced) {
    }
}
