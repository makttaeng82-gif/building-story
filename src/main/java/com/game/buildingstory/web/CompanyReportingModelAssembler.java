package com.game.buildingstory.web;

import com.game.buildingstory.domain.CompanyNewsArticle;
import com.game.buildingstory.domain.PlayerCompany;
import com.game.buildingstory.service.CompanyNewsService;
import com.game.buildingstory.service.CompanyReportingService;
import org.springframework.stereotype.Component;
import org.springframework.ui.Model;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** 기업 대시보드의 뉴스, 경영보고와 재무전망 조회 모델을 조립한다. */
@Component
public class CompanyReportingModelAssembler {
    private static final Pattern RAW_NEWS_MONEY =
            Pattern.compile("(?<![\\d,])(-?[\\d,]{5,})원");

    private final CompanyNewsService companyNewsService;
    private final CompanyReportingService companyReportingService;
    private final MoneyText moneyText;

    public CompanyReportingModelAssembler(
            CompanyNewsService companyNewsService,
            CompanyReportingService companyReportingService,
            MoneyText moneyText
    ) {
        this.companyNewsService = companyNewsService;
        this.companyReportingService = companyReportingService;
        this.moneyText = moneyText;
    }

    public void addAttributes(PlayerCompany company, boolean operational, Model model) {
        if (!operational) {
            model.addAttribute("companyNews", List.of());
            model.addAttribute("companyNewsCount", 0);
            model.addAttribute("companyUnreadNewsCount", 0L);
            model.addAttribute("companyManagementReports", List.of());
            model.addAttribute("companyUnreadReportCount", 0L);
            model.addAttribute("companyFinancialForecast",
                    CompanyFinancialForecastView.unavailable("제품 출시 후 전망을 제공합니다."));
            return;
        }

        List<CompanyNewsArticle> allNews = companyNewsService.articles(company);
        List<CompanyNewsArticle> visibleNews = new ArrayList<>(allNews.stream().limit(20).toList());
        allNews.stream().skip(20).filter(CompanyNewsArticle::isUnread).forEach(visibleNews::add);
        List<CompanyNewsView> newsViews = visibleNews.stream()
                .map(article -> new CompanyNewsView(
                        article.getId(),
                        article.getOccurredMarketMonth() + "개월차",
                        article.getCategory().getDisplayName(),
                        formatNewsMoney(article.getTitle()),
                        formatNewsMoney(article.getBody()),
                        article.getSource(),
                        article.isUnread()))
                .toList();
        List<CompanyManagementReportView> reports = companyReportingService.reports(company);

        model.addAttribute("companyNews", newsViews);
        model.addAttribute("companyNewsCount", allNews.size());
        model.addAttribute("companyUnreadNewsCount", allNews.stream().filter(CompanyNewsArticle::isUnread).count());
        model.addAttribute("companyManagementReports", reports);
        model.addAttribute("companyUnreadReportCount",
                reports.stream().filter(CompanyManagementReportView::unread).count());
        model.addAttribute("companyFinancialForecast", companyReportingService.financialForecast(company));
    }

    private String formatNewsMoney(String text) {
        Matcher matcher = RAW_NEWS_MONEY.matcher(text);
        StringBuilder formatted = new StringBuilder();
        while (matcher.find()) {
            long amount = Long.parseLong(matcher.group(1).replace(",", ""));
            matcher.appendReplacement(formatted, Matcher.quoteReplacement(moneyText.format(amount)));
        }
        return matcher.appendTail(formatted).toString();
    }
}
