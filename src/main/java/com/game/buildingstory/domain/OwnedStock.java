package com.game.buildingstory.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.math.BigInteger;

@Entity
@Table(name = "owned_stock", uniqueConstraints =
        @UniqueConstraint(name = "uk_owned_stock_player_key", columnNames = {"player_id", "stock_key"}))
public class OwnedStock {
    /*
     * 플레이어가 보유한 특정 주식의 수량과 평균단가다.
     *
     * 가격 이력은 StockPriceHistory에 따로 저장된다. 이 엔티티는 "몇 주를 얼마 평균에 샀는지"만
     * 들고 있어 평가손익 계산의 원가 기준으로 사용된다.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Player player;

    private String stockKey;
    private long quantity;
    private long averagePrice;
    private Long totalCostBasis;

    protected OwnedStock() {
    }

    public OwnedStock(Player player, String stockKey) {
        this.player = player;
        this.stockKey = stockKey;
        this.quantity = 0;
        this.averagePrice = 0;
        this.totalCostBasis = 0L;
    }

    public Long getId() {
        return id;
    }

    public Player getPlayer() {
        return player;
    }

    public String getStockKey() {
        return stockKey;
    }

    public long getQuantity() {
        return quantity;
    }

    public long getAveragePrice() {
        return averagePrice;
    }

    public long getTotalCostBasis() {
        // 기존 저장 데이터에는 이 컬럼이 없었으므로 평균단가로 원가를 복원한다.
        if (totalCostBasis == null || (totalCostBasis == 0 && quantity > 0)) {
            return Math.multiplyExact(averagePrice, quantity);
        }
        return totalCostBasis;
    }

    public void buy(long buyQuantity, long purchaseCost) {
        // purchaseCost에는 매수금액과 수수료가 모두 포함된다.
        if (buyQuantity <= 0) {
            return;
        }
        totalCostBasis = Math.addExact(getTotalCostBasis(), purchaseCost);
        quantity = Math.addExact(quantity, buyQuantity);
        averagePrice = totalCostBasis / quantity;
    }

    public long sell(long sellQuantity) {
        // 일부 매도 원가는 현재 총취득원가에서 매도 수량 비율만큼 배분한다.
        if (sellQuantity <= 0 || quantity < sellQuantity) {
            return -1;
        }
        long currentCostBasis = getTotalCostBasis();
        long soldCostBasis = BigInteger.valueOf(currentCostBasis)
                .multiply(BigInteger.valueOf(sellQuantity))
                .divide(BigInteger.valueOf(quantity))
                .longValueExact();
        quantity -= sellQuantity;
        if (quantity == 0) {
            averagePrice = 0;
            totalCostBasis = 0L;
        } else {
            totalCostBasis = currentCostBasis - soldCostBasis;
            averagePrice = totalCostBasis / quantity;
        }
        return soldCostBasis;
    }
}
