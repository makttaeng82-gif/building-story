package com.game.buildingstory.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

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

    protected OwnedStock() {
    }

    public OwnedStock(Player player, String stockKey) {
        this.player = player;
        this.stockKey = stockKey;
        this.quantity = 0;
        this.averagePrice = 0;
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

    public void buy(long buyQuantity, long price) {
        // 새 평균단가는 기존 원가 총액과 신규 매수 원가를 합친 뒤 전체 수량으로 나눈 값이다.
        if (buyQuantity <= 0) {
            return;
        }
        long previousCostBasis = Math.multiplyExact(averagePrice, quantity);
        long newCostBasis = Math.multiplyExact(price, buyQuantity);
        long totalCostBasis = Math.addExact(previousCostBasis, newCostBasis);
        quantity = Math.addExact(quantity, buyQuantity);
        averagePrice = totalCostBasis / quantity;
    }

    public boolean sell(long sellQuantity) {
        // 일부 매도는 평균단가를 유지한다. 전량 매도하면 다음 매수를 새 원가로 시작해야 하므로 평균단가를 0으로 초기화한다.
        if (sellQuantity <= 0 || quantity < sellQuantity) {
            return false;
        }
        quantity -= sellQuantity;
        if (quantity == 0) {
            averagePrice = 0;
        }
        return true;
    }
}
