package com.game.buildingstory.simulation;

import com.game.buildingstory.service.StockCompanyNewsCatalog;
import com.game.buildingstory.service.StockCompanyNewsDefinition;
import com.game.buildingstory.service.StockCatalog;
import com.game.buildingstory.service.StockIndustryNewsCatalog;
import com.game.buildingstory.service.StockIndustryNewsDefinition;
import com.game.buildingstory.service.StockMarketNewsCatalog;
import com.game.buildingstory.service.StockMarketNewsDefinition;
import com.game.buildingstory.service.StockNewsCertainty;
import com.game.buildingstory.service.StockNewsDirection;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;

/** 운영 뉴스의 발행 확률, 지속시간, 후속 확인과 재무 반영을 DB 없이 재현한다. */
final class StockNewsProcess {
    private static final int EVENT_COOLDOWN_UPDATES = 73;
    private static final int FAMILY_COOLDOWN_UPDATES = 12;
    private static final int INDUSTRY_COOLDOWN_UPDATES = 2;
    private static final int COMPANY_COOLDOWN_UPDATES = 6;

    private final Random random;
    private final List<StockMarketNewsDefinition> marketDefinitions;
    private final List<StockIndustryNewsDefinition> industryDefinitions;
    private final List<StockCompanyNewsDefinition> companyDefinitions;
    private final List<ActiveEffect> activeEffects = new ArrayList<>();
    private final List<PendingResolution> pendingResolutions = new ArrayList<>();
    private final Map<String, Integer> lastEventUpdate = new HashMap<>();
    private final Map<String, Integer> lastFamilyUpdate = new HashMap<>();
    private final Map<String, Integer> lastScopeUpdate = new HashMap<>();
    private final Map<Category, Deque<StockNewsDirection>> recentDirections = new HashMap<>();
    private int marketArticleCount;
    private int industryArticleCount;
    private int companyArticleCount;

    StockNewsProcess(long seed) {
        this.random = new Random(seed ^ 0x51A7C0DEL);
        this.marketDefinitions = new StockMarketNewsCatalog().all().stream()
                .filter(StockMarketNewsDefinition::randomPublication).toList();
        this.industryDefinitions = new StockIndustryNewsCatalog().all();
        this.companyDefinitions = new StockCompanyNewsCatalog(new StockCatalog()).all();
        for (Category category : Category.values()) {
            recentDirections.put(category, new ArrayDeque<>());
        }
    }

    double marketEffectPercent() {
        int basisPoints = activeEffects.stream()
                .filter(effect -> effect.category() == Category.MARKET)
                .mapToInt(ActiveEffect::signedPriceBasisPoints).sum();
        return clamp(basisPoints, -200, 200) / 100.0;
    }

    double industryEffectPercent(String industry) {
        int basisPoints = activeEffects.stream()
                .filter(effect -> effect.category() == Category.INDUSTRY)
                .filter(effect -> effect.scope().equals(industry))
                .mapToInt(ActiveEffect::signedPriceBasisPoints).sum();
        return clamp(basisPoints, -300, 300) / 100.0;
    }

    double companyEffectPercent(String stockKey) {
        int basisPoints = activeEffects.stream()
                .filter(effect -> effect.category() == Category.COMPANY)
                .filter(effect -> effect.scope().equals(stockKey))
                .mapToInt(ActiveEffect::signedPriceBasisPoints).sum();
        return clamp(basisPoints, -400, 400) / 100.0;
    }

    void afterUpdate(
            int update,
            boolean regimeChanged,
            Map<String, StockFundamentalProcess> fundamentals
    ) {
        consumeActiveEffects();
        resolveDue(update, fundamentals);
        if (!regimeChanged && activeCount(Category.MARKET) < 1 && random.nextInt(100) < 8) {
            publishMarket(update);
        }
        if (activeCount(Category.INDUSTRY) < 2 && random.nextInt(100) < 35) {
            publishIndustry(update, fundamentals);
        }
        if (activeCount(Category.COMPANY) < 2 && random.nextInt(100) < 6) {
            publishCompany(update, fundamentals);
        }
    }

    int marketArticleCount() {
        return marketArticleCount;
    }

    int industryArticleCount() {
        return industryArticleCount;
    }

    int companyArticleCount() {
        return companyArticleCount;
    }

    private void publishMarket(int update) {
        List<StockMarketNewsDefinition> candidates = marketDefinitions.stream()
                .filter(definition -> allowed(
                        Category.MARKET, definition.key(), definition.family(), "market",
                        definition.direction(), update, 0
                )).toList();
        if (candidates.isEmpty()) {
            return;
        }
        StockMarketNewsDefinition definition = candidates.get(random.nextInt(candidates.size()));
        registerPublication(Category.MARKET, definition.key(), definition.family(), "market", definition.direction(), update);
        activeEffects.add(new ActiveEffect(
                Category.MARKET, "market", definition.direction().signed(definition.priceImpactBasisPoints()),
                definition.durationRefreshes()
        ));
        scheduleResolution(update, definition);
        marketArticleCount++;
    }

    private void publishIndustry(int update, Map<String, StockFundamentalProcess> fundamentals) {
        List<StockIndustryNewsDefinition> candidates = industryDefinitions.stream()
                .filter(definition -> allowed(
                        Category.INDUSTRY, definition.key(), definition.family(), definition.industry(),
                        definition.direction(), update, INDUSTRY_COOLDOWN_UPDATES
                )).toList();
        if (candidates.isEmpty()) {
            return;
        }
        StockIndustryNewsDefinition definition = candidates.get(random.nextInt(candidates.size()));
        registerPublication(
                Category.INDUSTRY, definition.key(), definition.family(), definition.industry(),
                definition.direction(), update
        );
        activeEffects.add(new ActiveEffect(
                Category.INDUSTRY, definition.industry(),
                definition.direction().signed(definition.priceImpactBasisPoints()), definition.durationRefreshes()
        ));
        if (definition.certainty().financialEffectImmediate()) {
            applyIndustryFinancial(definition, fundamentals);
        } else {
            scheduleResolution(update, definition);
        }
        industryArticleCount++;
    }

    private void publishCompany(int update, Map<String, StockFundamentalProcess> fundamentals) {
        List<StockCompanyNewsDefinition> candidates = companyDefinitions.stream()
                .filter(definition -> allowed(
                        Category.COMPANY, definition.key(), definition.family(), definition.stockKey(),
                        definition.direction(), update, COMPANY_COOLDOWN_UPDATES
                )).toList();
        if (candidates.isEmpty()) {
            return;
        }
        StockCompanyNewsDefinition definition = candidates.get(random.nextInt(candidates.size()));
        registerPublication(
                Category.COMPANY, definition.key(), definition.family(), definition.stockKey(),
                definition.direction(), update
        );
        activeEffects.add(new ActiveEffect(
                Category.COMPANY, definition.stockKey(),
                definition.direction().signed(definition.priceImpactBasisPoints()), definition.durationRefreshes()
        ));
        if (definition.certainty().financialEffectImmediate()) {
            fundamentals.get(definition.stockKey()).applyCompanyFinancialImpact(definition.financialEffect());
        } else {
            scheduleResolution(update, definition);
        }
        companyArticleCount++;
    }

    private boolean allowed(
            Category category,
            String eventKey,
            String family,
            String scope,
            StockNewsDirection direction,
            int update,
            int scopeCooldown
    ) {
        if (recentlyUsed(lastEventUpdate, category + ":" + eventKey, update, EVENT_COOLDOWN_UPDATES)
                || recentlyUsed(lastFamilyUpdate, category + ":" + family, update, FAMILY_COOLDOWN_UPDATES)
                || scopeCooldown > 0 && recentlyUsed(lastScopeUpdate, category + ":" + scope, update, scopeCooldown)) {
            return false;
        }
        Deque<StockNewsDirection> latest = recentDirections.get(category);
        return latest.size() < 2 || latest.stream().anyMatch(previous -> previous != direction);
    }

    private void registerPublication(
            Category category,
            String eventKey,
            String family,
            String scope,
            StockNewsDirection direction,
            int update
    ) {
        lastEventUpdate.put(category + ":" + eventKey, update);
        lastFamilyUpdate.put(category + ":" + family, update);
        lastScopeUpdate.put(category + ":" + scope, update);
        Deque<StockNewsDirection> latest = recentDirections.get(category);
        latest.addFirst(direction);
        while (latest.size() > 2) {
            latest.removeLast();
        }
    }

    private boolean recentlyUsed(Map<String, Integer> updates, String key, int update, int cooldown) {
        Integer previous = updates.get(key);
        return previous != null && previous >= update - cooldown;
    }

    private void scheduleResolution(int update, StockMarketNewsDefinition definition) {
        if (definition.certainty() != StockNewsCertainty.CONFIRMED) {
            pendingResolutions.add(new PendingResolution(
                    Category.MARKET, "market", definition.direction(),
                    definition.priceImpactBasisPoints(), 0, null,
                    definition.confirmationChancePercent(), update + random.nextInt(1, 4)
            ));
        }
    }

    private void scheduleResolution(int update, StockIndustryNewsDefinition definition) {
        pendingResolutions.add(new PendingResolution(
                Category.INDUSTRY, definition.industry(), definition.direction(),
                definition.priceImpactBasisPoints(), definition.financialImpactBasisPoints(), null,
                definition.confirmationChancePercent(), update + random.nextInt(1, 4)
        ));
    }

    private void scheduleResolution(int update, StockCompanyNewsDefinition definition) {
        pendingResolutions.add(new PendingResolution(
                Category.COMPANY, definition.stockKey(), definition.direction(),
                definition.priceImpactBasisPoints(), 0, definition,
                definition.confirmationChancePercent(), update + random.nextInt(1, 4)
        ));
    }

    private void resolveDue(int update, Map<String, StockFundamentalProcess> fundamentals) {
        Iterator<PendingResolution> iterator = pendingResolutions.iterator();
        while (iterator.hasNext()) {
            PendingResolution pending = iterator.next();
            if (pending.resolutionUpdate() > update) {
                continue;
            }
            boolean confirmed = random.nextInt(100) < pending.confirmationChancePercent();
            int followUpBasisPoints;
            if (confirmed && pending.category() == Category.INDUSTRY) {
                followUpBasisPoints = pending.direction().signed(pending.financialBasisPoints());
                fundamentals.values().stream()
                        .filter(process -> processIndustry(process).equals(pending.scope()))
                        .forEach(process -> process.applyIndustryFinancialImpact(followUpBasisPoints));
            } else if (confirmed && pending.category() == Category.COMPANY) {
                followUpBasisPoints = pending.direction().signed(pending.priceBasisPoints());
                fundamentals.get(pending.scope()).applyCompanyFinancialImpact(pending.companyDefinition().financialEffect());
            } else if (confirmed) {
                followUpBasisPoints = pending.direction().signed(pending.priceBasisPoints());
            } else {
                followUpBasisPoints = -pending.direction().signed(Math.max(20, pending.priceBasisPoints()));
            }
            activeEffects.add(new ActiveEffect(pending.category(), pending.scope(), followUpBasisPoints, 1));
            iterator.remove();
        }
    }

    private void applyIndustryFinancial(
            StockIndustryNewsDefinition definition,
            Map<String, StockFundamentalProcess> fundamentals
    ) {
        int signedImpact = definition.direction().signed(definition.financialImpactBasisPoints());
        fundamentals.values().stream()
                .filter(process -> processIndustry(process).equals(definition.industry()))
                .forEach(process -> process.applyIndustryFinancialImpact(signedImpact));
    }

    private String processIndustry(StockFundamentalProcess process) {
        return process.industry();
    }

    private void consumeActiveEffects() {
        List<ActiveEffect> remaining = new ArrayList<>();
        for (ActiveEffect effect : activeEffects) {
            if (effect.remainingRefreshes() > 1) {
                remaining.add(new ActiveEffect(
                        effect.category(), effect.scope(), effect.signedPriceBasisPoints(),
                        effect.remainingRefreshes() - 1
                ));
            }
        }
        activeEffects.clear();
        activeEffects.addAll(remaining);
    }

    private long activeCount(Category category) {
        return activeEffects.stream().filter(effect -> effect.category() == category).count();
    }

    private static int clamp(int value, int minimum, int maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }

    private enum Category {
        MARKET,
        INDUSTRY,
        COMPANY
    }

    private record ActiveEffect(
            Category category,
            String scope,
            int signedPriceBasisPoints,
            int remainingRefreshes
    ) {
    }

    private record PendingResolution(
            Category category,
            String scope,
            StockNewsDirection direction,
            int priceBasisPoints,
            int financialBasisPoints,
            StockCompanyNewsDefinition companyDefinition,
            int confirmationChancePercent,
            int resolutionUpdate
    ) {
    }
}
