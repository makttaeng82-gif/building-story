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

/** 전체 시장 사건의 발행, 가격 효과, 경기 국면 전환 기사와 후속 진위 보도를 담당한다. */
@Service
@Transactional
public class StockMarketNewsService {
    private static final int PUBLICATION_CHANCE_PERCENT = 8;
    private static final int EVENT_COOLDOWN_DAYS = 365;
    private static final int FAMILY_COOLDOWN_DAYS = 60;
    private static final int MAX_ACTIVE_EVENTS = 1;

    private final Random random = new Random();
    private final StockMarketNewsCatalog catalog;
    private final StockNewsArticleRepository articleRepository;

    public StockMarketNewsService(
            StockMarketNewsCatalog catalog,
            StockNewsArticleRepository articleRepository
    ) {
        this.catalog = catalog;
        this.articleRepository = articleRepository;
    }

    /**
     * 현재 봉에 사용한 시장 효과를 먼저 한 회 소진한다.
     * 국면 전환 기사는 국면 수익률을 설명만 하며 가격 효과를 추가하지 않는다.
     */
    public void afterPriceUpdate(Player player, StockMarketRegimeService.RegimeTransition transition) {
        activeArticles(player).forEach(StockNewsArticle::consumePriceRefresh);
        resolveDueArticles(player);
        if (transition.changed()) {
            publishRegimeTransition(player, transition.current());
            return;
        }
        if (activeArticles(player).size() < MAX_ACTIVE_EVENTS
                && random.nextInt(100) < PUBLICATION_CHANCE_PERCENT) {
            publishRandomArticle(player);
        }
    }

    @Transactional(readOnly = true)
    public double activePriceEffectPercent(Player player) {
        int totalBasisPoints = activeArticles(player).stream()
                .mapToInt(StockNewsArticle::getPriceImpactBasisPoints)
                .sum();
        return Math.max(-2.0, Math.min(2.0, totalBasisPoints / 100.0));
    }

    @Transactional(readOnly = true)
    public String activeStatusText(Player player) {
        return activeArticles(player).stream().findFirst()
                .map(article -> "시장 " + article.getDirection().label() + " 적용중")
                .orElse("");
    }

    @Transactional(readOnly = true)
    public String activeStatusDirection(Player player) {
        return activeArticles(player).stream().findFirst()
                .map(article -> article.getDirection().cssClass())
                .orElse("flat");
    }

    /** 테스트와 운영 점검에서 확률을 거치지 않고 특정 시장 사건을 발행한다. */
    public StockNewsArticle publish(Player player, String eventKey, int variantIndex) {
        StockMarketNewsDefinition definition = catalog.require(eventKey);
        return publish(player, definition, Math.floorMod(variantIndex, definition.variants().size()));
    }

    private void publishRegimeTransition(Player player, StockMarketRegime regime) {
        String key = "market-regime-" + regime.name().toLowerCase(java.util.Locale.ROOT);
        StockMarketNewsDefinition definition = catalog.require(key);
        publish(player, definition, Math.floorMod(player.getElapsedDays(), definition.variants().size()));
    }

    private void publishRandomArticle(Player player) {
        List<StockNewsArticle> recent = recentMarketArticles(player);
        List<StockMarketNewsDefinition> candidates = catalog.all().stream()
                .filter(StockMarketNewsDefinition::randomPublication)
                .filter(definition -> allowed(player, definition, recent))
                .toList();
        if (candidates.isEmpty()) {
            return;
        }
        StockMarketNewsDefinition definition = candidates.get(random.nextInt(candidates.size()));
        publish(player, definition, random.nextInt(definition.variants().size()));
    }

    private StockNewsArticle publish(Player player, StockMarketNewsDefinition definition, int variantIndex) {
        int resolutionDelay = 5 * random.nextInt(1, 4);
        return articleRepository.save(new StockNewsArticle(
                player,
                definition,
                definition.variants().get(variantIndex),
                player.getElapsedDays() + resolutionDelay
        ));
    }

    private boolean allowed(
            Player player,
            StockMarketNewsDefinition definition,
            List<StockNewsArticle> recent
    ) {
        int currentDay = player.getElapsedDays();
        boolean eventRecentlyUsed = recent.stream().anyMatch(article ->
                article.getEventKey().equals(definition.key())
                        && article.getPublishedElapsedDays() >= currentDay - EVENT_COOLDOWN_DAYS);
        boolean familyRecentlyUsed = recent.stream().anyMatch(article ->
                article.getEventFamily().equals(definition.family())
                        && article.getPublishedElapsedDays() >= currentDay - FAMILY_COOLDOWN_DAYS);
        if (eventRecentlyUsed || familyRecentlyUsed) {
            return false;
        }

        List<StockNewsArticle> latest = recent.stream()
                .filter(article -> article.getParentArticleId() == null)
                .sorted(Comparator.comparingInt(StockNewsArticle::getPublishedElapsedDays).reversed()
                        .thenComparing(StockNewsArticle::getId, Comparator.reverseOrder()))
                .limit(2)
                .toList();
        return latest.size() < 2
                || latest.stream().anyMatch(article -> article.getDirection() != definition.direction());
    }

    private void resolveDueArticles(Player player) {
        var definitionsByKey = catalog.all().stream()
                .collect(Collectors.toMap(StockMarketNewsDefinition::key, Function.identity()));
        for (StockNewsArticle article : articleRepository
                .findByPlayerAndResolvedFalseAndResolutionElapsedDaysLessThanEqual(player, player.getElapsedDays()).stream()
                .filter(candidate -> candidate.getCategory() == StockNewsCategory.MARKET)
                .toList()) {
            StockMarketNewsDefinition definition = definitionsByKey.get(article.getEventKey());
            if (definition == null) {
                article.markResolved();
                continue;
            }
            boolean confirmed = random.nextInt(100) < definition.confirmationChancePercent();
            articleRepository.save(StockNewsArticle.followUp(player, article, confirmed));
            article.markResolved();
        }
    }

    private List<StockNewsArticle> recentMarketArticles(Player player) {
        return articleRepository
                .findByPlayerAndPublishedElapsedDaysGreaterThanEqualOrderByPublishedElapsedDaysDescIdDesc(
                        player,
                        Math.max(0, player.getElapsedDays() - EVENT_COOLDOWN_DAYS)
                ).stream()
                .filter(article -> article.getCategory() == StockNewsCategory.MARKET)
                .toList();
    }

    private List<StockNewsArticle> activeArticles(Player player) {
        return articleRepository.findByPlayerAndRemainingPriceRefreshesGreaterThan(player, 0).stream()
                .filter(article -> article.getCategory() == StockNewsCategory.MARKET)
                .sorted(Comparator.comparingInt(StockNewsArticle::getPublishedElapsedDays).reversed()
                        .thenComparing(StockNewsArticle::getId, Comparator.reverseOrder()))
                .toList();
    }
}
