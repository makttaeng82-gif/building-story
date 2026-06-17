package com.game.buildingstory.repo;

import com.game.buildingstory.domain.MonthlyRecord;
import com.game.buildingstory.domain.Player;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MonthlyRecordRepository extends JpaRepository<MonthlyRecord, Long> {
    List<MonthlyRecord> findByPlayerAndElapsedDaysGreaterThanEqualOrderByElapsedDaysDescIdDesc(Player player, int elapsedDays);

    void deleteByPlayerAndElapsedDaysLessThan(Player player, int elapsedDays);
}
