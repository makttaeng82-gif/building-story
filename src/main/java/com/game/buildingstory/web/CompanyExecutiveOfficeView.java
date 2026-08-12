package com.game.buildingstory.web;

import java.util.List;

/**
 * 대표실 화면에 표시할 총괄비서 현황과 부서별 업무 연결 정보다.
 *
 * <p>총괄 화면은 새로운 업무 규칙을 만들지 않는다. 각 명령은 기존 부서 상세 화면으로 연결되어
 * 동일한 비용, 처리능력, 동시 진행 제한을 사용한다.</p>
 */
public record CompanyExecutiveOfficeView(
        CompanyDashboardView.OperationItem operation,
        List<DepartmentCommand> commands,
        CompanyProfile companyProfile
) {
    public record DepartmentCommand(
            String departmentName,
            String targetKey,
            String status,
            String tone,
            String recommendation
    ) {
    }

    /** 대표실에서 기업 자체 정보와 상장 후 주식 정보를 한 번에 확인하기 위한 화면 값이다. */
    public record CompanyProfile(
            String stage,
            String foundedText,
            String headquarters,
            String enterpriseValue,
            String listingStatus,
            String currentPrice,
            String marketCap,
            String issuedShares,
            String founderShares,
            String ownership,
            String listingDate,
            String offerPrice,
            boolean listed
    ) {
    }
}
