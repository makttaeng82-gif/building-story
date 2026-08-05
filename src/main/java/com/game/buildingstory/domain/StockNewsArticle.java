package com.game.buildingstory.domain;

import com.game.buildingstory.service.StockIndustryNewsDefinition;
import com.game.buildingstory.service.StockCompanyNewsDefinition;
import com.game.buildingstory.service.StockMarketNewsDefinition;
import com.game.buildingstory.service.StockNewsCertainty;
import com.game.buildingstory.service.StockNewsCategory;
import com.game.buildingstory.service.StockNewsDirection;
import jakarta.persistence.Entity;
import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * 플레이어의 게임 시간에 실제 발행된 주식 뉴스를 보관한다.
 * 카탈로그 문구를 매번 다시 조립하지 않고 발행 시점의 제목과 본문을 저장하므로,
 * 새로고침하거나 나중에 과거 기사를 열어도 내용이 바뀌지 않는다.
 */
@Entity
@Table(name = "stock_news_article")
public class StockNewsArticle {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Player player;

    private String eventKey;
    private String eventFamily;
    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "varchar(32)")
    private StockNewsCategory category;
    private String industry;
    private String stockKey;
    private String companyName;
    @Enumerated(EnumType.STRING)
    private StockNewsDirection direction;
    @Enumerated(EnumType.STRING)
    private StockNewsCertainty certainty;
    private String source;
    private String title;
    @Lob
    private String firstParagraph;
    @Lob
    private String secondParagraph;
    private int publishedMonth;
    private int publishedDay;
    private int publishedElapsedDays;
    private int priceImpactBasisPoints;
    private int financialImpactBasisPoints;
    private int remainingPriceRefreshes;
    private Integer resolutionElapsedDays;
    private Boolean resolved = false;
    private Long parentArticleId;
    private Boolean readByPlayer = false;

    protected StockNewsArticle() {
    }

    public StockNewsArticle(
            Player player,
            StockIndustryNewsDefinition definition,
            StockIndustryNewsDefinition.StockNewsVariant variant,
            int resolutionElapsedDays
    ) {
        this.player = player;
        this.eventKey = definition.key();
        this.eventFamily = definition.family();
        this.category = StockNewsCategory.INDUSTRY;
        this.industry = definition.industry();
        this.direction = definition.direction();
        this.certainty = definition.certainty();
        this.source = definition.source();
        this.title = variant.title();
        this.firstParagraph = variant.firstParagraph();
        this.secondParagraph = variant.secondParagraph();
        this.publishedMonth = player.getMonth();
        this.publishedDay = player.getDay();
        this.publishedElapsedDays = player.getElapsedDays();
        this.priceImpactBasisPoints = definition.direction().signed(definition.priceImpactBasisPoints());
        this.financialImpactBasisPoints = definition.direction().signed(definition.financialImpactBasisPoints());
        this.remainingPriceRefreshes = definition.durationRefreshes();
        this.resolutionElapsedDays = definition.certainty() == StockNewsCertainty.CONFIRMED
                ? null : resolutionElapsedDays;
        this.resolved = definition.certainty() == StockNewsCertainty.CONFIRMED;
    }

    public StockNewsArticle(
            Player player,
            StockCompanyNewsDefinition definition,
            StockCompanyNewsDefinition.StockNewsVariant variant,
            int resolutionElapsedDays
    ) {
        this.player = player;
        this.eventKey = definition.key();
        this.eventFamily = definition.family();
        this.category = StockNewsCategory.COMPANY;
        this.industry = definition.industry();
        this.stockKey = definition.stockKey();
        this.companyName = definition.companyName();
        this.direction = definition.direction();
        this.certainty = definition.certainty();
        this.source = definition.source();
        this.title = variant.title();
        this.firstParagraph = variant.firstParagraph();
        this.secondParagraph = variant.secondParagraph();
        this.publishedMonth = player.getMonth();
        this.publishedDay = player.getDay();
        this.publishedElapsedDays = player.getElapsedDays();
        this.priceImpactBasisPoints = definition.direction().signed(definition.priceImpactBasisPoints());
        this.financialImpactBasisPoints = definition.financialEffect().revenueImpactBasisPoints();
        this.remainingPriceRefreshes = definition.durationRefreshes();
        this.resolutionElapsedDays = definition.certainty() == StockNewsCertainty.CONFIRMED
                ? null : resolutionElapsedDays;
        this.resolved = definition.certainty() == StockNewsCertainty.CONFIRMED;
    }

    public StockNewsArticle(
            Player player,
            StockMarketNewsDefinition definition,
            StockMarketNewsDefinition.StockNewsVariant variant,
            int resolutionElapsedDays
    ) {
        this.player = player;
        this.eventKey = definition.key();
        this.eventFamily = definition.family();
        this.category = StockNewsCategory.MARKET;
        this.direction = definition.direction();
        this.certainty = definition.certainty();
        this.source = definition.source();
        this.title = variant.title();
        this.firstParagraph = variant.firstParagraph();
        this.secondParagraph = variant.secondParagraph();
        this.publishedMonth = player.getMonth();
        this.publishedDay = player.getDay();
        this.publishedElapsedDays = player.getElapsedDays();
        this.priceImpactBasisPoints = definition.direction().signed(definition.priceImpactBasisPoints());
        this.financialImpactBasisPoints = 0;
        this.remainingPriceRefreshes = definition.durationRefreshes();
        this.resolutionElapsedDays = definition.certainty() == StockNewsCertainty.CONFIRMED
                ? null : resolutionElapsedDays;
        this.resolved = definition.certainty() == StockNewsCertainty.CONFIRMED;
    }

    private StockNewsArticle(
            Player player,
            StockNewsArticle original,
            boolean confirmed,
            int priceCorrectionBasisPoints,
            String title,
            String firstParagraph,
            String secondParagraph
    ) {
        this.player = player;
        this.eventKey = original.eventKey + (confirmed ? ":confirmed" : ":denied");
        this.eventFamily = original.eventFamily;
        this.category = original.getCategory();
        this.industry = original.industry;
        this.stockKey = original.stockKey;
        this.companyName = original.companyName;
        this.direction = confirmed ? original.direction : opposite(original.direction);
        this.certainty = StockNewsCertainty.CONFIRMED;
        this.source = confirmed ? "공식 발표" : "관계기관 확인";
        this.title = title;
        this.firstParagraph = firstParagraph;
        this.secondParagraph = secondParagraph;
        this.publishedMonth = player.getMonth();
        this.publishedDay = player.getDay();
        this.publishedElapsedDays = player.getElapsedDays();
        this.priceImpactBasisPoints = confirmed
                ? (original.getCategory() == StockNewsCategory.INDUSTRY
                        ? original.financialImpactBasisPoints
                        : original.priceImpactBasisPoints)
                : opposite(original.direction).signed(priceCorrectionBasisPoints);
        this.financialImpactBasisPoints = confirmed ? original.financialImpactBasisPoints : 0;
        this.remainingPriceRefreshes = 1;
        this.resolved = true;
        this.parentArticleId = original.id;
    }

    public static StockNewsArticle followUp(Player player, StockNewsArticle original, boolean confirmed) {
        String target = switch (original.getCategory()) {
            case MARKET -> "전체 시장";
            case INDUSTRY -> original.industry + " 업종";
            case COMPANY -> original.companyName;
        };
        if (confirmed) {
            return new StockNewsArticle(
                    player, original, true, 0,
                    original.title + " · 사실로 확인",
                    "앞서 제기된 " + target + " 관련 관측이 공식 자료를 통해 확인됐다.",
                    "최초 보도에서 예상한 방향의 변화가 다음 실적 전망에 반영될 예정이다."
            );
        }
        return new StockNewsArticle(
                player, original, false, Math.max(20, Math.abs(original.priceImpactBasisPoints)),
                original.title + " · 근거 부족으로 판명",
                "앞서 시장에 알려진 " + target + " 관련 내용은 확인 결과 뚜렷한 근거가 없는 것으로 나타났다.",
                "실적 전망은 변경되지 않으며, 소문으로 움직였던 투자 심리는 일부 되돌아갈 가능성이 있다."
        );
    }

    public void consumePriceRefresh() {
        if (remainingPriceRefreshes > 0) {
            remainingPriceRefreshes--;
        }
    }

    public void markResolved() {
        resolved = true;
    }

    public void markRead() {
        readByPlayer = true;
    }

    private static StockNewsDirection opposite(StockNewsDirection direction) {
        return switch (direction) {
            case POSITIVE -> StockNewsDirection.NEGATIVE;
            case NEGATIVE -> StockNewsDirection.POSITIVE;
            case NEUTRAL -> StockNewsDirection.NEUTRAL;
        };
    }

    public Long getId() { return id; }
    public Player getPlayer() { return player; }
    public String getEventKey() { return eventKey; }
    public String getEventFamily() { return eventFamily; }
    public StockNewsCategory getCategory() { return category == null ? StockNewsCategory.INDUSTRY : category; }
    public String getIndustry() { return industry; }
    public String getStockKey() { return stockKey; }
    public String getCompanyName() { return companyName; }
    public StockNewsDirection getDirection() { return direction; }
    public StockNewsCertainty getCertainty() { return certainty; }
    public String getSource() { return source; }
    public String getTitle() { return title; }
    public String getFirstParagraph() { return firstParagraph; }
    public String getSecondParagraph() { return secondParagraph; }
    public int getPublishedMonth() { return publishedMonth; }
    public int getPublishedDay() { return publishedDay; }
    public int getPublishedElapsedDays() { return publishedElapsedDays; }
    public int getPriceImpactBasisPoints() { return priceImpactBasisPoints; }
    public int getFinancialImpactBasisPoints() { return financialImpactBasisPoints; }
    public int getRemainingPriceRefreshes() { return remainingPriceRefreshes; }
    public Integer getResolutionElapsedDays() { return resolutionElapsedDays; }
    public boolean isResolved() { return Boolean.TRUE.equals(resolved); }
    public Long getParentArticleId() { return parentArticleId; }
    public boolean isRead() { return Boolean.TRUE.equals(readByPlayer); }
}
