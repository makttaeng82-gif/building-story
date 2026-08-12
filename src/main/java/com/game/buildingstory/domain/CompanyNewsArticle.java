package com.game.buildingstory.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/** 실제 기업 운영 결과에서 발행된 공개 뉴스를 저장한다. */
@Entity
@Table(name = "company_news_article", uniqueConstraints =
        @UniqueConstraint(name = "uk_company_news_event", columnNames = {"company_id", "event_key"}))
public class CompanyNewsArticle {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private PlayerCompany company;

    private String eventKey;

    @Enumerated(EnumType.STRING)
    @jakarta.persistence.Column(columnDefinition = "varchar(32)")
    private CompanyNewsCategory category;

    private String title;
    private String source;
    private int occurredMarketMonth;
    private Integer occurredElapsedDay;
    private Boolean unread;

    @jakarta.persistence.Column(length = 2000)
    private String body;

    protected CompanyNewsArticle() {
    }

    public CompanyNewsArticle(
            PlayerCompany company,
            String eventKey,
            CompanyNewsCategory category,
            String title,
            String body,
            String source,
            int occurredMarketMonth
    ) {
        this(company, eventKey, category, title, body, source, occurredMarketMonth,
                company.getPlayer().getElapsedDays());
    }

    public CompanyNewsArticle(
            PlayerCompany company,
            String eventKey,
            CompanyNewsCategory category,
            String title,
            String body,
            String source,
            int occurredMarketMonth,
            int occurredElapsedDay
    ) {
        this.company = company;
        this.eventKey = eventKey;
        this.category = category;
        this.title = title;
        this.body = body;
        this.source = source;
        this.occurredMarketMonth = occurredMarketMonth;
        this.occurredElapsedDay = occurredElapsedDay;
        this.unread = true;
    }

    public Long getId() { return id; }
    public PlayerCompany getCompany() { return company; }
    public String getEventKey() { return eventKey; }
    public CompanyNewsCategory getCategory() { return category; }
    public String getTitle() { return title; }
    public String getBody() { return body; }
    public String getSource() { return source; }
    public int getOccurredMarketMonth() { return occurredMarketMonth; }
    public int getOccurredElapsedDay(
            int establishedElapsedDay,
            int currentElapsedDay,
            int currentMarketMonth
    ) {
        if (occurredElapsedDay != null) {
            return Math.min(currentElapsedDay, occurredElapsedDay);
        }
        int monthsAgo = Math.max(0, currentMarketMonth - occurredMarketMonth);
        int estimatedDay = currentElapsedDay - monthsAgo * 30;
        return Math.max(establishedElapsedDay, Math.min(currentElapsedDay, estimatedDay));
    }
    public boolean isUnread() { return Boolean.TRUE.equals(unread); }

    public void markRead() {
        unread = false;
    }

    public void revisePublication(String title, String body, int marketMonth, int elapsedDay) {
        this.title = title;
        this.body = body;
        this.occurredMarketMonth = marketMonth;
        this.occurredElapsedDay = elapsedDay;
    }

    public void mergeUpdate(CompanyNewsArticle update) {
        appendUpdate(update.getBody());
        unread = isUnread() || update.isUnread();
    }

    public void appendUpdate(String updateBody) {
        if (updateBody == null || updateBody.isBlank() || body.contains(updateBody)) {
            return;
        }
        body = body + "\n\n" + updateBody;
        unread = true;
    }
}
