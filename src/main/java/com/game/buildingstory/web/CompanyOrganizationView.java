package com.game.buildingstory.web;

public record CompanyOrganizationView(
        String currentName,
        String employeeLimit,
        String monthlyHireLimit,
        String capacityEffect,
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
