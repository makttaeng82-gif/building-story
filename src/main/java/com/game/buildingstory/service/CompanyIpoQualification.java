package com.game.buildingstory.service;

import java.util.List;

/** IPO 자격 판정 결과와 각 조건의 현재값을 함께 전달한다. */
public record CompanyIpoQualification(List<RequirementCheck> checks) {
    public CompanyIpoQualification {
        checks = List.copyOf(checks);
    }

    public boolean canApply() {
        return checks.stream().allMatch(RequirementCheck::met);
    }

    public List<RequirementCheck> unmetChecks() {
        return checks.stream().filter(check -> !check.met()).toList();
    }

    public RequirementCheck check(CompanyIpoRequirement requirement) {
        return checks.stream()
                .filter(check -> check.requirement() == requirement)
                .findFirst()
                .orElseThrow();
    }

    /**
     * 모든 조건을 숫자로 표현해 화면 문구가 판정 로직을 다시 계산하지 않게 한다.
     * 참·거짓 조건은 1과 0, 기업 단계는 enum 순번을 사용한다.
     */
    public record RequirementCheck(
            CompanyIpoRequirement requirement,
            long currentValue,
            long requiredValue,
            boolean met
    ) {
    }
}
