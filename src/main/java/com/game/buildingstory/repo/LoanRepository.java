package com.game.buildingstory.repo;

import com.game.buildingstory.domain.Loan;
import com.game.buildingstory.domain.Player;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LoanRepository extends JpaRepository<Loan, Long> {
    List<Loan> findByPlayer(Player player);
}
