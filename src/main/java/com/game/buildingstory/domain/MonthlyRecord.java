package com.game.buildingstory.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Column;

@Entity
public class MonthlyRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Player player;

    @Column(name = "record_month")
    private int month;

    @Column(name = "record_day")
    private int day;
    private int elapsedDays;

    @Enumerated(EnumType.STRING)
    @Column(name = "record_type")
    private RecordType type;

    private String title;
    private Long amount;
    private long cashAfter;
    private int reputationChange;
    private String buildingName;
    private String memo;

    protected MonthlyRecord() {
    }

    public MonthlyRecord(Player player, RecordType type, String title, Long amount, int reputationChange, String buildingName, String memo) {
        this.player = player;
        this.month = player.getMonth();
        this.day = player.getDay();
        this.elapsedDays = player.getElapsedDays();
        this.type = type;
        this.title = title;
        this.amount = amount;
        this.cashAfter = player.getCash();
        this.reputationChange = reputationChange;
        this.buildingName = buildingName;
        this.memo = memo;
    }

    public Long getId() {
        return id;
    }

    public int getMonth() {
        return month;
    }

    public int getDay() {
        return day;
    }

    public int getElapsedDays() {
        return elapsedDays;
    }

    public RecordType getType() {
        return type;
    }

    public String getTitle() {
        return title;
    }

    public Long getAmount() {
        return amount;
    }

    public long getCashAfter() {
        return cashAfter;
    }

    public int getReputationChange() {
        return reputationChange;
    }

    public String getBuildingName() {
        return buildingName;
    }

    public String getMemo() {
        return memo;
    }
}
