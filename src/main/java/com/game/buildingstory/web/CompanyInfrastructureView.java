package com.game.buildingstory.web;

import java.util.List;

/** 연산인프라 상세에서 보여줄 처리한도와 계약 변경 정보다. */
public record CompanyInfrastructureView(
        String planName,
        String pendingPlanName,
        String demand,
        String cloudCapacity,
        String ownNetworkName,
        String ownCapacity,
        String ownMonthlyCost,
        String permanentCapacity,
        String reserveCapacity,
        String totalCapacity,
        String reserveRemaining,
        String reserveCost,
        boolean reserveActive,
        String operationsCapacity,
        String usableCapacity,
        String utilization,
        String status,
        String tone,
        List<PlanOption> plans,
        List<ReserveOption> reserveOptions,
        Construction construction,
        NextConstruction nextConstruction
) {
    public record PlanOption(String value, String name, String capacity, String monthlyCost, boolean current) {
    }

    public record ReserveOption(int percentage, String capacity, String cost, boolean available, String reason) {
    }

    public record Construction(String name, String progress, int progressPercent, String remainingPayment, boolean paymentDue) {
    }

    public record NextConstruction(String name, String capacity, String totalCost, String upfrontPayment,
                                   String monthlyCost, String duration, boolean available, String reason) {
    }
}
