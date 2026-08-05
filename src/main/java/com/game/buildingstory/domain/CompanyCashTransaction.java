package com.game.buildingstory.domain;

import jakarta.persistence.Column;
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

/** 실제 법인 현금의 증감을 보존하여 분기 현금흐름 보고의 근거로 사용한다. */
@Entity
@Table(name = "company_cash_transaction", uniqueConstraints =
        @UniqueConstraint(name = "uk_company_cash_event", columnNames = {"company_id", "event_key"}))
public class CompanyCashTransaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private PlayerCompany company;

    @Column(name = "event_key", nullable = false, length = 120)
    private String eventKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CompanyCashFlowType flowType;

    @Column(nullable = false, length = 80)
    private String description;

    /** 입금은 양수, 출금은 음수로 저장한다. */
    private long amount;
    private int elapsedDay;
    private int periodIndex;

    protected CompanyCashTransaction() {
    }

    public CompanyCashTransaction(
            PlayerCompany company,
            String eventKey,
            CompanyCashFlowType flowType,
            String description,
            long amount,
            int elapsedDay,
            int periodIndex
    ) {
        this.company = company;
        this.eventKey = eventKey;
        this.flowType = flowType;
        this.description = description;
        this.amount = amount;
        this.elapsedDay = elapsedDay;
        this.periodIndex = periodIndex;
    }

    public Long getId() { return id; }
    public PlayerCompany getCompany() { return company; }
    public String getEventKey() { return eventKey; }
    public CompanyCashFlowType getFlowType() { return flowType; }
    public String getDescription() { return description; }
    public long getAmount() { return amount; }
    public int getElapsedDay() { return elapsedDay; }
    public int getPeriodIndex() { return periodIndex; }
}
