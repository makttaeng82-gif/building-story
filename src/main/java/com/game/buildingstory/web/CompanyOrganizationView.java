package com.game.buildingstory.web;

public record CompanyOrganizationView(
        String currentName,
        String employeeLimit,
        String currentEmployees,
        String remainingCapacity,
        String monthlyHireLimit,
        String approvedVacancies,
        String pendingHires,
        String capacityEffect,
        String automaticHiringStatus,
        String monthlyHireRule,
        boolean automaticHiringEnabled,
        boolean automaticHiringAvailable,
        boolean upgradeInProgress,
        String upgradeStatus,
        boolean nextAvailable,
        String nextName,
        String nextRequirement,
        String nextCost
) {
}
