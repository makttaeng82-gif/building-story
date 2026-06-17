package com.game.buildingstory.service;

import com.game.buildingstory.domain.Loan;
import com.game.buildingstory.domain.MonthlyRecord;
import com.game.buildingstory.domain.Player;
import com.game.buildingstory.domain.RecordType;
import com.game.buildingstory.repo.LoanRepository;
import com.game.buildingstory.repo.MonthlyRecordRepository;
import com.game.buildingstory.repo.PlayerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class LoanService {
    private static final int RECORD_RETENTION_DAYS = 62;

    private final PlayerRepository playerRepository;
    private final LoanRepository loanRepository;
    private final MonthlyRecordRepository monthlyRecordRepository;

    public LoanService(
            PlayerRepository playerRepository,
            LoanRepository loanRepository,
            MonthlyRecordRepository monthlyRecordRepository
    ) {
        this.playerRepository = playerRepository;
        this.loanRepository = loanRepository;
        this.monthlyRecordRepository = monthlyRecordRepository;
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
        return Math.max(10_000_000L, (long) (player.getReputation() + 1) * 100_000L);
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
        long repayment = loan.remainingRepayment();
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
            loan.advanceMonth();
            if (loan.isMatured()) {
                int oldReputation = player.getReputation();
                int reputationLoss = oldReputation / 2;
                player.addReputation(-reputationLoss);
                loan.extendGracePeriod();
                saveRecord(player, RecordType.LOAN_PAYMENT, "대출상환 실패", null, -reputationLoss, null, "6개월 유예 연장");
                notice = appendNotice(notice, "대출상환 실패 · 평판 -" + reputationLoss + " · 6개월 유예");
            }
        }
        return notice;
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
