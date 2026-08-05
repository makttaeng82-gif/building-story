package com.game.buildingstory.domain;

/** 창업 이력서 카탈로그 한 명의 변경되지 않는 조건이다. */
public record CompanyTalentCandidate(
        String key,
        String name,
        CompanyDepartmentType departmentType,
        String specialty,
        int grade,
        int ability,
        int growthSpeed,
        int organizationFit,
        int leadership,
        long annualSalary,
        long signingBonus
) {
}
