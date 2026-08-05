package com.game.buildingstory.web;

import java.util.List;

/**
 * 기업 화면이 한 번에 표시할 정보를 묶은 읽기 전용 모델이다.
 *
 * <p>현재는 화면 골격을 검증하기 위한 예시 데이터를 사용한다. 이후 기업 도메인이 구현되면
 * CompanyPageModelAssembler가 실제 서비스 결과를 이 형식으로 변환한다.</p>
 */
public record CompanyDashboardView(
        Identity identity,
        List<Metric> summaryMetrics,
        String selectedKey,
        List<OperationItem> departments,
        List<OperationItem> businesses,
        List<WorkItem> workQueue,
        List<NewsItem> news,
        List<ReportItem> reports
) {
    public record Identity(String companyName, String stage, String industry, String foundedText, String headquarters, String ceo) {
    }

    public record Metric(String label, String value, String note, String tone) {
    }

    public record OperationItem(
            String key,
            String group,
            String name,
            String description,
            String status,
            String tone,
            String leader,
            String workforce,
            String result,
            int progressPercent,
            List<Metric> detailMetrics,
            Secretary secretary
    ) {
    }

    public record Secretary(String name, String role, String imagePath, String proficiency, String effect, String advice) {
    }

    public record WorkItem(
            String priority,
            String tone,
            String title,
            String owner,
            String dueText,
            String status,
            String targetKey
    ) {
        public WorkItem(String priority, String tone, String title, String owner, String dueText, String status) {
            this(priority, tone, title, owner, dueText, status, null);
        }
    }

    public record NewsItem(String dateText, String category, String title, String source) {
    }

    public record ReportItem(String label, String currentValue, String previousValue, String changeText, String tone) {
    }
}
