package com.game.buildingstory.service;

import com.game.buildingstory.domain.Loan;
import com.game.buildingstory.domain.MonthlyRecord;
import com.game.buildingstory.domain.OwnedBuilding;
import com.game.buildingstory.domain.Player;
import com.game.buildingstory.domain.RecordType;
import com.game.buildingstory.repo.LoanRepository;
import com.game.buildingstory.repo.MonthlyRecordRepository;
import com.game.buildingstory.repo.OwnedBuildingRepository;
import com.game.buildingstory.repo.PlayerRepository;
import com.game.buildingstory.repo.SecretaryTenantEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class LoanService {
    /*
     * 대출 한도와 상환 금액 계산을 담당한다.
     *
     * 건물 구매 서비스는 대출을 만들지만, 총 한도와 남은 원금 계산은 이 서비스에 위임한다.
     */
    private static final int RECORD_RETENTION_DAYS = 62;

    private final PlayerRepository playerRepository;
    private final LoanRepository loanRepository;
    private final MonthlyRecordRepository monthlyRecordRepository;
    private final OwnedBuildingRepository ownedBuildingRepository;
    private final SecretaryTenantEventRepository secretaryTenantEventRepository;

    public LoanService(
            PlayerRepository playerRepository,
            LoanRepository loanRepository,
            MonthlyRecordRepository monthlyRecordRepository,
            OwnedBuildingRepository ownedBuildingRepository,
            SecretaryTenantEventRepository secretaryTenantEventRepository
    ) {
        this.playerRepository = playerRepository;
        this.loanRepository = loanRepository;
        this.monthlyRecordRepository = monthlyRecordRepository;
        this.ownedBuildingRepository = ownedBuildingRepository;
        this.secretaryTenantEventRepository = secretaryTenantEventRepository;
    }

    @Transactional(readOnly = true)
    public List<Loan> loans(Player player) {
        return loanRepository.findByPlayer(player);
    }

    @Transactional(readOnly = true)
    public long remainingRepayment(Player player) {
        return loanRepository.findByPlayer(player).stream()
                .mapToLong(Loan::remainingRepayment)
                .sum();
    }

    @Transactional(readOnly = true)
    public long remainingPrincipal(Player player) {
        return loanRepository.findByPlayer(player).stream()
                .mapToLong(Loan::getPrincipal)
                .sum();
    }

    @Transactional(readOnly = true)
    public long availableLoanLimit(Player player) {
        return Math.max(0, loanLimit(player) - remainingPrincipal(player));
    }

    @Transactional(readOnly = true)
    public long loanLimit(Player player) {
        return ownedBuildingRepository.findByPlayerOrderById(player).stream()
                .mapToLong(building -> building.getMarketPrice() * 60 / 100)
                .sum();
    }

    public String repayLoan(long playerId, long loanId) {
        Player player = playerRepository.findById(playerId).orElseThrow();
        if (player.isPaused()) {
            return pausedActionMessage();
        }
        Loan loan = loanRepository.findById(loanId).orElseThrow();
        if (!loan.getPlayer().getId().equals(player.getId())) {
            return "잘못된 대출";
        }
        long repayment = loan.getPrincipal();
        if (!player.spendCash(repayment)) {
            return "상환금 부족 · 필요 금액 " + repayment + "원";
        }
        loanRepository.delete(loan);
        saveRecord(player, RecordType.LOAN_PAYMENT, "대출상환", -repayment, 0, null, "즉시상환");
        return "대출상환 완료";
    }

    public String processMaturity(Player player) {
        String notice = "";
        for (Loan loan : loanRepository.findByPlayer(player)) {
            long interest = loan.getMonthlyPayment();
            if (!player.payLoanInterest(interest)) {
                loan.recordDelinquency();
                saveRecord(player, RecordType.LOAN_PAYMENT, "대출이자 연체", null, 0, buildingName(loan), "연체 " + loan.getDelinquentMonths() + "개월");
                notice = appendNotice(notice, "대출이자 연체 " + loan.getDelinquentMonths() + "개월");
                if (loan.getDelinquentMonths() >= 2) {
                    notice = appendNotice(notice, foreclose(player, loan));
                }
                continue;
            }
            loan.recordPayment();
            loan.advanceMonth();
            saveRecord(player, RecordType.LOAN_PAYMENT, "대출이자", -interest, 0, buildingName(loan), "연 4.8%");
            if (loan.isMatured()) {
                loan.refinance();
                notice = appendNotice(notice, "담보대출 24개월 자동 재심사 통과");
            }
        }
        return notice;
    }

    private String foreclose(Player player, Loan loan) {
        OwnedBuilding building = loan.getBuilding();
        if (building == null || building.isProtectedTenant() || secretaryTenantEventRepository.existsByBuilding(building)) {
            return "담보 처분 보류";
        }
        long forcedSalePrice = building.getMarketPrice() * 90 / 100;
        long payout = Math.max(0L, forcedSalePrice - loan.getPrincipal());
        if (payout > 0) {
            player.addCash(payout);
        }
        loanRepository.delete(loan);
        ownedBuildingRepository.delete(building);
        saveRecord(player, RecordType.BUILDING_SELL, "담보 강제매각", payout, 0, building.getName(), "감정가 90% · 대출원금 우선상환");
        return building.getName() + " 담보 강제매각";
    }

    private String buildingName(Loan loan) {
        return loan.getBuilding() == null ? null : loan.getBuilding().getName();
    }

    private String pausedActionMessage() {
        return "일시정지 중에는 경제 행동을 할 수 없음";
    }

    private String appendNotice(String base, String addition) {
        if (addition == null || addition.isBlank()) {
            return base;
        }
        if (base == null || base.isBlank()) {
            return addition;
        }
        return base + " · " + addition;
    }

    private void saveRecord(Player player, RecordType type, String title, Long amount, int reputationChange, String buildingName, String memo) {
        monthlyRecordRepository.save(new MonthlyRecord(player, type, title, amount, reputationChange, buildingName, memo));
        monthlyRecordRepository.deleteByPlayerAndElapsedDaysLessThan(player, Math.max(1, player.getElapsedDays() - RECORD_RETENTION_DAYS + 1));
    }
}
