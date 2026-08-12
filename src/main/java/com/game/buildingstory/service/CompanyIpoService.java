package com.game.buildingstory.service;

import com.game.buildingstory.domain.CompanyCashFlowType;
import com.game.buildingstory.domain.CompanyDepartmentType;
import com.game.buildingstory.domain.CompanyListing;
import com.game.buildingstory.domain.CompanyListingStatus;
import com.game.buildingstory.domain.ListedCompany;
import com.game.buildingstory.domain.Player;
import com.game.buildingstory.domain.PlayerCompany;
import com.game.buildingstory.domain.StockPriceHistory;
import com.game.buildingstory.repo.CompanyListingRepository;
import com.game.buildingstory.repo.CompanyQuarterlyReportRepository;
import com.game.buildingstory.repo.CompanyValuationSnapshotRepository;
import com.game.buildingstory.repo.ListedCompanyRepository;
import com.game.buildingstory.repo.PlayerCompanyRepository;
import com.game.buildingstory.repo.PlayerRepository;
import com.game.buildingstory.repo.StockPriceHistoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** IPO 신청, 준비 진행과 취소의 상태 변경을 한 트랜잭션 경계에서 처리한다. */
@Service
public class CompanyIpoService {
    private final PlayerRepository playerRepository;
    private final PlayerCompanyRepository companyRepository;
    private final CompanyListingRepository listingRepository;
    private final CompanyIpoQualificationService qualificationService;
    private final CompanyWorkforceService workforceService;
    private final CompanyCashLedgerService cashLedgerService;
    private final CompanyValuationSnapshotRepository valuationRepository;
    private final ListedCompanyRepository listedCompanyRepository;
    private final StockPriceHistoryRepository priceHistoryRepository;
    private final CompanyQuarterlyReportRepository quarterlyReportRepository;
    private final CompanyNewsService companyNewsService;

    public CompanyIpoService(
            PlayerRepository playerRepository,
            PlayerCompanyRepository companyRepository,
            CompanyListingRepository listingRepository,
            CompanyIpoQualificationService qualificationService,
            CompanyWorkforceService workforceService,
            CompanyCashLedgerService cashLedgerService,
            CompanyValuationSnapshotRepository valuationRepository,
            ListedCompanyRepository listedCompanyRepository,
            StockPriceHistoryRepository priceHistoryRepository,
            CompanyQuarterlyReportRepository quarterlyReportRepository,
            CompanyNewsService companyNewsService
    ) {
        this.playerRepository = playerRepository;
        this.companyRepository = companyRepository;
        this.listingRepository = listingRepository;
        this.qualificationService = qualificationService;
        this.workforceService = workforceService;
        this.cashLedgerService = cashLedgerService;
        this.valuationRepository = valuationRepository;
        this.listedCompanyRepository = listedCompanyRepository;
        this.priceHistoryRepository = priceHistoryRepository;
        this.quarterlyReportRepository = quarterlyReportRepository;
        this.companyNewsService = companyNewsService;
    }

    /**
     * 화면에 필요한 IPO 자격, 현재 진행 상태와 공모비율별 예상 결과를 같은 정책으로 계산한다.
     * 신청 전에는 일반 자격을, 준비가 시작된 뒤에는 기존 신청 자체를 결격으로 보지 않는 최종 자격을 사용한다.
     */
    @Transactional(readOnly = true)
    public CompanyIpoOverview overview(PlayerCompany company) {
        CompanyListing listing = listingRepository.findByCompany(company).orElse(null);
        boolean activeApplication = listing != null
                && listing.getStatus() != CompanyListingStatus.CANCELLED;
        CompanyIpoQualification qualification = activeApplication
                ? qualificationService.evaluateForFinalReview(company)
                : qualificationService.evaluate(company);
        long valuation = valuationRepository.findFirstByCompanyOrderByQuarterSequenceDesc(company)
                .map(snapshot -> snapshot.getEnterpriseValue())
                .orElse(0L);
        List<CompanyIpoOverview.OfferOption> options = valuation <= 0
                ? List.of()
                : CompanyIpoPolicy.OFFER_PERCENT_OPTIONS.stream()
                        .sorted()
                        .map(percent -> offerOption(company, valuation, percent))
                        .toList();
        boolean strategyFinanceAvailable = qualification.checks().stream()
                .filter(check -> check.requirement() == CompanyIpoRequirement.STRATEGY_FINANCE)
                .anyMatch(check -> check.currentValue() > 0);
        boolean workCapacityAvailable = activeApplication
                || (strategyFinanceAvailable && workforceService.canReserveMajorWork(
                        company,
                        CompanyDepartmentType.STRATEGY_FINANCE,
                        CompanyIpoPolicy.STRATEGY_FINANCE_WORKLOAD
                ));
        return new CompanyIpoOverview(
                listing == null ? null : listing.getStatus(),
                qualification,
                valuation,
                listing == null ? 0 : listing.getSelectedOfferPercent(),
                listing == null ? 0 : listing.getPreparationMonthsCompleted(),
                listing == null ? CompanyIpoPolicy.PREPARATION_COST : listing.getPreparationCost(),
                listing == null ? 0 : listing.getOfferPrice(),
                listing == null ? 0 : listing.getNewShares(),
                listing == null ? 0 : listing.getProceeds(),
                listing == null ? null : listing.getListedElapsedDay(),
                workCapacityAvailable,
                options
        );
    }

    private CompanyIpoOverview.OfferOption offerOption(
            PlayerCompany company,
            long valuation,
            int percent
    ) {
        long newShares = CompanyIpoPolicy.calculateNewShares(company.getIssuedShares(), percent);
        long offerPrice = CompanyIpoPolicy.calculateOfferPrice(valuation, company.getIssuedShares());
        long issuedAfterListing = Math.addExact(company.getIssuedShares(), newShares);
        boolean preservesMinimumOwnership = CompanyIpoPolicy.preservesMinimumPlayerOwnership(
                company.getPlayerShares(), issuedAfterListing);
        return new CompanyIpoOverview.OfferOption(
                percent,
                newShares,
                offerPrice,
                CompanyIpoPolicy.calculateProceeds(newShares, offerPrice),
                company.getPlayerShares() * 100.0 / issuedAfterListing,
                preservesMinimumOwnership
        );
    }

    /**
     * 자격 확인, 업무 슬롯 예약, 준비비 차감과 신청 저장을 함께 처리한다.
     * 중간 단계가 실패하면 트랜잭션이 롤백되어 슬롯이나 현금만 변경된 상태가 남지 않는다.
     */
    @Transactional
    public String apply(long playerId, int offerPercent) {
        if (!CompanyIpoPolicy.isValidOfferPercent(offerPercent)) {
            return "공모비율은 15%, 25%, 35% 중에서 선택해야 합니다.";
        }
        Player player = playerRepository.findByIdForUpdate(playerId).orElseThrow();
        PlayerCompany company = companyRepository.findByPlayer(player).orElseThrow();
        CompanyIpoQualification qualification = qualificationService.evaluate(company);
        if (!qualification.canApply()) {
            return "IPO 신청 조건 미충족 · " + qualification.unmetChecks().size() + "개 조건 확인 필요";
        }
        long requestedNewShares = CompanyIpoPolicy.calculateNewShares(
                company.getIssuedShares(), offerPercent);
        long requestedIssuedShares = Math.addExact(company.getIssuedShares(), requestedNewShares);
        if (!CompanyIpoPolicy.preservesMinimumPlayerOwnership(
                company.getPlayerShares(), requestedIssuedShares)) {
            return "상장 후 플레이어 지분이 20% 미만이 되는 공모는 신청할 수 없습니다.";
        }
        if (company.getCorporateCash() < CompanyIpoPolicy.PREPARATION_COST) {
            return "법인현금 부족 · IPO 준비비 "
                    + CompanyIpoPolicy.PREPARATION_COST / 100_000_000L + "억원 필요";
        }
        if (!workforceService.canReserveMajorWork(
                company,
                CompanyDepartmentType.STRATEGY_FINANCE,
                CompanyIpoPolicy.STRATEGY_FINANCE_WORKLOAD
        )) {
            return "IPO를 시작할 전사·전략재무 업무 여력이 부족합니다.";
        }

        CompanyListing listing = listingRepository.findByCompany(company).orElse(null);
        workforceService.reserveMajorWork(
                company,
                CompanyDepartmentType.STRATEGY_FINANCE,
                CompanyIpoPolicy.STRATEGY_FINANCE_WORKLOAD
        );
        if (listing == null) {
            listing = new CompanyListing(
                    company,
                    CompanyIpoPolicy.PLAYER_COMPANY_STOCK_KEY,
                    offerPercent,
                    player.getElapsedDays(),
                    CompanyIpoPolicy.PREPARATION_COST
            );
        } else {
            listing.restartApplication(
                    offerPercent,
                    player.getElapsedDays(),
                    CompanyIpoPolicy.PREPARATION_COST
            );
        }
        if (!cashLedgerService.withdraw(
                company,
                "ipo:preparation:" + listing.getApplicationSequence(),
                CompanyCashFlowType.FINANCING,
                "IPO 준비비",
                CompanyIpoPolicy.PREPARATION_COST
        )) {
            throw new IllegalStateException("IPO 준비비 거래를 기록하지 못했습니다.");
        }
        listingRepository.save(listing);
        companyNewsService.recordIpoApplication(company, listing);
        return "IPO 신청 완료 · 공모비율 " + offerPercent + "% · 준비기간 "
                + CompanyIpoPolicy.PREPARATION_MONTHS + "개월";
    }

    /** 정상 월 정산이 끝났을 때 호출되며, 운영중단 월에는 호출되지 않아 준비도 함께 정지한다. */
    @Transactional
    public String processSuccessfulMonth(PlayerCompany company) {
        CompanyListing listing = listingRepository.findByCompany(company).orElse(null);
        if (listing == null || listing.getStatus() != CompanyListingStatus.PREPARING) {
            return "";
        }
        boolean becameReady = listing.advancePreparationMonth(CompanyIpoPolicy.PREPARATION_MONTHS);
        if (!becameReady) {
            return "IPO 준비 " + listing.getPreparationMonthsCompleted()
                    + "/" + CompanyIpoPolicy.PREPARATION_MONTHS + "개월";
        }
        companyNewsService.recordIpoReady(company, listing);
        boolean qualified = qualificationService.evaluateForFinalReview(company).canApply();
        return qualified
                ? "IPO 준비 완료 · 최종 상장 확정 가능"
                : "IPO 준비 완료 · 최종 심사 조건 회복 필요";
    }

    /** 준비비는 환불하지 않고 예약했던 전사·전략재무 업무 슬롯만 반환한다. */
    @Transactional
    public String cancel(long playerId) {
        Player player = playerRepository.findByIdForUpdate(playerId).orElseThrow();
        PlayerCompany company = companyRepository.findByPlayer(player).orElseThrow();
        CompanyListing listing = listingRepository.findByCompany(company).orElse(null);
        if (listing == null || (listing.getStatus() != CompanyListingStatus.PREPARING
                && listing.getStatus() != CompanyListingStatus.READY)) {
            return "취소할 IPO 신청이 없습니다.";
        }
        workforceService.releaseMajorWork(
                company,
                CompanyDepartmentType.STRATEGY_FINANCE,
                CompanyIpoPolicy.STRATEGY_FINANCE_WORKLOAD
        );
        listing.cancel();
        return "IPO 신청 취소 · 준비비는 반환되지 않음";
    }

    /**
     * 최신 확정 지분가치로 공모조건을 다시 계산하고 신주·공모대금·상장기업·첫 가격을 함께 확정한다.
     * 어느 한 저장이 실패하면 전체가 롤백되어 주식 수와 법인현금이 서로 어긋나지 않는다.
     */
    @Transactional
    public String confirmListing(long playerId) {
        Player player = playerRepository.findByIdForUpdate(playerId).orElseThrow();
        PlayerCompany company = companyRepository.findByPlayer(player).orElseThrow();
        CompanyListing listing = listingRepository.findByCompany(company).orElse(null);
        if (listing == null || listing.getStatus() != CompanyListingStatus.READY) {
            return "상장 확정 가능한 IPO가 없습니다.";
        }
        CompanyIpoQualification qualification = qualificationService.evaluateForFinalReview(company);
        if (!qualification.canApply()) {
            return "최종 상장 조건 미충족 · " + qualification.unmetChecks().size() + "개 조건 확인 필요";
        }
        if (listedCompanyRepository.findByPlayerAndStockKey(player, listing.getStockKey()).isPresent()
                || priceHistoryRepository.existsByPlayerAndStockKey(player, listing.getStockKey())) {
            throw new IllegalStateException("플레이어 기업 상장 데이터가 이미 존재합니다.");
        }

        long valuation = valuationRepository.findFirstByCompanyOrderByQuarterSequenceDesc(company)
                .orElseThrow(() -> new IllegalStateException("확정 기업가치가 없습니다."))
                .getEnterpriseValue();
        long existingShares = company.getIssuedShares();
        long playerShares = company.getPlayerShares();
        long newShares = CompanyIpoPolicy.calculateNewShares(
                existingShares, listing.getSelectedOfferPercent());
        long issuedSharesAfterListing = Math.addExact(existingShares, newShares);
        if (!CompanyIpoPolicy.preservesMinimumPlayerOwnership(
                playerShares, issuedSharesAfterListing)) {
            return "상장 후 플레이어 지분이 20% 미만이 되는 공모는 진행할 수 없습니다.";
        }
        long offerPrice = CompanyIpoPolicy.calculateOfferPrice(valuation, existingShares);
        long proceeds = CompanyIpoPolicy.calculateProceeds(newShares, offerPrice);
        long existingExternalShares = Math.subtractExact(existingShares, playerShares);
        var reportsAtListing = quarterlyReportRepository
                .findByCompanyOrderByQuarterSequenceDesc(company);
        int latestQuarterSequence = reportsAtListing.stream()
                .findFirst()
                .map(com.game.buildingstory.domain.CompanyQuarterlyReport::getQuarterSequence)
                .orElse(-1);
        int latestDividendQuarterSequence = reportsAtListing.stream()
                .filter(com.game.buildingstory.domain.CompanyQuarterlyReport::isDividendDecided)
                .mapToInt(com.game.buildingstory.domain.CompanyQuarterlyReport::getQuarterSequence)
                .max()
                .orElse(-1);

        company.issueExternalShares(newShares);
        if (!cashLedgerService.deposit(
                company,
                "ipo:proceeds:" + listing.getApplicationSequence(),
                CompanyCashFlowType.FINANCING,
                "IPO 공모대금",
                proceeds
        )) {
            throw new IllegalStateException("IPO 공모대금 거래를 기록하지 못했습니다.");
        }
        listedCompanyRepository.save(new ListedCompany(
                player,
                listing.getStockKey(),
                issuedSharesAfterListing,
                playerShares,
                existingExternalShares,
                newShares,
                0,
                0
        ));
        priceHistoryRepository.save(new StockPriceHistory(
                player,
                listing.getStockKey(),
                offerPrice,
                offerPrice,
                offerPrice,
                offerPrice,
                Math.max(1, newShares / 100)
        ));
        listing.completeListing(
                valuation,
                offerPrice,
                newShares,
                proceeds,
                player.getElapsedDays(),
                latestQuarterSequence,
                latestDividendQuarterSequence
        );
        companyNewsService.recordIpoListing(company, listing);
        workforceService.releaseMajorWork(
                company,
                CompanyDepartmentType.STRATEGY_FINANCE,
                CompanyIpoPolicy.STRATEGY_FINANCE_WORKLOAD
        );
        return "상장 완료 · 공모가 " + offerPrice + "원 · 법인 조달금 " + proceeds + "원";
    }
}
