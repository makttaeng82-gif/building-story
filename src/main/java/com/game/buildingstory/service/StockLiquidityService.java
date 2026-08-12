package com.game.buildingstory.service;

import com.game.buildingstory.domain.ListedCompany;
import com.game.buildingstory.domain.Player;
import com.game.buildingstory.domain.StockLiquidityState;
import com.game.buildingstory.domain.StockPriceHistory;
import com.game.buildingstory.repo.StockLiquidityStateRepository;
import com.game.buildingstory.repo.StockPriceHistoryRepository;
import com.game.buildingstory.repo.ListedCompanyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigInteger;

/** 시점별 거래 가능 물량을 관리하고 시장가 주문의 평균 체결가와 최종 호가를 계산한다. */
@Service
@Transactional
public class StockLiquidityService {
    private final StockUniverseService stockUniverseService;
    private final StockLiquidityPolicy policy;
    private final StockLiquidityStateRepository stateRepository;
    private final StockPriceHistoryRepository priceRepository;
    private final ListedCompanyRepository listedCompanyRepository;

    public StockLiquidityService(
            StockUniverseService stockUniverseService,
            StockLiquidityPolicy policy,
            StockLiquidityStateRepository stateRepository,
            StockPriceHistoryRepository priceRepository,
            ListedCompanyRepository listedCompanyRepository
    ) {
        this.stockUniverseService = stockUniverseService;
        this.policy = policy;
        this.stateRepository = stateRepository;
        this.priceRepository = priceRepository;
        this.listedCompanyRepository = listedCompanyRepository;
    }

    public void ensureInitialized(Player player) {
        for (StockSpec stock : stockUniverseService.stocks(player)) {
            long currentCapacity = capacity(player, stock);
            stateRepository.findByPlayerAndStockKey(player, stock.key())
                    .ifPresentOrElse(
                            state -> state.updateCapacity(currentCapacity),
                            () -> stateRepository.save(new StockLiquidityState(
                                    player, stock.key(), currentCapacity, currentPrice(player, stock)
                            ))
                    );
        }
    }

    /** 모든 종목의 새 5일 캔들이 생성된 직후 유동성과 주문 충격 기준가를 초기화한다. */
    public void refreshAll(Player player) {
        ensureInitialized(player);
        for (StockLiquidityState state : stateRepository.findByPlayer(player)) {
            StockSpec stock = stockUniverseService.find(player, state.getStockKey()).orElseThrow();
            state.refresh(capacity(player, stock), currentPrice(player, stock), player.getElapsedDays());
        }
    }

    /** 배당락처럼 5일 갱신 밖에서 기준가격이 바뀐 경우 해당 종목의 주문 누적 상태를 재기준화한다. */
    public void rebase(Player player, String stockKey) {
        StockSpec stock = stockUniverseService.find(player, stockKey).orElseThrow();
        StockLiquidityState state = requireState(player, stockKey);
        state.refresh(capacity(player, stock), currentPrice(player, stock), player.getElapsedDays());
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
            StockSpec stock = stockUniverseService.find(player, stockKey).orElseThrow();
            return stateRepository.save(new StockLiquidityState(
                    player, stockKey, capacity(player, stock), currentPrice(player, stock)
            ));
        });
    }

    private long capacity(Player player, StockSpec stock) {
        long tradableShares = listedCompanyRepository.findByPlayerAndStockKey(player, stock.key())
                .map(company -> Math.max(1L, company.getIssuedShares() - company.getFounderShares()))
                .orElse(stock.issuedShares());
        return policy.capacity(stock, tradableShares);
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
