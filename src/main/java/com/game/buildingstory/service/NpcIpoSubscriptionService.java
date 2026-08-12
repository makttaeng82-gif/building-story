package com.game.buildingstory.service;

import com.game.buildingstory.domain.ListedCompany;
import com.game.buildingstory.domain.ListedCompanyQuarterlyReport;
import com.game.buildingstory.domain.NpcCompanyListing;
import com.game.buildingstory.domain.NpcCompanyListingStage;
import com.game.buildingstory.domain.OwnedStock;
import com.game.buildingstory.domain.Player;
import com.game.buildingstory.domain.StockTradeHistory;
import com.game.buildingstory.repo.NpcCompanyListingRepository;
import com.game.buildingstory.repo.OwnedStockRepository;
import com.game.buildingstory.repo.StockTradeHistoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigInteger;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/** NPC 신규상장 청약의 신청, 예수금 예약, 상장일 배정과 환불을 담당한다. */
@Service
@Transactional
public class NpcIpoSubscriptionService {
    static final long SUBSCRIPTION_FEE = 20_000L;

    private final NpcCompanyListingRepository listingRepository;
    private final StockCatalog stockCatalog;
    private final ListedCompanyService listedCompanyService;
    private final ListedCompanyFinancialService financialService;
    private final ListedCompanyValuationService valuationService;
    private final OwnedStockRepository ownedStockRepository;
    private final StockTradeHistoryRepository tradeHistoryRepository;

    public NpcIpoSubscriptionService(
            NpcCompanyListingRepository listingRepository,
            StockCatalog stockCatalog,
            ListedCompanyService listedCompanyService,
            ListedCompanyFinancialService financialService,
            ListedCompanyValuationService valuationService,
            OwnedStockRepository ownedStockRepository,
            StockTradeHistoryRepository tradeHistoryRepository
    ) {
        this.listingRepository = listingRepository;
        this.stockCatalog = stockCatalog;
        this.listedCompanyService = listedCompanyService;
        this.financialService = financialService;
        this.valuationService = valuationService;
        this.ownedStockRepository = ownedStockRepository;
        this.tradeHistoryRepository = tradeHistoryRepository;
    }

    @Transactional(readOnly = true)
    public Optional<NpcIpoSubscriptionView> active(Player player) {
        return listingRepository.findByPlayerAndStage(player, NpcCompanyListingStage.OFFER_CONFIRMED)
                .stream()
                .findFirst()
                .map(listing -> view(player, listing));
    }

    @Transactional(readOnly = true)
    public Optional<NpcIpoAllocationResultView> pendingResult(Player player) {
        return listingRepository.findByPlayerOrderByTargetElapsedDayAsc(player).stream()
                .filter(NpcCompanyListing::isSubscriptionSettled)
                .filter(listing -> !listing.isSubscriptionResultAcknowledged())
                .findFirst()
                .map(this::resultView);
    }

    public boolean acknowledgeResult(Player player, String stockKey) {
        NpcCompanyListing listing = listingRepository.findByPlayerAndStockKey(player, stockKey).orElse(null);
        if (listing == null || !listing.isSubscriptionSettled()) {
            return false;
        }
        listing.acknowledgeSubscriptionResult();
        return true;
    }

    public String subscribe(Player player, String stockKey, long quantity) {
        if (player.isPaused()) {
            return "일시정지 중에는 공모주를 청약할 수 없습니다.";
        }
        NpcCompanyListing listing = listingRepository.findByPlayerAndStockKey(player, stockKey).orElse(null);
        if (listing == null || listing.getStage() != NpcCompanyListingStage.OFFER_CONFIRMED) {
            return "현재 청약 가능한 공모주가 아닙니다.";
        }
        if (listing.hasSubscription()) {
            return "이미 청약을 신청한 종목입니다.";
        }
        long maximumQuantity = maximumRequestQuantity(player, listing);
        if (quantity <= 0 || quantity > maximumQuantity) {
            return "청약 수량 오류 · 최대 " + quantityText(maximumQuantity) + "주";
        }
        long reservedAmount;
        long totalPayment;
        try {
            reservedAmount = Math.multiplyExact(listing.getOfferPrice(), quantity);
            totalPayment = Math.addExact(reservedAmount, SUBSCRIPTION_FEE);
        } catch (ArithmeticException exception) {
            return "청약 수량 오류";
        }
        if (!player.spendSecuritiesCash(totalPayment)) {
            return "예수금 부족 · 필요 " + moneyText(totalPayment);
        }
        listing.subscribe(quantity, reservedAmount, SUBSCRIPTION_FEE);
        return stock(listing).name() + " " + quantityText(quantity) + "주 청약 신청";
    }

    public void settleOnListing(Player player, NpcCompanyListing listing, ListedCompany company) {
        if (!listing.hasSubscription() || listing.isSubscriptionSettled()) {
            return;
        }
        long requested = listing.getSubscriptionRequestedQuantity();
        long allocated = allocationQuantity(requested, listing.getDemandBasisPoints());
        allocated = Math.min(allocated, company.getMarketParticipantShares());
        long allocatedCost = Math.multiplyExact(allocated, listing.getOfferPrice());
        long refund = Math.subtractExact(listing.getSubscriptionReservedAmount(), allocatedCost);

        if (allocated > 0) {
            if (!company.transferMarketSharesToPlayer(allocated)) {
                throw new IllegalStateException("공모주 배정 수량을 시장 보유분에서 이전하지 못했습니다.");
            }
            OwnedStock holding = ownedStockRepository.findByPlayerAndStockKey(player, listing.getStockKey())
                    .orElseGet(() -> ownedStockRepository.save(new OwnedStock(player, listing.getStockKey())));
            long purchaseCost = Math.addExact(allocatedCost, listing.getSubscriptionFee());
            holding.buy(allocated, purchaseCost);
            StockSpec stock = stock(listing);
            tradeHistoryRepository.save(new StockTradeHistory(
                    player, stock.key(), stock.name(), "청약", allocated, listing.getOfferPrice(),
                    allocatedCost, listing.getSubscriptionFee(), purchaseCost, purchaseCost, 0
            ));
        }
        player.addSecuritiesCash(refund);
        listing.settleSubscription(allocated, refund);
    }

    private NpcIpoSubscriptionView view(Player player, NpcCompanyListing listing) {
        StockSpec stock = stock(listing);
        ListedCompany company = listedCompanyService.requireCompany(player, stock.key());
        var valuation = valuationService.latest(company).orElseThrow();
        ListedCompanyQuarterlyReport report = financialService.latestReport(company).orElseThrow();
        List<NpcIpoQuarterView> recentQuarters = financialService.recentReports(company).stream()
                .map(this::quarterView)
                .toList();
        int allocationPercent = allocationBasisPoints(listing.getDemandBasisPoints()) / 100;
        long maximumQuantity = maximumRequestQuantity(player, listing);
        long expectedAllocation = listing.hasSubscription()
                ? allocationQuantity(listing.getSubscriptionRequestedQuantity(), listing.getDemandBasisPoints())
                : 0;
        long expectedRefund = listing.hasSubscription()
                ? listing.getSubscriptionReservedAmount() - expectedAllocation * listing.getOfferPrice()
                : 0;
        return new NpcIpoSubscriptionView(
                stock.key(), stock.name(), stock.industry(), stock.description(), stock.riskType().label(),
                com.game.buildingstory.domain.GameCalendar.dateText(listing.getTargetElapsedDay()),
                listing.getOfferPrice(), moneyText(listing.getOfferPrice()),
                marketCapText(Math.multiplyExact(listing.getOfferPrice(), company.getIssuedShares())),
                moneyText(valuation.getFairValueLower()) + " - " + moneyText(valuation.getFairValueUpper()),
                demandLabel(listing.getDemandBasisPoints()), competitionText(listing.getDemandBasisPoints()),
                allocationPercent, maximumQuantity, quantityText(maximumQuantity) + "주",
                SUBSCRIPTION_FEE, moneyText(SUBSCRIPTION_FEE),
                moneyText(report.getRevenue()), moneyText(report.getOperatingProfit()), moneyText(report.getNetIncome()),
                percentText(report.getRevenueGrowthBasisPoints()),
                ratioText(report.getOperatingProfit(), report.getRevenue()),
                ratioText(company.getDebt(), company.getCash() + company.getNonCashAssets()),
                discountText(listing.getOfferPrice(), valuation.getFairValueBase()),
                quantityText(company.getMarketParticipantShares()) + "주 · "
                        + ratioText(company.getMarketParticipantShares(), company.getIssuedShares()),
                recentQuarters,
                listing.hasSubscription(), listing.getSubscriptionRequestedQuantity(), expectedAllocation,
                moneyText(listing.getSubscriptionReservedAmount() + listing.getSubscriptionFee()),
                moneyText(expectedRefund)
        );
    }

    private NpcIpoQuarterView quarterView(ListedCompanyQuarterlyReport report) {
        return new NpcIpoQuarterView(
                report.getFiscalYear() + "년 " + report.getFiscalQuarter() + "분기",
                moneyText(report.getRevenue()), moneyText(report.getOperatingProfit()),
                moneyText(report.getNetIncome()), percentText(report.getRevenueGrowthBasisPoints()),
                report.getRevenueGrowthBasisPoints() > 0 ? "up"
                        : report.getRevenueGrowthBasisPoints() < 0 ? "down" : "flat"
        );
    }

    private NpcIpoAllocationResultView resultView(NpcCompanyListing listing) {
        long purchaseAmount = listing.getSubscriptionAllocatedQuantity() * listing.getOfferPrice()
                + listing.getSubscriptionFee();
        return new NpcIpoAllocationResultView(
                listing.getStockKey(), stock(listing).name(),
                com.game.buildingstory.domain.GameCalendar.dateText(listing.getTargetElapsedDay()),
                quantityText(listing.getSubscriptionRequestedQuantity()) + "주",
                quantityText(listing.getSubscriptionAllocatedQuantity()) + "주",
                moneyText(listing.getOfferPrice()), moneyText(purchaseAmount),
                moneyText(listing.getSubscriptionRefundAmount())
        );
    }

    private long maximumRequestQuantity(Player player, NpcCompanyListing listing) {
        ListedCompany company = listedCompanyService.requireCompany(player, listing.getStockKey());
        long ownershipLimit = Math.max(1, company.getIssuedShares() / 100);
        long affordable = player.getSecuritiesCash() <= SUBSCRIPTION_FEE
                ? 0 : (player.getSecuritiesCash() - SUBSCRIPTION_FEE) / listing.getOfferPrice();
        return Math.min(ownershipLimit, affordable);
    }

    private long allocationQuantity(long requested, int demandBasisPoints) {
        if (requested <= 0) {
            return 0;
        }
        long allocated = BigInteger.valueOf(requested)
                .multiply(BigInteger.valueOf(allocationBasisPoints(demandBasisPoints)))
                .divide(BigInteger.valueOf(10_000))
                .longValueExact();
        return Math.max(1, allocated);
    }

    private int allocationBasisPoints(int demandBasisPoints) {
        if (demandBasisPoints >= 300) return 1_000;
        if (demandBasisPoints <= -300) return 7_000;
        return 3_000;
    }

    private String demandLabel(int demandBasisPoints) {
        if (demandBasisPoints >= 300) return "흥행";
        if (demandBasisPoints <= -300) return "수요 부진";
        return "보통";
    }

    private String competitionText(int demandBasisPoints) {
        if (demandBasisPoints >= 300) return "약 10:1";
        if (demandBasisPoints <= -300) return "약 1.4:1";
        return "약 3.3:1";
    }

    private StockSpec stock(NpcCompanyListing listing) {
        return stockCatalog.find(listing.getStockKey()).orElseThrow();
    }

    private String quantityText(long quantity) {
        return String.format("%,d", quantity);
    }

    private String marketCapText(long amount) {
        long eok = amount / 100_000_000L;
        return eok >= 10_000
                ? String.format("%,d조 %,d억원", eok / 10_000, eok % 10_000)
                : String.format("%,d억원", eok);
    }

    private String discountText(long offerPrice, long fairValue) {
        if (fairValue <= 0) return "계산 불가";
        long basisPoints = BigInteger.valueOf(fairValue - offerPrice)
                .multiply(BigInteger.valueOf(10_000))
                .divide(BigInteger.valueOf(fairValue))
                .longValue();
        return String.format(Locale.ROOT, "%.1f%% 할인", basisPoints / 100.0);
    }

    private String ratioText(long numerator, long denominator) {
        if (denominator <= 0) return "계산 불가";
        return String.format(Locale.ROOT, "%.1f%%", numerator * 100.0 / denominator);
    }

    private String percentText(int basisPoints) {
        String sign = basisPoints > 0 ? "+" : "";
        return sign + String.format(Locale.ROOT, "%.1f%%", basisPoints / 100.0);
    }

    private String moneyText(long amount) {
        if (amount == 0) return "0원";
        String sign = amount < 0 ? "-" : "";
        long absolute = Math.abs(amount);
        long eok = absolute / 100_000_000L;
        long man = absolute % 100_000_000L / 10_000L;
        long won = absolute % 10_000L;
        StringBuilder text = new StringBuilder();
        text.append(sign);
        if (eok > 0) text.append(eok).append("억");
        if (man > 0) text.append(man).append("만");
        if (won > 0 || (eok == 0 && man == 0)) text.append(won);
        return text.append("원").toString();
    }
}
