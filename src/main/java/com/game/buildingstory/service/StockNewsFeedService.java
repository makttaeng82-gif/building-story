package com.game.buildingstory.service;

import com.game.buildingstory.domain.Player;
import com.game.buildingstory.domain.StockNewsArticle;
import com.game.buildingstory.repo.StockNewsArticleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** 업종·기업 등 서로 다른 뉴스 원인을 하나의 최신 기사 피드로 변환한다. */
@Service
@Transactional(readOnly = true)
public class StockNewsFeedService {
    private final StockNewsArticleRepository articleRepository;

    public StockNewsFeedService(StockNewsArticleRepository articleRepository) {
        this.articleRepository = articleRepository;
    }

    public List<StockNewsArticleView> latestArticles(Player player) {
        return articleRepository.findTop20ByPlayerOrderByPublishedElapsedDaysDescIdDesc(player).stream()
                .map(article -> view(player, article))
                .toList();
    }

    @Transactional
    public boolean markRead(Player player, long articleId) {
        return articleRepository.findByIdAndPlayer(articleId, player)
                .map(article -> {
                    article.markRead();
                    return true;
                })
                .orElse(false);
    }

    private StockNewsArticleView view(Player player, StockNewsArticle article) {
        int age = Math.max(0, player.getElapsedDays() - article.getPublishedElapsedDays());
        String dateText = age == 0 ? "오늘" : age == 1 ? "어제" : age + "일 전";
        String effectText = article.getCertainty() == StockNewsCertainty.CONFIRMED
                ? "실적 전망 반영"
                : article.isResolved() ? "후속 보도 완료" : "추가 확인 필요";
        StockNewsCategory category = article.getCategory();
        String scopeText = switch (category) {
            case MARKET -> "종합시장";
            case INDUSTRY -> article.getIndustry() + " 업종";
            case COMPANY -> article.getCompanyName();
        };
        return new StockNewsArticleView(
                article.getId(),
                category.cssClass(),
                category.label(),
                scopeText,
                article.getDirection().cssClass(),
                article.getTitle(),
                article.getSource(),
                dateText,
                article.getFirstParagraph(),
                article.getSecondParagraph(),
                article.getCertainty().label(),
                effectText,
                !article.isRead()
        );
    }
}
