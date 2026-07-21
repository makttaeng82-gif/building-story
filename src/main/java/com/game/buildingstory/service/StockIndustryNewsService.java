package com.game.buildingstory.service;

import com.game.buildingstory.domain.Player;
import com.game.buildingstory.domain.StockNewsArticle;
import com.game.buildingstory.repo.StockNewsArticleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.function.Function;
import java.util.stream.Collectors;

/** 업종 사건의 발행, 반복 제한, 가격 효과 소진과 미확인 기사 후속 보도를 담당한다. */
@Service
@Transactional
public class StockIndustryNewsService {
    private static final int PUBLICATION_CHANCE_PERCENT = 18;
    private static final int EVENT_COOLDOWN_DAYS = 365;
    private static final int FAMILY_COOLDOWN_DAYS = 60;
    private static final int INDUSTRY_COOLDOWN_DAYS = 10;
    private static final int ARTICLE_RETENTION_DAYS = 365;
    private static final int MAX_ACTIVE_EVENTS = 2;

    private final Random random = new Random();
    private final StockIndustryNewsCatalog catalog;
    private final StockNewsArticleRepository articleRepository;
    private final ListedCompanyFinancialService financialService;

    public StockIndustryNewsService(
            StockIndustryNewsCatalog catalog,
            StockNewsArticleRepository articleRepository,
            ListedCompanyFinancialService financialService
    ) {
        this.catalog = catalog;
        this.articleRepository = articleRepository;
        this.financialService = financialService;
    }

    /**
     * 한 번의 5일 주가 갱신이 끝난 뒤 기존 효과를 소진하고 후속 기사와 신규 기사를 처리한다.
     * 새 기사는 이미 계산된 현재 봉을 바꾸지 않고 다음 봉부터 영향을 준다.
     */
    public void afterPriceUpdate(Player player) {
        articleRepository.deleteByPlayerAndPublishedElapsedDaysLessThan(
                player,
                Math.max(0, player.getElapsedDays() - ARTICLE_RETENTION_DAYS)
        );
        consumeActivePriceEffects(player);
        resolveDueArticles(player);
        if (activeArticles(player).size() < MAX_ACTIVE_EVENTS
                && random.nextInt(100) < PUBLICATION_CHANCE_PERCENT) {
            publishRandomArticle(player);
        }
    }

    @Transactional(readOnly = true)
    public double activePriceEffectPercent(Player player, String industry) {
        int totalBasisPoints = activeArticles(player).stream()
                .filter(article -> article.getIndustry().equals(industry))
                .mapToInt(StockNewsArticle::getPriceImpactBasisPoints)
                .sum();
        return Math.max(-3.0, Math.min(3.0, totalBasisPoints / 100.0));
    }

    @Transactional(readOnly = true)
    public String activeStatusText(Player player) {
        List<StockNewsArticle> active = activeArticles(player);
        if (active.isEmpty()) {
            return "";
        }
        StockNewsArticle first = active.get(0);
        String suffix = active.size() > 1 ? " 외 " + (active.size() - 1) + "건" : "";
        return first.getIndustry() + " " + first.getDirection().label() + suffix + " 적용중";
    }

    @Transactional(readOnly = true)
    public String activeStatusDirection(Player player) {
        List<StockNewsArticle> active = activeArticles(player);
        return active.isEmpty() ? "flat" : active.get(0).getDirection().cssClass();
    }

    /** 테스트와 운영 점검에서 확률을 거치지 않고 특정 사건을 발행한다. */
    public StockNewsArticle publish(Player player, String eventKey, int variantIndex) {
        StockIndustryNewsDefinition definition = catalog.all().stream()
                .filter(candidate -> candidate.key().equals(eventKey))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 업종 사건입니다: " + eventKey));
        return publish(player, definition, Math.floorMod(variantIndex, definition.variants().size()));
    }

    private void publishRandomArticle(Player player) {
        List<StockNewsArticle> recent = articleRepository
                .findByPlayerAndPublishedElapsedDaysGreaterThanEqualOrderByPublishedElapsedDaysDescIdDesc(
                        player,
                        Math.max(0, player.getElapsedDays() - EVENT_COOLDOWN_DAYS)
                );
        List<StockIndustryNewsDefinition> candidates = catalog.all().stream()
                .filter(definition -> allowed(player, definition, recent))
                .toList();
        if (candidates.isEmpty()) {
            return;
        }
        StockIndustryNewsDefinition definition = candidates.get(random.nextInt(candidates.size()));
        publish(player, definition, random.nextInt(definition.variants().size()));
    }

    private StockNewsArticle publish(Player player, StockIndustryNewsDefinition definition, int variantIndex) {
        int resolutionDelay = 5 * random.nextInt(1, 4);
        StockNewsArticle article = articleRepository.save(new StockNewsArticle(
                player,
                definition,
                definition.variants().get(variantIndex),
                player.getElapsedDays() + resolutionDelay
        ));
        if (definition.certainty().financialEffectImmediate()) {
            financialService.applyIndustryNews(player, definition.industry(), article.getFinancialImpactBasisPoints());
        }
        return article;
    }

    private boolean allowed(
            Player player,
            StockIndustryNewsDefinition definition,
            List<StockNewsArticle> recent
    ) {
        int currentDay = player.getElapsedDays();
        boolean eventRecentlyUsed = recent.stream().anyMatch(article ->
                article.getEventKey().equals(definition.key())
                        && article.getPublishedElapsedDays() >= currentDay - EVENT_COOLDOWN_DAYS);
        boolean familyRecentlyUsed = recent.stream().anyMatch(article ->
                article.getEventFamily().equals(definition.family())
                        && article.getPublishedElapsedDays() >= currentDay - FAMILY_COOLDOWN_DAYS);
        boolean industryRecentlyUsed = recent.stream().anyMatch(article ->
                definition.industry().equals(article.getIndustry())
                        && article.getPublishedElapsedDays() >= currentDay - INDUSTRY_COOLDOWN_DAYS);
        if (eventRecentlyUsed || familyRecentlyUsed || industryRecentlyUsed) {
            return false;
        }

        // 같은 방향의 기사 세 건이 연속되면 후보에서 제외해 뉴스가 한쪽으로만 몰리지 않게 한다.
        List<StockNewsArticle> latestIndependentArticles = recent.stream()
                .filter(article -> article.getParentArticleId() == null)
                .sorted(Comparator.comparingInt(StockNewsArticle::getPublishedElapsedDays).reversed()
                        .thenComparing(StockNewsArticle::getId, Comparator.reverseOrder()))
                .limit(2)
                .toList();
        return latestIndependentArticles.size() < 2
                || latestIndependentArticles.stream().anyMatch(article -> article.getDirection() != definition.direction());
    }

    private void resolveDueArticles(Player player) {
        var definitionsByKey = catalog.all().stream()
                .collect(Collectors.toMap(StockIndustryNewsDefinition::key, Function.identity()));
        for (StockNewsArticle article : articleRepository
                .findByPlayerAndResolvedFalseAndResolutionElapsedDaysLessThanEqual(player, player.getElapsedDays()).stream()
                .filter(article -> article.getCategory() == StockNewsCategory.INDUSTRY)
                .toList()) {
            StockIndustryNewsDefinition definition = definitionsByKey.get(article.getEventKey());
            if (definition == null) {
                article.markResolved();
                continue;
            }
            boolean confirmed = random.nextInt(100) < definition.confirmationChancePercent();
            StockNewsArticle followUp = articleRepository.save(StockNewsArticle.followUp(player, article, confirmed));
            article.markResolved();
            if (confirmed) {
                financialService.applyIndustryNews(player, article.getIndustry(), followUp.getFinancialImpactBasisPoints());
            }
        }
    }

    private void consumeActivePriceEffects(Player player) {
        activeArticles(player).forEach(StockNewsArticle::consumePriceRefresh);
    }

    private List<StockNewsArticle> activeArticles(Player player) {
        return articleRepository.findByPlayerAndRemainingPriceRefreshesGreaterThan(player, 0).stream()
                .filter(article -> article.getCategory() == StockNewsCategory.INDUSTRY)
                .sorted(Comparator.comparingInt(StockNewsArticle::getPublishedElapsedDays).reversed()
                        .thenComparing(StockNewsArticle::getId, Comparator.reverseOrder()))
                .toList();
    }

}
