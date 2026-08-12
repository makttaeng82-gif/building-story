package com.game.buildingstory.service;

import com.game.buildingstory.domain.ListedCompany;
import com.game.buildingstory.domain.NpcCompanyListing;
import com.game.buildingstory.domain.NpcCompanyListingStage;
import com.game.buildingstory.domain.Player;
import com.game.buildingstory.domain.StockNewsArticle;
import com.game.buildingstory.domain.StockPriceHistory;
import com.game.buildingstory.repo.NpcCompanyListingRepository;
import com.game.buildingstory.repo.StockNewsArticleRepository;
import com.game.buildingstory.repo.StockPriceHistoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigInteger;
import java.util.Random;

/** 16개 고정 NPC 후보의 공개, 심사, 공모가 확정과 거래소 상장을 진행한다. */
@Service
@Transactional
public class NpcCompanyListingService {
    private static final int ANNOUNCEMENT_LEAD_DAYS = 120;
    private static final int INFORMATION_LEAD_DAYS = 75;
    private static final int REVIEW_LEAD_DAYS = 31;
    private static final int OFFER_LEAD_DAYS = 30;

    private final NpcIpoCandidateCatalog candidateCatalog;
    private final StockCatalog stockCatalog;
    private final NpcCompanyListingRepository listingRepository;
    private final ListedCompanyService listedCompanyService;
    private final ListedCompanyFinancialService financialService;
    private final ListedCompanyValuationService valuationService;
    private final StockPriceHistoryRepository priceRepository;
    private final StockNewsArticleRepository newsRepository;
    private final StockMarketRegimeService marketRegimeService;
    private final StockMarketNewsService marketNewsService;
    private final StockIndustryNewsService industryNewsService;
    private final NpcIpoFirstCandleModel firstCandleModel;
    private final StockLiquidityService liquidityService;
    private final NpcIpoSubscriptionService subscriptionService;

    public NpcCompanyListingService(
            NpcIpoCandidateCatalog candidateCatalog,
            StockCatalog stockCatalog,
            NpcCompanyListingRepository listingRepository,
            ListedCompanyService listedCompanyService,
            ListedCompanyFinancialService financialService,
            ListedCompanyValuationService valuationService,
            StockPriceHistoryRepository priceRepository,
            StockNewsArticleRepository newsRepository,
            StockMarketRegimeService marketRegimeService,
            StockMarketNewsService marketNewsService,
            StockIndustryNewsService industryNewsService,
            NpcIpoFirstCandleModel firstCandleModel,
            StockLiquidityService liquidityService,
            NpcIpoSubscriptionService subscriptionService
    ) {
        this.candidateCatalog = candidateCatalog;
        this.stockCatalog = stockCatalog;
        this.listingRepository = listingRepository;
        this.listedCompanyService = listedCompanyService;
        this.financialService = financialService;
        this.valuationService = valuationService;
        this.priceRepository = priceRepository;
        this.newsRepository = newsRepository;
        this.marketRegimeService = marketRegimeService;
        this.marketNewsService = marketNewsService;
        this.industryNewsService = industryNewsService;
        this.firstCandleModel = firstCandleModel;
        this.liquidityService = liquidityService;
        this.subscriptionService = subscriptionService;
    }

    public void process(Player player) {
        if (!player.isStockContentUnlocked()) {
            return;
        }
        ensureScheduled(player);
        for (NpcCompanyListing listing : listingRepository.findByPlayerOrderByTargetElapsedDayAsc(player)) {
            advanceOneStage(player, listing);
        }
    }

    private void ensureScheduled(Player player) {
        if (listingRepository.countByPlayer(player) > 0) {
            return;
        }
        int unlockDay = player.getStockUnlockAvailableDay();
        int scheduleBaseDay = player.getElapsedDays() - unlockDay > 30
                ? player.getElapsedDays()
                : unlockDay;
        candidateCatalog.all().stream()
                .map(candidate -> new NpcCompanyListing(
                        player,
                        candidate.stockKey(),
                        candidateCatalog.targetElapsedDay(scheduleBaseDay, candidate)
                ))
                .forEach(listingRepository::save);
    }

    private void advanceOneStage(Player player, NpcCompanyListing listing) {
        int day = player.getElapsedDays();
        switch (listing.getStage()) {
            case SCHEDULED -> {
                if (day >= listing.getTargetElapsedDay() - ANNOUNCEMENT_LEAD_DAYS) {
                    announce(player, listing);
                }
            }
            case ANNOUNCED -> {
                if (day >= listing.getTargetElapsedDay() - INFORMATION_LEAD_DAYS) {
                    publishInformation(player, listing);
                }
            }
            case INFORMATION_PUBLISHED -> {
                if (day >= listing.getTargetElapsedDay() - REVIEW_LEAD_DAYS) {
                    review(player, listing);
                }
            }
            case REVIEWED -> {
                if (day >= listing.getTargetElapsedDay() - OFFER_LEAD_DAYS) {
                    confirmOffer(player, listing);
                }
            }
            case OFFER_CONFIRMED -> {
                if (day >= listing.getTargetElapsedDay() && isMarketRefreshDay(player)) {
                    list(player, listing);
                }
            }
            case LISTED -> { }
        }
    }

    private void announce(Player player, NpcCompanyListing listing) {
        StockSpec stock = stock(listing);
        listedCompanyService.createNpcCandidate(player, stock);
        financialService.ensureBaselineHistory(player);
        publish(player, listing, "announce", StockNewsDirection.NEUTRAL, "거래소 예비 공시",
                stock.name() + ", 신규상장 준비 착수",
                stock.description() + "인 " + stock.name() + "이 거래소 상장을 위한 예비 절차에 들어갔다.",
                "상장 전 실적과 재무상태가 순차적으로 공개될 예정이며 아직 공모 조건은 확정되지 않았다.");
        listing.advanceTo(NpcCompanyListingStage.ANNOUNCED);
    }

    private void publishInformation(Player player, NpcCompanyListing listing) {
        StockSpec stock = stock(listing);
        ListedCompany company = listedCompanyService.requireCompany(player, stock.key());
        String profitability = company.getGrossMarginBasisPoints() > company.getOperatingExpenseBasisPoints()
                ? "영업흑자 구조" : "수익성 검증 필요";
        publish(player, listing, "information", StockNewsDirection.NEUTRAL, "기업설명서",
                stock.name() + ", 상장 전 주요 재무정보 공개",
                stock.name() + "은 최근 사업 규모와 자산·부채 현황을 담은 기업설명서를 공개했다.",
                "현재 기준은 " + profitability + "이며 최종 기업가치는 심사 시점의 최근 4개 분기로 계산된다.");
        listing.advanceTo(NpcCompanyListingStage.INFORMATION_PUBLISHED);
    }

    private void review(Player player, NpcCompanyListing listing) {
        int delayDays = reviewDelayDays(player, listing);
        if (delayDays > 0) {
            int originalTarget = listing.getTargetElapsedDay();
            listing.postpone(delayDays);
            listingRepository.findByPlayerOrderByTargetElapsedDayAsc(player).stream()
                    .filter(other -> other != listing && !other.isListed()
                            && other.getTargetElapsedDay() > originalTarget)
                    .forEach(other -> other.shiftTarget(delayDays));
            publish(player, listing, "postponed", StockNewsDirection.NEGATIVE, "거래소 심사부",
                    stock(listing).name() + ", 상장심사 보완으로 일정 연기",
                    "거래소가 추가 자료 확인을 요청하면서 상장 일정이 " + delayDays / 30 + "개월 미뤄졌다.",
                    "후속 심사는 자동으로 재개되며 후보 자격이 영구적으로 취소된 것은 아니다.");
            return;
        }
        publish(player, listing, "reviewed", StockNewsDirection.POSITIVE, "거래소 심사부",
                stock(listing).name() + ", 신규상장 심사 승인",
                "거래소가 최근 4개 분기 실적과 재무 건전성을 검토해 상장심사를 승인했다.",
                "공모가는 적정가치와 위험등급별 할인율을 반영해 상장 직전에 확정된다.");
        listing.advanceTo(NpcCompanyListingStage.REVIEWED);
    }

    private void confirmOffer(Player player, NpcCompanyListing listing) {
        StockSpec stock = stock(listing);
        ListedCompany company = listedCompanyService.requireCompany(player, stock.key());
        long fairValue = valuationService.latest(company).orElseThrow().getFairValueBase();
        NpcIpoCandidate candidate = candidateCatalog.require(stock.key());
        long offerPrice = roundToHundred(multiplyDivide(
                fairValue,
                10_000 - candidate.offerDiscountBasisPoints(),
                10_000
        ));
        int demand = demandBasisPoints(player, stock, company, candidate);
        listing.confirmOffer(offerPrice, demand);
        StockNewsDirection direction = demand >= 300 ? StockNewsDirection.POSITIVE
                : demand <= -300 ? StockNewsDirection.NEGATIVE : StockNewsDirection.NEUTRAL;
        publish(player, listing, "offer", direction, "주관사 공시",
                stock.name() + ", 공모가 " + offerPrice + "원 확정",
                "최종 공모가는 최근 실적에 따른 적정가치와 "
                        + candidate.offerDiscountBasisPoints() / 100.0 + "%의 공모 할인을 반영했다.",
                "예상 수요는 '" + demandLabel(demand) + "' 단계다. 실제 첫 거래가는 시장 상황에 따라 달라질 수 있다.");
    }

    private void list(Player player, NpcCompanyListing listing) {
        StockSpec stock = stock(listing);
        ListedCompany company = listedCompanyService.requireCompany(player, stock.key());
        long fairValue = valuationService.latest(company).orElseThrow().getFairValueBase();
        double market = marketRegimeService.currentPulse(player).effectPercent()
                + marketNewsService.activePriceEffectPercent(player);
        double industry = industryNewsService.activePriceEffectPercent(player, stock.industry())
                * stock.industryBeta();
        Random random = new Random(seed(player, stock.key(), listing.getTargetElapsedDay()));
        NpcIpoFirstCandleModel.Result candle = firstCandleModel.calculate(
                new NpcIpoFirstCandleModel.Input(
                        listing.getOfferPrice(), fairValue, listing.getDemandBasisPoints(),
                        market * stock.beta(), industry,
                        stock.idiosyncraticVolatilityPercent(), stock.riskType()
                ),
                random
        );
        priceRepository.save(StockPriceHistory.listing(
                player, stock.key(), candle.open(), candle.high(), candle.low(), candle.close(),
                Math.max(1, company.getMarketParticipantShares() / 100),
                candle.listingImpactBasisPoints(), candle.marketImpactBasisPoints(),
                candle.industryImpactBasisPoints(), candle.valuationImpactBasisPoints(),
                candle.idiosyncraticImpactBasisPoints(), candle.pathImpactBasisPoints()
        ));
        subscriptionService.settleOnListing(player, listing, company);
        listing.markListed(player.getElapsedDays());
        publish(player, listing, "listed", StockNewsDirection.NEUTRAL, "거래소 공시",
                stock.name() + ", 거래소 신규상장",
                stock.name() + "의 주식 거래가 시작됐다. 공모가는 " + listing.getOfferPrice() + "원이었다.",
                "첫 5일봉에는 공모 수요와 상장 시점의 시장·업종 상황이 함께 반영됐다.");
        liquidityService.ensureInitialized(player);
    }

    private int reviewDelayDays(Player player, NpcCompanyListing listing) {
        if (listing.getPostponementCount() > 0) {
            return 0;
        }
        int roll = Math.floorMod(Long.hashCode(seed(
                player, listing.getStockKey(), listing.getTargetElapsedDay())), 100);
        if (roll < 5) {
            return 180;
        }
        return roll < 20 ? 90 : 0;
    }

    private int demandBasisPoints(
            Player player,
            StockSpec stock,
            ListedCompany company,
            NpcIpoCandidate candidate
    ) {
        int performance = clamp((company.getAnnualGrowthBasisPoints() - 800) / 2, -400, 400);
        int market = switch (marketRegimeService.currentRegime(player)) {
            case EXPANSION -> 300;
            case NEUTRAL -> 0;
            case RECESSION -> -300;
        };
        int industry = clamp((int) Math.round(
                industryNewsService.activePriceEffectPercent(player, stock.industry()) * 100
        ), -300, 300);
        int operatingMargin = company.getGrossMarginBasisPoints() - company.getOperatingExpenseBasisPoints();
        long assets = Math.max(1, company.getCash() + company.getNonCashAssets());
        int debtRatioBasisPoints = (int) Math.min(
                10_000, multiplyDivide(company.getDebt(), 10_000, assets));
        int quality = clamp(
                (operatingMargin - 800) / 3 - Math.max(0, debtRatioBasisPoints - 4_000) / 8,
                -400,
                400
        );
        int discount = candidate.offerDiscountBasisPoints() >= 1_600 ? 300
                : candidate.offerDiscountBasisPoints() >= 1_200 ? 200 : 100;
        return performance + market + industry + quality + discount;
    }

    private void publish(
            Player player,
            NpcCompanyListing listing,
            String phase,
            StockNewsDirection direction,
            String source,
            String title,
            String firstParagraph,
            String secondParagraph
    ) {
        StockSpec stock = stock(listing);
        String eventKey = "npc_ipo:" + stock.key() + ":" + phase + ":" + listing.getPostponementCount();
        if (newsRepository.existsByPlayerAndEventKey(player, eventKey)) {
            return;
        }
        newsRepository.save(StockNewsArticle.ipo(
                player, eventKey, stock.industry(), stock.key(), stock.name(), direction,
                source, title, firstParagraph, secondParagraph
        ));
    }

    private StockSpec stock(NpcCompanyListing listing) {
        return stockCatalog.find(listing.getStockKey()).orElseThrow();
    }

    private boolean isMarketRefreshDay(Player player) {
        String referenceKey = stockCatalog.initial().getFirst().key();
        return priceRepository.findFirstByPlayerAndStockKeyOrderByElapsedDaysDescIdDesc(player, referenceKey)
                .map(history -> player.getElapsedDays() - history.getElapsedDays() >= 5)
                .orElse(true);
    }

    private String demandLabel(int demandBasisPoints) {
        if (demandBasisPoints >= 300) {
            return "흥행";
        }
        if (demandBasisPoints <= -300) {
            return "수요 부진";
        }
        return "보통";
    }

    private long roundToHundred(long value) {
        return Math.max(100L, Math.round(value / 100.0) * 100L);
    }

    private long multiplyDivide(long value, long multiplier, long divisor) {
        return BigInteger.valueOf(value).multiply(BigInteger.valueOf(multiplier))
                .divide(BigInteger.valueOf(divisor)).longValueExact();
    }

    private int clamp(int value, int minimum, int maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }

    private long seed(Player player, String stockKey, int day) {
        long seed = player.getId() == null ? 0 : player.getId();
        return (seed * 31 + stockKey.hashCode()) * 31 + day;
    }
}
