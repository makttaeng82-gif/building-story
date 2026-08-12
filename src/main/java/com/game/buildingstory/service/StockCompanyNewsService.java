package com.game.buildingstory.service;

import com.game.buildingstory.domain.Player;
import com.game.buildingstory.domain.StockNewsArticle;
import com.game.buildingstory.repo.StockNewsArticleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/** 기업 고유 사건의 발행, 단일 종목 가격 효과, 후속 확인과 재무 반영을 담당한다. */
@Service
@Transactional
public class StockCompanyNewsService {
    private static final int PUBLICATION_CHANCE_PERCENT = 6;
    private static final int EVENT_COOLDOWN_DAYS = 365;
    private static final int FAMILY_COOLDOWN_DAYS = 60;
    private static final int COMPANY_COOLDOWN_DAYS = 30;
    private static final int MAX_ACTIVE_EVENTS = 2;

    private final Random random = new Random();
    private final StockCompanyNewsCatalog catalog;
    private final StockNewsArticleRepository articleRepository;
    private final ListedCompanyFinancialService financialService;
    private final StockUniverseService stockUniverseService;

    public StockCompanyNewsService(
            StockCompanyNewsCatalog catalog,
            StockNewsArticleRepository articleRepository,
            ListedCompanyFinancialService financialService,
            StockUniverseService stockUniverseService
    ) {
        this.catalog = catalog;
        this.articleRepository = articleRepository;
        this.financialService = financialService;
        this.stockUniverseService = stockUniverseService;
    }

    /** 새 기업 사건도 현재 봉 계산 뒤 발행해 다음 5일봉부터 적용한다. */
    public void afterPriceUpdate(Player player) {
        activeArticles(player).forEach(StockNewsArticle::consumePriceRefresh);
        resolveDueArticles(player);
        if (activeArticles(player).size() < MAX_ACTIVE_EVENTS
                && random.nextInt(100) < PUBLICATION_CHANCE_PERCENT) {
            publishRandomArticle(player);
        }
    }

    @Transactional(readOnly = true)
    public double activePriceEffectPercent(Player player, String stockKey) {
        int totalBasisPoints = activeArticles(player).stream()
                .filter(article -> stockKey.equals(article.getStockKey()))
                .mapToInt(StockNewsArticle::getPriceImpactBasisPoints)
                .sum();
        return Math.max(-4.0, Math.min(4.0, totalBasisPoints / 100.0));
    }

    /** 테스트와 운영 점검에서 확률을 거치지 않고 특정 사건을 발행한다. */
    public StockNewsArticle publish(Player player, String eventKey, int variantIndex) {
        StockCompanyNewsDefinition definition = catalog.all().stream()
                .filter(candidate -> candidate.key().equals(eventKey))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 기업 사건입니다: " + eventKey));
        return publish(player, definition, Math.floorMod(variantIndex, definition.variants().size()));
    }

    private void publishRandomArticle(Player player) {
        List<StockNewsArticle> recent = recentCompanyArticles(player);
        Set<String> listedStockKeys = stockUniverseService.stocks(player).stream()
                .map(StockSpec::key)
                .collect(Collectors.toSet());
        List<StockCompanyNewsDefinition> candidates = catalog.all().stream()
                .filter(definition -> listedStockKeys.contains(definition.stockKey()))
                .filter(definition -> allowed(player, definition, recent))
                .toList();
        if (candidates.isEmpty()) {
            return;
        }
        StockCompanyNewsDefinition definition = candidates.get(random.nextInt(candidates.size()));
        publish(player, definition, random.nextInt(definition.variants().size()));
    }

    private StockNewsArticle publish(Player player, StockCompanyNewsDefinition definition, int variantIndex) {
        int resolutionDelay = 5 * random.nextInt(1, 4);
        StockNewsArticle article = articleRepository.save(new StockNewsArticle(
                player,
                definition,
                definition.variants().get(variantIndex),
                player.getElapsedDays() + resolutionDelay
        ));
        if (definition.certainty().financialEffectImmediate()) {
            financialService.applyCompanyNews(player, definition.stockKey(), definition.financialEffect());
        }
        return article;
    }

    private boolean allowed(
            Player player,
            StockCompanyNewsDefinition definition,
            List<StockNewsArticle> recent
    ) {
        int currentDay = player.getElapsedDays();
        boolean eventRecentlyUsed = recent.stream().anyMatch(article ->
                article.getEventKey().equals(definition.key())
                        && article.getPublishedElapsedDays() >= currentDay - EVENT_COOLDOWN_DAYS);
        boolean familyRecentlyUsed = recent.stream().anyMatch(article ->
                article.getEventFamily().equals(definition.family())
                        && article.getPublishedElapsedDays() >= currentDay - FAMILY_COOLDOWN_DAYS);
        boolean companyRecentlyUsed = recent.stream().anyMatch(article ->
                definition.stockKey().equals(article.getStockKey())
                        && article.getPublishedElapsedDays() >= currentDay - COMPANY_COOLDOWN_DAYS);
        if (eventRecentlyUsed || familyRecentlyUsed || companyRecentlyUsed) {
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
                .collect(Collectors.toMap(StockCompanyNewsDefinition::key, Function.identity()));
        for (StockNewsArticle article : articleRepository
                .findByPlayerAndResolvedFalseAndResolutionElapsedDaysLessThanEqual(player, player.getElapsedDays()).stream()
                .filter(candidate -> candidate.getCategory() == StockNewsCategory.COMPANY)
                .toList()) {
            StockCompanyNewsDefinition definition = definitionsByKey.get(article.getEventKey());
            if (definition == null) {
                article.markResolved();
                continue;
            }
            boolean confirmed = random.nextInt(100) < definition.confirmationChancePercent();
            articleRepository.save(StockNewsArticle.followUp(player, article, confirmed));
            article.markResolved();
            if (confirmed) {
                financialService.applyCompanyNews(player, definition.stockKey(), definition.financialEffect());
            }
        }
    }

    private List<StockNewsArticle> recentCompanyArticles(Player player) {
        return articleRepository
                .findByPlayerAndPublishedElapsedDaysGreaterThanEqualOrderByPublishedElapsedDaysDescIdDesc(
                        player,
                        Math.max(0, player.getElapsedDays() - EVENT_COOLDOWN_DAYS)
                ).stream()
                .filter(article -> article.getCategory() == StockNewsCategory.COMPANY)
                .toList();
    }

    private List<StockNewsArticle> activeArticles(Player player) {
        return articleRepository.findByPlayerAndRemainingPriceRefreshesGreaterThan(player, 0).stream()
                .filter(article -> article.getCategory() == StockNewsCategory.COMPANY)
                .sorted(Comparator.comparingInt(StockNewsArticle::getPublishedElapsedDays).reversed()
                        .thenComparing(StockNewsArticle::getId, Comparator.reverseOrder()))
                .toList();
    }
}
