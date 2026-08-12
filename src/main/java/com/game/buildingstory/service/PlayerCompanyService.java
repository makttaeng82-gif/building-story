package com.game.buildingstory.service;

import com.game.buildingstory.domain.MonthlyRecord;
import com.game.buildingstory.domain.Player;
import com.game.buildingstory.domain.PlayerCompany;
import com.game.buildingstory.domain.CompanyDepartment;
import com.game.buildingstory.domain.CompanyDepartmentType;
import com.game.buildingstory.domain.CompanyCashFlowType;
import com.game.buildingstory.domain.RecordType;
import com.game.buildingstory.repo.MonthlyRecordRepository;
import com.game.buildingstory.repo.OwnedSecretaryRepository;
import com.game.buildingstory.repo.PlayerCompanyRepository;
import com.game.buildingstory.repo.PlayerRepository;
import com.game.buildingstory.repo.CompanyDepartmentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/** 기업 설립과 법인 기본 상태 조회를 담당한다. */
@Service
public class PlayerCompanyService {
    public static final long MINIMUM_INVESTMENT = 150_000_000_000L;
    public static final long RECOMMENDED_INVESTMENT = 800_000_000_000L;
    public static final long AGGRESSIVE_INVESTMENT = 1_200_000_000_000L;
    public static final long SECRETARY_TRAINING_COST = 300_000_000L;
    public static final long INITIAL_ISSUED_SHARES = 10_000_000L;
    public static final long ESTIMATED_MONTHLY_FIXED_COST = 11_960_000_000L;

    private static final int RECORD_RETENTION_DAYS = 62;

    private final PlayerRepository playerRepository;
    private final PlayerCompanyRepository playerCompanyRepository;
    private final OwnedSecretaryRepository ownedSecretaryRepository;
    private final MonthlyRecordRepository monthlyRecordRepository;
    private final StockCatalog stockCatalog;
    private final CompanyDepartmentRepository companyDepartmentRepository;
    private final CompanyCashLedgerService cashLedgerService;
    private final CompanyAccessService companyAccessService;
    private final CompanyFoundationEligibilityService foundationEligibilityService;

    public PlayerCompanyService(
            PlayerRepository playerRepository,
            PlayerCompanyRepository playerCompanyRepository,
            OwnedSecretaryRepository ownedSecretaryRepository,
            MonthlyRecordRepository monthlyRecordRepository,
            StockCatalog stockCatalog,
            CompanyDepartmentRepository companyDepartmentRepository,
            CompanyCashLedgerService cashLedgerService,
            CompanyAccessService companyAccessService,
            CompanyFoundationEligibilityService foundationEligibilityService
    ) {
        this.playerRepository = playerRepository;
        this.playerCompanyRepository = playerCompanyRepository;
        this.ownedSecretaryRepository = ownedSecretaryRepository;
        this.monthlyRecordRepository = monthlyRecordRepository;
        this.stockCatalog = stockCatalog;
        this.companyDepartmentRepository = companyDepartmentRepository;
        this.cashLedgerService = cashLedgerService;
        this.companyAccessService = companyAccessService;
        this.foundationEligibilityService = foundationEligibilityService;
    }

    @Transactional(readOnly = true)
    public Optional<PlayerCompany> company(Player player) {
        return playerCompanyRepository.findByPlayer(player);
    }

    @Transactional
    public String establish(long playerId, String requestedCompanyName, String requestedServiceName, long investment) {
        Player player = playerRepository.findByIdForUpdate(playerId).orElseThrow();
        Optional<PlayerCompany> existingCompany = playerCompanyRepository.findByPlayer(player);
        if (existingCompany.isPresent()) {
            return "이미 설립된 기업이 있음";
        }
        if (!companyAccessService.isUnlocked(player)) {
            return "AI 기업 설립 제안을 먼저 수락해야 합니다.";
        }
        String readinessError = foundationEligibilityService.failureMessage(player);
        if (readinessError != null) {
            return readinessError;
        }
        String companyName = normalizeName(requestedCompanyName);
        String serviceName = normalizeName(requestedServiceName);
        String validationError = validateNames(companyName, serviceName);
        if (validationError != null) {
            return validationError;
        }
        if (investment < MINIMUM_INVESTMENT) {
            return "최소 출자금은 1,500억원";
        }
        if (investment > player.getCash()) {
            return "출자금이 개인 현금을 초과함";
        }
        if (!player.spendCash(investment)) {
            return "출자금 부족";
        }

        long initialCorporateCash = Math.subtractExact(investment, SECRETARY_TRAINING_COST);
        PlayerCompany company = new PlayerCompany(
                player,
                companyName,
                serviceName,
                investment,
                initialCorporateCash,
                INITIAL_ISSUED_SHARES,
                player.getElapsedDays()
        );
        playerCompanyRepository.save(company);
        cashLedgerService.recordApplied(
                company, "founding:capital", CompanyCashFlowType.FINANCING, "설립 출자금", investment);
        cashLedgerService.recordApplied(
                company, "founding:secretary-training", CompanyCashFlowType.OPERATING,
                "비서 기업교육비", -SECRETARY_TRAINING_COST);
        for (CompanyDepartmentType type : List.of(
                CompanyDepartmentType.AI_DEVELOPMENT,
                CompanyDepartmentType.SALES_MARKETING,
                CompanyDepartmentType.SERVICE_OPERATIONS)) {
            companyDepartmentRepository.save(new CompanyDepartment(company, type));
        }
        ownedSecretaryRepository.findByPlayerOrderById(player)
                .forEach(secretary -> secretary.assignTo(null));
        monthlyRecordRepository.save(new MonthlyRecord(
                player,
                RecordType.COMPANY,
                "기업 설립 출자",
                -investment,
                0,
                null,
                companyName + " · 비서 기업교육비 3억원 · 지분 100%"
        ));
        monthlyRecordRepository.deleteByPlayerAndElapsedDaysLessThan(
                player, Math.max(0, player.getElapsedDays() - RECORD_RETENTION_DAYS));
        return companyName + " 설립 완료";
    }

    private String normalizeName(String value) {
        return value == null ? "" : value.trim();
    }

    private String validateNames(String companyName, String serviceName) {
        if (!validName(companyName)) {
            return "회사명은 2~20자로 입력";
        }
        if (!validName(serviceName)) {
            return "서비스명은 2~20자로 입력";
        }
        boolean duplicatesListedCompany = stockCatalog.all().stream()
                .anyMatch(spec -> spec.name().equalsIgnoreCase(companyName));
        return duplicatesListedCompany ? "기존 상장기업과 같은 회사명은 사용할 수 없음" : null;
    }

    private boolean validName(String value) {
        return value.length() >= 2
                && value.length() <= 20
                && value.codePoints().noneMatch(Character::isISOControl);
    }
}
