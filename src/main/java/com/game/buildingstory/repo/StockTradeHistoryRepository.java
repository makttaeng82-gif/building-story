package com.game.buildingstory.repo;

import com.game.buildingstory.domain.Player;
import com.game.buildingstory.domain.StockTradeHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * 주식 체결 내역 저장소다.
 *
 * <p>보유 수량 계산과 별개로 사용자가 언제 무엇을 매수/매도했는지 보여주기 위해 별도 기록을 저장한다.</p>
 */
public interface StockTradeHistoryRepository extends JpaRepository<StockTradeHistory, Long> {
    List<StockTradeHistory> findTop12ByPlayerOrderByElapsedDaysDescIdDesc(Player player);

    List<StockTradeHistory> findByPlayerAndElapsedDaysGreaterThanEqualOrderByElapsedDaysDescIdDesc(Player player, int elapsedDays);
}
