package com.game.buildingstory.web;

import com.game.buildingstory.domain.CompanyDepartmentType;

/** 기업 화면에서 반복 사용하는 상태·인원 표시 규칙을 한곳에서 관리한다. */
final class CompanyViewText {
    private CompanyViewText() {
    }

    static String departmentKey(CompanyDepartmentType type) {
        return switch (type) {
            case AI_DEVELOPMENT -> "development";
            case SALES_MARKETING -> "sales";
            case SERVICE_OPERATIONS -> "operations";
            case HR_ORGANIZATION -> "hr";
            case STRATEGY_FINANCE -> "finance";
        };
    }

    static int progressPercent(long value, long required) {
        if (required <= 0) {
            return 100;
        }
        return (int) Math.max(0, Math.min(100, value * 100.0 / required));
    }

    static String stabilityStatus(int value) {
        if (value >= 80) {
            return "양호";
        }
        return value >= 60 ? "주의" : "위험";
    }

    static String stabilityTone(int value) {
        if (value >= 80) {
            return "good";
        }
        return value >= 60 ? "warn" : "danger";
    }

    static String technicalDebtStatus(int value) {
        if (value <= 20) {
            return "양호";
        }
        return value <= 40 ? "주의" : "위험";
    }

    static String technicalDebtTone(int value) {
        if (value <= 20) {
            return "good";
        }
        return value <= 40 ? "warn" : "danger";
    }

    static String formatPeople(long people) {
        if (people >= 100_000_000L) {
            return String.format("%.1f억명", people / 100_000_000.0);
        }
        if (people >= 10_000L) {
            return String.format("%.1f만명", people / 10_000.0);
        }
        return people + "명";
    }

    /**
     * 인사조직팀의 분석 역량이 높을수록 후보자의 실제 성장속도에 가까운 값을 보여준다.
     */
    static String growthSpeedText(int growthSpeed, int hrExpertise) {
        if (hrExpertise < 0) {
            return "분석 전";
        }
        if (hrExpertise >= 70) {
            return String.valueOf(growthSpeed);
        }
        int margin = hrExpertise >= 40 ? 8 : 15;
        return Math.max(1, growthSpeed - margin) + " ~ " + Math.min(100, growthSpeed + margin);
    }
}
