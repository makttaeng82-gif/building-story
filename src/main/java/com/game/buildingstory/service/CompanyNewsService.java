package com.game.buildingstory.service;

import com.game.buildingstory.domain.CompanyNewsArticle;
import com.game.buildingstory.domain.CompanyNewsCategory;
import com.game.buildingstory.domain.CompanyQuarterlyReport;
import com.game.buildingstory.domain.GameCalendar;
import com.game.buildingstory.domain.PlayerCompany;
import com.game.buildingstory.repo.CompanyNewsArticleRepository;
import com.game.buildingstory.repo.CompanyQuarterlyReportRepository;
import com.game.buildingstory.repo.PlayerCompanyRepository;
import com.game.buildingstory.repo.PlayerRepository;
import com.game.buildingstory.web.MoneyText;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/** 실제 월간 운영 결과를 공개 기업뉴스로 변환하고 읽음 상태를 관리한다. */
@Service
public class CompanyNewsService {
    private final PlayerRepository playerRepository;
    private final PlayerCompanyRepository companyRepository;
    private final CompanyNewsArticleRepository newsRepository;
    private final CompanyServiceIncidentService incidentService;
    private final CompanyQuarterlyReportRepository quarterlyRepository;
    private final MoneyText moneyText;
    private final PlayerCompanyStockNewsBridgeService stockNewsBridgeService;

    public CompanyNewsService(
            PlayerRepository playerRepository,
            PlayerCompanyRepository companyRepository,
            CompanyNewsArticleRepository newsRepository,
            CompanyServiceIncidentService incidentService,
            CompanyQuarterlyReportRepository quarterlyRepository,
            MoneyText moneyText,
            PlayerCompanyStockNewsBridgeService stockNewsBridgeService
    ) {
        this.playerRepository = playerRepository;
        this.companyRepository = companyRepository;
        this.newsRepository = newsRepository;
        this.incidentService = incidentService;
        this.quarterlyRepository = quarterlyRepository;
        this.moneyText = moneyText;
        this.stockNewsBridgeService = stockNewsBridgeService;
    }

    @Transactional
    public void recordMonthlyEvents(
            PlayerCompany company,
            CompanyMarketService.MarketSnapshot market,
            CompanyProductProjectService.ProjectMonthResult productResult,
            CompanyComputeConstructionService.ConstructionMonthResult constructionResult,
            CompanyServiceIncidentService.IncidentMonthResult incidentResult,
            Optional<CompanyQuarterlyReport> quarterlyReport
    ) {
        int month = company.getMarketMonthsProcessed();
        if (month % 3 == 0) {
            create(
                    company,
                    "market-" + month,
                    CompanyNewsCategory.MARKET,
                    "생성형 AI 시장 이용자 기반 확대",
                    "전체 AI 서비스 시장은 " + String.format("%,d명", market.totalMarketUsers())
                            + " 규모로 집계됐다. 시장 기준 벤치마크는 "
                            + String.format("%.1f점", market.marketBenchmark())
                            + "이며 경쟁사와 플레이어 기업의 제품력이 점유율을 나눠 갖고 있다.",
                    "AI산업동향",
                    month,
                    StockNewsDirection.NEUTRAL,
                    0,
                    0,
                    false
            );
        }
        if (productResult.completed()) {
            create(
                    company,
                    "product-" + month + "-" + productResult.notice(),
                    CompanyNewsCategory.PRODUCT,
                    company.getCompanyName() + ", " + productResult.notice(),
                    company.getServiceName() + "의 제품 개선 작업이 완료됐다. 개선 결과는 다음 월부터 "
                            + "제품 성능과 시장 경쟁력 계산에 반영된다.",
                    "회사 공시",
                    month,
                    StockNewsDirection.POSITIVE,
                    80,
                    1,
                    true
            );
        }
        if (constructionResult.completed()) {
            create(
                    company,
                    "infrastructure-" + month + "-" + constructionResult.notice(),
                    CompanyNewsCategory.INFRASTRUCTURE,
                    company.getCompanyName() + ", " + constructionResult.notice(),
                    "자체 연산망 공사를 마치고 처리용량을 정식 가동한다. 완공된 설비의 처리용량과 "
                            + "월 운영비는 이후 서비스 운영에 계속 반영된다.",
                    "회사 공시",
                    month,
                    StockNewsDirection.POSITIVE,
                    50,
                    1,
                    true
            );
        }
        if (incidentResult.generated()) {
            incidentService.incidents(company).stream().findFirst().ifPresent(incident -> create(
                    company,
                    "incident-" + incident.getId(),
                    CompanyNewsCategory.INCIDENT,
                    incident.getSeverity().getDisplayName() + " " + incidentTitle(incident) + " 발생",
                    "회사 운영 과정에서 " + incidentTitle(incident) + "가 확인됐다. "
                            + "대응이 끝날 때까지 고객 계약과 서비스 처리능력에 영향을 줄 수 있다.",
                    "기업 운영실",
                    month,
                    StockNewsDirection.NEGATIVE,
                    incidentPriceImpact(incident.getSeverity()),
                    1,
                    true
            ));
        }
        quarterlyReport.ifPresent(report -> createPerformanceArticle(company, report, month, true));
    }

    @Transactional(readOnly = true)
    public List<CompanyNewsArticle> articles(PlayerCompany company) {
        return newsRepository.findByCompanyOrderByOccurredMarketMonthDescIdDesc(company);
    }

    /**
     * 기업뉴스 기능이 추가되기 전에 생성된 분기보고서도 뉴스에서 확인할 수 있게 시작 시 한 번 보완한다.
     * 화면 조회와 분리해 GET 요청이 데이터베이스를 변경하지 않도록 한다.
     */
    @Transactional
    public void backfillPerformanceArticles() {
        companyRepository.findAll().forEach(company ->
                quarterlyRepository.findByCompanyOrderByQuarterSequenceDesc(company)
                        .forEach(report -> {
                            createPerformanceArticle(
                                    company, report, report.getQuarterSequence() * 3, false);
                            repairPerformancePublication(company, report);
                        }));
    }

    @Transactional
    public boolean markRead(long playerId, long articleId) {
        var player = playerRepository.findByIdForUpdate(playerId).orElseThrow();
        PlayerCompany company = companyRepository.findByPlayer(player).orElseThrow();
        CompanyNewsArticle article = newsRepository.findById(articleId).orElseThrow();
        if (!article.getCompany().getId().equals(company.getId())) {
            return false;
        }
        article.markRead();
        return true;
    }

    @Transactional
    public void recordDividendDecision(PlayerCompany company, CompanyQuarterlyReport report) {
        Optional<CompanyNewsArticle> performanceArticle = newsRepository.findByCompanyAndEventKey(
                company, "performance-" + report.getQuarterSequence());
        if (performanceArticle.isPresent()) {
            long dividendPerShare = report.getDividendAmount()
                    / Math.max(1, company.getIssuedShares());
            String mergedDecision = report.getDividendAmount() <= 0
                    ? "무배당으로 확정됐다."
                    : "배당성향 " + report.getDividendRate() + "% · 주당 "
                            + moneyText.format(dividendPerShare) + "으로 확정됐다.";
            performanceArticle.get().appendUpdate(
                    mergedDecision + " 배당총액은 " + moneyText.format(report.getDividendAmount()) + "이다.");
            return;
        }
        long dividendPerShare = report.getDividendAmount()
                / Math.max(1, company.getIssuedShares());
        String decision = report.getDividendAmount() <= 0
                ? "무배당"
                : "배당성향 " + report.getDividendRate() + "% · 주당 "
                        + moneyText.format(dividendPerShare);
        create(
                company,
                "dividend-" + report.getQuarterSequence(),
                CompanyNewsCategory.PERFORMANCE,
                company.getCompanyName() + " "
                        + FiscalQuarter.periodText(report, company.getPlayer()) + " 배당 결정",
                decision + "으로 확정됐다. 배당총액은 "
                        + moneyText.format(report.getDividendAmount()) + "이다.",
                "회사 공시",
                company.getMarketMonthsProcessed(),
                StockNewsDirection.NEUTRAL,
                0,
                0,
                true
        );
    }

    /** IPO 신청 사실은 상장 전 기업 내부 뉴스에만 남긴다. */
    @Transactional
    public void recordIpoApplication(PlayerCompany company, com.game.buildingstory.domain.CompanyListing listing) {
        create(
                company,
                "ipo-application-" + listing.getApplicationSequence(),
                CompanyNewsCategory.FINANCE,
                company.getCompanyName() + ", 기업공개 절차 착수",
                "신주 " + listing.getSelectedOfferPercent() + "% 공모안을 선택하고 기업공개 준비를 시작했다. "
                        + "준비기간은 " + CompanyIpoPolicy.PREPARATION_MONTHS + "개월이며 준비비 "
                        + moneyText.format(listing.getPreparationCost()) + "은 반환되지 않는다.",
                "회사 공시",
                company.getMarketMonthsProcessed(),
                StockNewsDirection.NEUTRAL,
                0,
                0,
                false
        );
    }

    /** 준비 완료 사실은 최종 심사 대기 상태로 한 번만 기록한다. */
    @Transactional
    public void recordIpoReady(PlayerCompany company, com.game.buildingstory.domain.CompanyListing listing) {
        create(
                company,
                "ipo-ready-" + listing.getApplicationSequence(),
                CompanyNewsCategory.FINANCE,
                company.getCompanyName() + ", 기업공개 준비 완료",
                "기업공개 준비 절차를 마치고 최종 상장 심사를 기다린다. 상장 확정 시 최신 기업가치로 "
                        + "공모가와 법인 조달금을 다시 계산한다.",
                "회사 공시",
                company.getMarketMonthsProcessed(),
                StockNewsDirection.NEUTRAL,
                0,
                0,
                false
        );
    }

    /** 상장 완료 공시는 기업뉴스와 주식 뉴스에 함께 전달하되 추가 가격 충격은 주지 않는다. */
    @Transactional
    public void recordIpoListing(PlayerCompany company, com.game.buildingstory.domain.CompanyListing listing) {
        create(
                company,
                "ipo-listed-" + listing.getApplicationSequence(),
                CompanyNewsCategory.FINANCE,
                company.getCompanyName() + ", 주식시장 상장 완료",
                "공모가 " + moneyText.format(listing.getOfferPrice()) + "에 신주 "
                        + String.format("%,d주", listing.getNewShares()) + "를 발행해 법인자금 "
                        + moneyText.format(listing.getProceeds()) + "을 조달했다.",
                "회사 공시",
                company.getMarketMonthsProcessed(),
                StockNewsDirection.NEUTRAL,
                0,
                0,
                true
        );
    }

    private void create(
            PlayerCompany company,
            String eventKey,
            CompanyNewsCategory category,
            String title,
            String body,
            String source,
            int month,
            StockNewsDirection stockDirection,
            int stockPriceImpactBasisPoints,
            int stockDurationRefreshes,
            boolean publishToStockMarket
    ) {
        if (newsRepository.findByCompanyAndEventKey(company, eventKey).isPresent()) {
            return;
        }
        CompanyNewsArticle article = newsRepository.save(new CompanyNewsArticle(
                company, eventKey, category, title, body, source, month));
        if (publishToStockMarket) {
            stockNewsBridgeService.publish(
                    article,
                    stockDirection,
                    stockPriceImpactBasisPoints,
                    stockDurationRefreshes
            );
        }
    }

    private void createPerformanceArticle(
            PlayerCompany company,
            CompanyQuarterlyReport report,
            int month,
            boolean publishToStockMarket
    ) {
        create(
                company,
                "performance-" + report.getQuarterSequence(),
                CompanyNewsCategory.PERFORMANCE,
                company.getCompanyName() + " "
                        + FiscalQuarter.periodText(report, company.getPlayer()) + " 실적 확정",
                "분기 매출 " + moneyText.format(report.getRevenue()) + ", 영업이익 "
                        + moneyText.format(report.getOperatingProfit()) + ", 순이익 "
                        + moneyText.format(report.getNetIncome())
                        + "으로 집계됐다. 수치는 실제 월 정산 3건을 합산한 확정 실적이다.",
                "회사 공시",
                month,
                StockNewsDirection.NEUTRAL,
                0,
                0,
                publishToStockMarket
        );
    }

    private int incidentPriceImpact(com.game.buildingstory.domain.CompanyServiceIncidentSeverity severity) {
        return switch (severity) {
            case MINOR -> 60;
            case MAJOR -> 120;
            case CRITICAL -> 200;
        };
    }

    private void repairPerformancePublication(PlayerCompany company, CompanyQuarterlyReport report) {
        Optional<CompanyNewsArticle> performance = newsRepository.findByCompanyAndEventKey(
                company, "performance-" + report.getQuarterSequence());
        if (performance.isEmpty()) {
            return;
        }
        CompanyNewsArticle article = performance.get();
        article.revisePublication(
                company.getCompanyName() + " "
                        + FiscalQuarter.periodText(report, company.getPlayer()) + " 실적 확정",
                article.getBody(),
                report.getQuarterSequence() * 3,
                performanceElapsedDay(company, report)
        );
        newsRepository.findByCompanyAndEventKey(company, "dividend-" + report.getQuarterSequence())
                .ifPresent(dividend -> {
                    article.mergeUpdate(dividend);
                    newsRepository.delete(dividend);
                });
    }

    private int performanceElapsedDay(PlayerCompany company, CompanyQuarterlyReport report) {
        int currentDay = company.getPlayer().getElapsedDays();
        if (report.getPublishedElapsedDay() > 0) {
            return Math.min(currentDay, report.getPublishedElapsedDay());
        }
        int periodIndex = Math.max(0, report.getEndingPeriodIndex());
        int year = periodIndex / 12 + 1;
        int month = periodIndex % 12 + 1;
        int establishedDay = GameCalendar.day(company.getEstablishedElapsedDay());
        int day = Math.min(establishedDay, GameCalendar.daysInMonth(month));
        return Math.min(currentDay, GameCalendar.elapsedDay(year, month, day));
    }

    private String incidentTitle(com.game.buildingstory.domain.CompanyServiceIncident incident) {
        if (incident.isSecurityIncident()) {
            return "보안사고";
        }
        return incident.isComputeIncident() ? "연산장비 장애" : "서비스 장애";
    }
}
