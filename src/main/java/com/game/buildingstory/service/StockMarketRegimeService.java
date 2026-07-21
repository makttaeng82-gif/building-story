package com.game.buildingstory.service;

import com.game.buildingstory.domain.Player;
import com.game.buildingstory.domain.StockMarketRegimeState;
import com.game.buildingstory.repo.StockMarketRegimeStateRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Random;

@Service
@Transactional
public class StockMarketRegimeService {
    private final StockMarketRegimeStateRepository repository;
    private final Random random = new Random();

    public StockMarketRegimeService(StockMarketRegimeStateRepository repository) {
        this.repository = repository;
    }

    public StockMarketRegimeState ensureInitialized(Player player) {
        return repository.findByPlayer(player).orElseGet(() ->
                repository.save(new StockMarketRegimeState(player, StockMarketRegime.NEUTRAL.name(), duration())));
    }

    public MarketPulse currentPulse(Player player) {
        StockMarketRegime regime = currentRegime(player);
        double effect = switch (regime) {
            case EXPANSION -> 0.25 + random.nextDouble(-0.50, 0.51);
            case NEUTRAL -> 0.09 + random.nextDouble(-0.50, 0.51);
            case RECESSION -> -0.25 + random.nextDouble(-0.70, 0.71);
        };
        return new MarketPulse(regime, effect);
    }

    public RegimeTransition advance(Player player) {
        StockMarketRegimeState state = ensureInitialized(player);
        StockMarketRegime current = StockMarketRegime.valueOf(state.getRegime());
        state.advance(nextRegime(current).name(), duration());
        StockMarketRegime next = StockMarketRegime.valueOf(state.getRegime());
        return new RegimeTransition(current, next, current != next);
    }

    /** 이번 5일 가격 경로에 사용한 국면을 분기 실적용 누적값에 기록한다. */
    public void recordQuarterExposure(Player player) {
        ensureInitialized(player).recordQuarterExposure();
    }

    /** 분기 결산이 누적 경기 노출을 한 번만 사용하도록 읽기와 초기화를 함께 수행한다. */
    public RegimeExposure consumeQuarterExposure(Player player) {
        StockMarketRegimeState state = ensureInitialized(player);
        RegimeExposure exposure = new RegimeExposure(
                state.getQuarterExpansionUpdates(),
                state.getQuarterNeutralUpdates(),
                state.getQuarterRecessionUpdates()
        );
        state.clearQuarterExposure();
        return exposure;
    }

    @Transactional(readOnly = true)
    public StockMarketRegime currentRegime(Player player) {
        return repository.findByPlayer(player)
                .map(state -> StockMarketRegime.valueOf(state.getRegime()))
                .orElse(StockMarketRegime.NEUTRAL);
    }

    private StockMarketRegime nextRegime(StockMarketRegime current) {
        int roll = random.nextInt(100);
        return switch (current) {
            case EXPANSION -> roll < 55 ? StockMarketRegime.EXPANSION
                    : roll < 90 ? StockMarketRegime.NEUTRAL : StockMarketRegime.RECESSION;
            case NEUTRAL -> roll < 30 ? StockMarketRegime.EXPANSION
                    : roll < 70 ? StockMarketRegime.NEUTRAL : StockMarketRegime.RECESSION;
            case RECESSION -> roll < 10 ? StockMarketRegime.EXPANSION
                    : roll < 45 ? StockMarketRegime.NEUTRAL : StockMarketRegime.RECESSION;
        };
    }

    private int duration() {
        return random.nextInt(12, 25);
    }

    public record MarketPulse(StockMarketRegime regime, double effectPercent) {
    }

    public record RegimeTransition(StockMarketRegime previous, StockMarketRegime current, boolean changed) {
    }

    public record RegimeExposure(int expansionUpdates, int neutralUpdates, int recessionUpdates) {
        public int totalUpdates() {
            return expansionUpdates + neutralUpdates + recessionUpdates;
        }
    }
}
