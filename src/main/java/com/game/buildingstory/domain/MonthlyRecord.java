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
    /*
     * 월세, 구매, 판매, 이벤트 같은 게임 로그를 저장한다.
     *
     * 화면의 최근 기록 패널은 이 엔티티를 날짜 역순으로 읽는다. 금액 변화와 평판 변화를 함께 저장해
     * 플레이어가 왜 자금/평판이 바뀌었는지 추적할 수 있게 한다.
     */
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
