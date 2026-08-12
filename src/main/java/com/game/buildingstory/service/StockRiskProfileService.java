package com.game.buildingstory.service;

import com.game.buildingstory.domain.CompanyQuarterlyReport;
import com.game.buildingstory.domain.ListedCompany;
import com.game.buildingstory.domain.ListedCompanyQuarterlyReport;
import com.game.buildingstory.domain.Player;
import com.game.buildingstory.repo.CompanyListingRepository;
import com.game.buildingstory.repo.CompanyQuarterlyReportRepository;
import com.game.buildingstory.repo.ListedCompanyQuarterlyReportRepository;
import com.game.buildingstory.repo.ListedCompanyRepository;
import com.game.buildingstory.repo.PlayerCompanyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** 최근 재무상태와 상장 경과기간을 종목의 현재 베타와 고유 변동성으로 변환한다. */
@Service
@Transactional(readOnly = true)
public class StockRiskProfileService {
    private static final double MIN_BETA = 0.55;
    private static final double MAX_BETA = 1.85;
    private static final double MIN_VOLATILITY = 0.55;
    private static final double MAX_VOLATILITY = 4.80;

    private final ListedCompanyRepository listedCompanyRepository;
    private final ListedCompanyQuarterlyReportRepository listedReportRepository;
    private final PlayerCompanyRepository playerCompanyRepository;
    private final CompanyQuarterlyReportRepository playerReportRepository;
    private final CompanyListingRepository listingRepository;

    public StockRiskProfileService(
            ListedCompanyRepository listedCompanyRepository,
            ListedCompanyQuarterlyReportRepository listedReportRepository,
            PlayerCompanyRepository playerCompanyRepository,
            CompanyQuarterlyReportRepository playerReportRepository,
            CompanyListingRepository listingRepository
    ) {
        this.listedCompanyRepository = listedCompanyRepository;
        this.listedReportRepository = listedReportRepository;
        this.playerCompanyRepository = playerCompanyRepository;
        this.playerReportRepository = playerReportRepository;
        this.listingRepository = listingRepository;
    }

    public StockSpec currentSpec(Player player, StockSpec base) {
        Adjustment adjustment = CompanyIpoPolicy.PLAYER_COMPANY_STOCK_KEY.equals(base.key())
                ? playerCompanyAdjustment(player)
                : npcAdjustment(player, base.key());
        return new StockSpec(
                base.key(), base.industry(), base.name(), base.riskType(),
                clamp(base.beta() + adjustment.beta(), MIN_BETA, MAX_BETA),
                base.industryBeta(),
                clamp(base.idiosyncraticVolatilityPercent() + adjustment.volatility(),
                        MIN_VOLATILITY, MAX_VOLATILITY),
                base.basePrice(), base.issuedShares(), base.description()
        );
    }

    private Adjustment npcAdjustment(Player player, String stockKey) {
        ListedCompany company = listedCompanyRepository.findByPlayerAndStockKey(player, stockKey).orElse(null);
        if (company == null || !company.isFinancialInitialized()) {
            return Adjustment.NONE;
        }
        double beta = 0;
        double volatility = 0;
        long assets = Math.max(1, company.getCash() + company.getNonCashAssets());
        double debtRatio = company.getDebt() / (double) assets;
        if (debtRatio >= 0.55) {
            beta += 0.10;
            volatility += 0.40;
        } else if (debtRatio <= 0.25) {
            beta -= 0.04;
            volatility -= 0.15;
        }
        if (company.getAnnualGrowthBasisPoints() >= 1_200) {
            beta += 0.08;
            volatility += 0.25;
        } else if (company.getAnnualGrowthBasisPoints() <= 400) {
            beta -= 0.04;
            volatility -= 0.10;
        }

        List<ListedCompanyQuarterlyReport> reports =
                listedReportRepository.findTop4ByListedCompanyOrderByFiscalPeriodIndexDesc(company);
        if (!reports.isEmpty() && reports.getFirst().getNetIncome() < 0) {
            beta += 0.12;
            volatility += 0.50;
        } else if (reports.size() >= 3 && reports.stream().limit(3)
                .allMatch(report -> report.getNetIncome() > 0)) {
            beta -= 0.06;
            volatility -= 0.25;
        }
        return new Adjustment(beta, volatility);
    }

    private Adjustment playerCompanyAdjustment(Player player) {
        var company = playerCompanyRepository.findByPlayer(player).orElse(null);
        if (company == null) {
            return Adjustment.NONE;
        }
        double beta = 0;
        double volatility = 0;
        var listing = listingRepository.findByCompany(company).orElse(null);
        if (listing != null && listing.getListedElapsedDay() != null) {
            int listedMonths = Math.max(0, player.getElapsedDays() - listing.getListedElapsedDay()) / 30;
            if (listedMonths < 3) {
                beta += 0.20;
                volatility += 0.90;
            } else if (listedMonths < 6) {
                beta += 0.10;
                volatility += 0.45;
            }
        }
        if (company.isOperationsSuspended()) {
            beta += 0.25;
            volatility += 1.20;
        }
        if (company.getTechnicalDebt() >= 60) {
            beta += 0.12;
            volatility += 0.55;
        } else if (company.getTechnicalDebt() <= 20) {
            beta -= 0.04;
            volatility -= 0.15;
        }

        List<CompanyQuarterlyReport> reports =
                playerReportRepository.findByCompanyOrderByQuarterSequenceDesc(company);
        if (!reports.isEmpty() && reports.getFirst().getNetIncome() < 0) {
            beta += 0.12;
            volatility += 0.50;
        } else if (reports.size() >= 3 && reports.stream().limit(3)
                .allMatch(report -> report.getNetIncome() > 0)) {
            beta -= 0.08;
            volatility -= 0.35;
        }
        if (reports.size() >= 2) {
            long previousRevenue = Math.max(1, reports.get(1).getRevenue());
            double growth = (reports.getFirst().getRevenue() - previousRevenue) / (double) previousRevenue;
            if (growth >= 0.15) {
                beta += 0.08;
                volatility += 0.25;
            } else if (growth <= -0.10) {
                beta += 0.10;
                volatility += 0.35;
            }
        }
        return new Adjustment(beta, volatility);
    }

    private double clamp(double value, double minimum, double maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }

    private record Adjustment(double beta, double volatility) {
        private static final Adjustment NONE = new Adjustment(0, 0);
    }
}
