package com.game.buildingstory.web;

import java.util.List;

/** 부서 상세 화면에 표시할 핵심인재 재직자와 채용후보 목록이다. */
public record CompanyCoreTalentView(
        int occupiedSlots,
        int maximumSlots,
        List<Employee> employees,
        List<Candidate> candidates
) {
    public record Employee(long id, String name, int grade, int ability, int leadership, String growthSpeed,
                           String status, boolean teamLeader, boolean canLead,
                           int resignationRisk, boolean resignationWarning, String retentionCost,
                           boolean canTrain, String trainingCost) {
    }

    public record Candidate(String key, String name, String specialty, int grade, int ability,
                            int leadership, String growthSpeed, String annualSalary, String signingBonus) {
    }
}
