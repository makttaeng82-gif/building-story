package com.game.buildingstory.repo;

import com.game.buildingstory.domain.Loan;
import com.game.buildingstory.domain.Player;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * 대출 저장소다.
 *
 * <p>대출 구매 시 Loan이 생성되고, 월초 정산에서 남은 대출들을 조회해 상환액을 계산한다.</p>
 */
public interface LoanRepository extends JpaRepository<Loan, Long> {
    List<Loan> findByPlayer(Player player);
}
