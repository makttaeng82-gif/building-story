package com.game.buildingstory.web;

/** 기업 화면에서 부서별 승인 정원과 일반인력 채용 상태를 표시한다. */
public record CompanyWorkforceView(
        String departmentType,
        int currentEmployees,
        int adaptingEmployees,
        int pendingEmployees,
        int approvedHeadcount,
        int maximumApprovedHeadcount,
        int availableToHire,
        String hiringFeePerPerson,
        int lastResignations,
        String lastResignationRate
) {
}
