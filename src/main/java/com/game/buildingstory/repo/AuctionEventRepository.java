package com.game.buildingstory.repo;

import com.game.buildingstory.domain.AuctionEvent;
import com.game.buildingstory.domain.AuctionStatus;
import com.game.buildingstory.domain.Player;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.Optional;

/**
 * 경매 이벤트 저장소다.
 *
 * <p>경매는 ACTIVE, RESULT, COMPLETED 상태를 오가므로 플레이어별 최신 경매를
 * 상태 조건으로 조회하는 메서드가 필요하다. 메서드 이름의 {@code OrderByIdDesc}는
 * 가장 최근에 만들어진 경매를 먼저 가져오겠다는 뜻이다.</p>
 */
public interface AuctionEventRepository extends JpaRepository<AuctionEvent, Long> {
    Optional<AuctionEvent> findFirstByPlayerAndStatusInOrderByIdDesc(Player player, Collection<AuctionStatus> statuses);
}
