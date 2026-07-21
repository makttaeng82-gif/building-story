package com.game.buildingstory.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/** 플레이어별 주식시장 경기 국면과 남은 지속 횟수를 보존한다. */
@Entity
@Table(name = "stock_market_regime_state", uniqueConstraints =
        @UniqueConstraint(name = "uk_stock_market_regime_player", columnNames = "player_id"))
public class StockMarketRegimeState {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    private Player player;

    private String regime;
    private Integer remainingUpdates;
    private Integer quarterExpansionUpdates;
    private Integer quarterNeutralUpdates;
    private Integer quarterRecessionUpdates;

    protected StockMarketRegimeState() {
    }

    public StockMarketRegimeState(Player player, String regime, int remainingUpdates) {
        this.player = player;
        this.regime = regime;
        this.remainingUpdates = remainingUpdates;
    }

    public Player getPlayer() { return player; }
    public String getRegime() { return regime; }
    public int getRemainingUpdates() { return remainingUpdates == null ? 0 : remainingUpdates; }
    public int getQuarterExpansionUpdates() {
        return quarterExpansionUpdates == null ? 0 : quarterExpansionUpdates;
    }
    public int getQuarterNeutralUpdates() {
        return quarterNeutralUpdates == null ? 0 : quarterNeutralUpdates;
    }
    public int getQuarterRecessionUpdates() {
        return quarterRecessionUpdates == null ? 0 : quarterRecessionUpdates;
    }

    /** 현재 가격 갱신에 실제 사용된 경기 국면을 분기 노출 횟수에 누적한다. */
    public void recordQuarterExposure() {
        switch (regime) {
            case "EXPANSION" -> quarterExpansionUpdates = getQuarterExpansionUpdates() + 1;
            case "RECESSION" -> quarterRecessionUpdates = getQuarterRecessionUpdates() + 1;
            default -> quarterNeutralUpdates = getQuarterNeutralUpdates() + 1;
        }
    }

    /** 결산에서 읽은 노출값이 다음 분기에 다시 쓰이지 않도록 모두 초기화한다. */
    public void clearQuarterExposure() {
        quarterExpansionUpdates = 0;
        quarterNeutralUpdates = 0;
        quarterRecessionUpdates = 0;
    }

    public void advance(String nextRegime, int nextDuration) {
        int remaining = getRemainingUpdates() - 1;
        if (remaining > 0) {
            remainingUpdates = remaining;
            return;
        }
        regime = nextRegime;
        remainingUpdates = nextDuration;
    }
}
