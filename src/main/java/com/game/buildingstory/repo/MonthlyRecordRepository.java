package com.game.buildingstory.repo;

import com.game.buildingstory.domain.MonthlyRecord;
import com.game.buildingstory.domain.Player;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * 최근 기록 패널에 표시할 월간/일간 기록 저장소다.
 *
 * <p>수입, 지출, 평판 변화, 이벤트 로그가 모두 MonthlyRecord로 저장된다.
 * 화면은 최신 기록을 먼저 보여주므로 elapsedDays와 id 역순 조회를 사용한다.</p>
 */
public interface MonthlyRecordRepository extends JpaRepository<MonthlyRecord, Long> {
    List<MonthlyRecord> findByPlayerAndElapsedDaysGreaterThanEqualOrderByElapsedDaysDescIdDesc(Player player, int elapsedDays);

    void deleteByPlayerAndElapsedDaysLessThan(Player player, int elapsedDays);
}
