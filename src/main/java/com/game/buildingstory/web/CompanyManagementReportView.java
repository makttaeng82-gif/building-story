package com.game.buildingstory.web;

import java.util.List;

/** 확정 분기 손익과 실제 월 정산 구성을 설명하는 경영보고 모델이다. */
public record CompanyManagementReportView(
        long id,
        String period,
        String operatingProfit,
        String cashChange,
        String cause,
        boolean unread,
        List<Line> lines
) {
    public record Line(String label, String value, String tone) {
    }
}
