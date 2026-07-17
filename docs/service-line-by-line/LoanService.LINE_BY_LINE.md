# LoanService 코드 주석형 해설

원본 파일: `src/main/java/com/game/buildingstory/service/LoanService.java`

형식:
- 원본 서비스 코드를 그대로 배치한다.
- 필요한 코드 바로 아래에 `// 해설:` 주석을 붙인다.
- package/import/단순 상수/단순 필드/반복 애너테이션은 설명하지 않는다.

```java
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
// 해설: 대출 목록, 대출 한도, 남은 원금/상환액, 즉시 상환과 만기 처리를 담당한다.
    /*
     * 대출 한도와 상환 금액 계산을 담당한다.
     *
     * 건물 구매 서비스는 대출을 만들지만, 총 한도와 남은 원금 계산은 이 서비스에 위임한다.
     */
    private static final int RECORD_RETENTION_DAYS = 62;

    private final PlayerRepository playerRepository;
    private final LoanRepository loanRepository;
    private final MonthlyRecordRepository monthlyRecordRepository;

    public LoanService(
    // 해설: 대출 처리에 필요한 플레이어, 대출, 월간 기록 Repository를 생성자 주입으로 받는다.
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
    // 해설: 플레이어가 가진 대출 목록을 조회한다.
        return loanRepository.findByPlayer(player);
    }

    @Transactional(readOnly = true)
    public long remainingRepayment(Player player) {
    // 해설: 남은 원금과 이자를 포함한 총 상환 필요 금액을 계산한다.
        return loanRepository.findByPlayer(player).stream()
                .mapToLong(Loan::remainingRepayment)
                // 해설: 각 대출에서 남은 상환액을 꺼낸다.
                .sum();
    }

    @Transactional(readOnly = true)
    public long remainingPrincipal(Player player) {
    // 해설: 플레이어의 남은 대출 원금 합계를 계산한다.
        return loanRepository.findByPlayer(player).stream()
                .mapToLong(Loan::getPrincipal)
                // 해설: 각 대출에서 원금만 꺼낸다.
                .sum();
    }

    @Transactional(readOnly = true)
    public long availableLoanLimit(Player player) {
    // 해설: 현재 추가로 받을 수 있는 대출 한도를 계산한다.
        return Math.max(0, loanLimit(player) - remainingPrincipal(player));
        // 해설: 총 한도에서 남은 원금을 빼고 음수가 되지 않게 0으로 보정한다.
    }

    @Transactional(readOnly = true)
    public long loanLimit(Player player) {
    // 해설: 평판 기준 총 대출 한도를 계산한다.
        return Math.max(10_000_000L, (long) (player.getReputation() + 1) * 100_000L);
        // 해설: 최소 1천만원을 보장하고, 평판이 높을수록 한도를 늘린다.
    }

    public String repayLoan(long playerId, long loanId) {
    // 해설: 사용자가 특정 대출을 즉시 상환할 때 실행된다.
        Player player = playerRepository.findById(playerId).orElseThrow();
        // 해설: 요청한 플레이어를 DB에서 조회한다.
        if (player.isPaused()) {
        // 해설: 일시정지 중에는 경제 행동을 막는다.
            return pausedActionMessage();
        }
        Loan loan = loanRepository.findById(loanId).orElseThrow();
        // 해설: 상환할 대출을 DB에서 조회한다.
        if (!loan.getPlayer().getId().equals(player.getId())) {
        // 해설: 다른 플레이어의 대출을 상환하지 못하게 검증한다.
            return "잘못된 대출";
        }
        long repayment = loan.remainingRepayment();
        // 해설: 현재 시점에 필요한 전체 상환액을 계산한다.
        if (!player.spendCash(repayment)) {
        // 해설: 상환액을 현금에서 차감한다. 부족하면 실패한다.
            return "상환금 부족 · 필요 금액 " + repayment + "원";
        }
        loanRepository.delete(loan);
        // 해설: 상환 완료된 대출을 삭제한다.
        saveRecord(player, RecordType.LOAN_PAYMENT, "대출상환", -repayment, 0, null, "즉시상환");
        // 해설: 즉시 상환 지출을 월간 기록에 저장한다.
        return "대출상환 완료";
    }

    public String processMaturity(Player player) {
    // 해설: 매월 대출 만기 상태를 진행하고, 만기 실패 패널티를 처리한다.
        String notice = "";
        // 해설: 만기 처리 결과 안내 문구를 누적한다.
        for (Loan loan : loanRepository.findByPlayer(player)) {
        // 해설: 플레이어의 모든 대출을 순회한다.
            loan.advanceMonth();
            // 해설: 대출의 경과 월수를 1개월 증가시킨다.
            if (loan.isMatured()) {
            // 해설: 상환 만기에 도달했는지 확인한다.
                int oldReputation = player.getReputation();
                // 해설: 패널티 계산 기준이 되는 현재 평판을 저장한다.
                int reputationLoss = oldReputation / 2;
                // 해설: 만기 실패 시 현재 평판의 절반을 잃는다.
                player.addReputation(-reputationLoss);
                // 해설: 평판 패널티를 적용한다.
                loan.extendGracePeriod();
                // 해설: 대출을 삭제하지 않고 6개월 유예 기간으로 연장한다.
                saveRecord(player, RecordType.LOAN_PAYMENT, "대출상환 실패", null, -reputationLoss, null, "6개월 유예 연장");
                // 해설: 상환 실패와 평판 감소를 월간 기록에 저장한다.
                notice = appendNotice(notice, "대출상환 실패 · 평판 -" + reputationLoss + " · 6개월 유예");
                // 해설: 사용자에게 보여줄 만기 실패 안내를 누적한다.
            }
        }
        return notice;
    }

    private String pausedActionMessage() {
        return "일시정지 중에는 경제 행동을 할 수 없음";
    }

    private String appendNotice(String base, String addition) {
    // 해설: 여러 안내 문구를 하나로 합친다.
        if (addition == null || addition.isBlank()) {
            return base;
        }
        if (base == null || base.isBlank()) {
            return addition;
        }
        return base + " · " + addition;
    }

    private void saveRecord(Player player, RecordType type, String title, Long amount, int reputationChange, String buildingName, String memo) {
    // 해설: 대출 처리 결과를 월간 기록에 저장하는 공통 메서드다.
        monthlyRecordRepository.save(new MonthlyRecord(player, type, title, amount, reputationChange, buildingName, memo));
        monthlyRecordRepository.deleteByPlayerAndElapsedDaysLessThan(player, Math.max(1, player.getElapsedDays() - RECORD_RETENTION_DAYS + 1));
    }
}
```