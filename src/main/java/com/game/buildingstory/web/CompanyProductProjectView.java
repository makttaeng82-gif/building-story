package com.game.buildingstory.web;

import java.util.List;

/** 핵심제품 상세에 표시할 진행 프로젝트 또는 신규 개선 선택지다. */
public record CompanyProductProjectView(
        boolean active,
        String activeName,
        String activeDirection,
        String progress,
        int progressPercent,
        String expectedCompletion,
        List<Option> options,
        List<Direction> directions
) {
    public record Option(String value, String name, String work, String expectedMonths, String result, String risk,
                         boolean requiresDirection, boolean available, String availabilityReason) {
    }

    public record Direction(String value, String name, String description) {
    }
}
