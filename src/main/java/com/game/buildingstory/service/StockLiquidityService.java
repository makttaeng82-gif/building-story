package com.game.buildingstory.service;

import com.game.buildingstory.domain.ListedCompany;
import com.game.buildingstory.domain.Player;
import com.game.buildingstory.domain.StockLiquidityState;
import com.game.buildingstory.domain.StockPriceHistory;
import com.game.buildingstory.repo.StockLiquidityStateRepository;
import com.game.buildingstory.repo.StockPriceHistoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigInteger;

/** 시점별 거래 가능 물량을 관리하고 시장가 주문의 평균 체결가와 최종 호가를 계산한다. */
@Service
@Transactional
public class StockLiquidityService {
    private final StockCatalog stockCatalog;
    private final StockLiquidityPolicy policy;
    private final StockLiquidityStateRepository stateRepository;
    private final StockPriceHistoryRepository priceRepository;

    public StockLiquidityService(
            StockCatalog stockCatalog,
            StockLiquidityPolicy policy,
            StockLiquidityStateRepository stateRepository,
            StockPriceHistoryRepository priceRepository
    ) {
        this.stockCatalog = stockCatalog;
        this.policy = policy;
        this.stateRepository = stateRepository;
        this.priceRepository = priceRepository;
    }

    public void ensureInitialized(Player player) {
        for (StockSpec stock : stockCatalog.all()) {
            stateRepository.findByPlayerAndStockKey(player, stock.key()).orElseGet(() -> stateRepository.save(
                    new StockLiquidityState(player, stock.key(), policy.capacity(stock), currentPrice(player, stock))
            ));
        }
    }

    /** 모든 종목의 새 5일 캔들이 생성된 직후 유동성과 주문 충격 기준가를 초기화한다. */
    public void refreshAll(Player player) {
        ensureInitialized(player);
        for (StockLiquidityState state : stateRepository.findByPlayer(player)) {
            StockSpec stock = stockCatalog.find(state.getStockKey()).orElseThrow();
            state.refresh(policy.capacity(stock), currentPrice(player, stock), player.getElapsedDays());
        }
    }

    /** 배당락처럼 5일 갱신 밖에서 기준가격이 바뀐 경우 해당 종목의 주문 누적 상태를 재기준화한다. */
    public void rebase(Player player, String stockKey) {
        StockSpec stock = stockCatalog.find(stockKey).orElseThrow();
        StockLiquidityState state = requireState(player, stockKey);
        state.refresh(policy.capacity(stock), currentPrice(player, stock), player.getElapsedDays());
    }

    public long availableBuyQuantity(Player player, ListedCompany company) {
        StockLiquidityState state = requireState(player, company.getStockKey());
        long remainingFlow = state.getCapacity() - Math.max(0, state.getNetPlayerBuyQuantity());
        return Math.min(company.getMarketParticipantShares(), remainingFlow);
    }

    public long availableSellQuantity(Player player, String stockKey) {
        StockLiquidityState state = requireState(player, stockKey);
        return state.getCapacity() - Math.max(0, -state.getNetPlayerBuyQuantity());
    }

    public Execution preview(Player player, String stockKey, long signedQuantity) {
        StockLiquidityState state = requireState(player, stockKey);
        StockLiquidityPolicy.PriceImpact impact = policy.priceImpact(
                state.getCapacity(), state.getNetPlayerBuyQuantity(), signedQuantity
        );
        long averagePrice = applyBasisPoints(state.getReferencePrice(), impact.averageBasisPoints());
        long finalPrice = applyBasisPoints(state.getReferencePrice(), impact.finalBasisPoints());
        return new Execution(Math.max(1L, averagePrice), Math.max(1L, finalPrice), impact.averageBasisPoints());
    }

    public void complete(Player player, String stockKey, long signedQuantity, Execution execution) {
        StockLiquidityState state = requireState(player, stockKey);
        state.applyNetBuyChange(signedQuantity);
        StockPriceHistory latest = priceRepository
                .findFirstByPlayerAndStockKeyOrderByElapsedDaysDescIdDesc(player, stockKey)
                .orElseThrow();
        latest.applyTradePrice(execution.finalPrice());
    }

    private StockLiquidityState requireState(Player player, String stockKey) {
        return stateRepository.findByPlayerAndStockKey(player, stockKey).orElseGet(() -> {
            StockSpec stock = stockCatalog.find(stockKey).orElseThrow();
            return stateRepository.save(new StockLiquidityState(
                    player, stockKey, policy.capacity(stock), currentPrice(player, stock)
            ));
        });
    }

    private long currentPrice(Player player, StockSpec stock) {
        return priceRepository.findFirstByPlayerAndStockKeyOrderByElapsedDaysDescIdDesc(player, stock.key())
                .map(StockPriceHistory::getClosePrice)
                .orElse(stock.basePrice());
    }

    private long applyBasisPoints(long amount, int basisPoints) {
        return BigInteger.valueOf(amount)
                .multiply(BigInteger.valueOf(10_000L + basisPoints))
                .divide(BigInteger.valueOf(10_000L))
                .longValueExact();
    }

    public record Execution(long averagePrice, long finalPrice, int averageImpactBasisPoints) {
    }
}
