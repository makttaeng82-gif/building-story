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

/** 발생한 시장·산업 이슈와 적용 기간을 보존한다. 세부 효과는 고정 카탈로그에서 조회한다. */
@Entity
@Table(name = "company_external_event", uniqueConstraints =
        @UniqueConstraint(name = "uk_company_external_event", columnNames = {"company_id", "event_key"}))
public class CompanyExternalEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private PlayerCompany company;

    private String eventKey;
    private String catalogKey;

    @Enumerated(EnumType.STRING)
    private CompanyExternalEventCategory category;

    private int startMarketMonth;
    private int endMarketMonth;
    private int variantIndex;

    protected CompanyExternalEvent() {
    }

    public CompanyExternalEvent(
            PlayerCompany company,
            String eventKey,
            String catalogKey,
            CompanyExternalEventCategory category,
            int startMarketMonth,
            int endMarketMonth,
            int variantIndex
    ) {
        this.company = company;
        this.eventKey = eventKey;
        this.catalogKey = catalogKey;
        this.category = category;
        this.startMarketMonth = startMarketMonth;
        this.endMarketMonth = endMarketMonth;
        this.variantIndex = variantIndex;
    }

    public Long getId() { return id; }
    public PlayerCompany getCompany() { return company; }
    public String getEventKey() { return eventKey; }
    public String getCatalogKey() { return catalogKey; }
    public CompanyExternalEventCategory getCategory() { return category; }
    public int getStartMarketMonth() { return startMarketMonth; }
    public int getEndMarketMonth() { return endMarketMonth; }
    public int getVariantIndex() { return variantIndex; }

    public boolean isActiveAt(int marketMonth) {
        return startMarketMonth <= marketMonth && marketMonth <= endMarketMonth;
    }
}
