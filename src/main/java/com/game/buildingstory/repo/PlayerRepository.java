package com.game.buildingstory.repo;

import com.game.buildingstory.domain.Player;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

/**
 * 플레이어 계정/상태 저장소다.
 *
 * <p>로그인은 username으로 Player를 찾고, 게임 진행은 세션의 playerId로 Player를 찾는다.</p>
 */
public interface PlayerRepository extends JpaRepository<Player, Long> {
    // tick은 여러 브라우저 탭에서 동시에 들어올 수 있으므로 해당 플레이어 행을 트랜잭션 종료까지 잠근다.
    // 먼저 들어온 tick이 날짜를 저장한 뒤 다음 요청이 최신 elapsedDays를 읽게 만드는 것이 목적이다.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select player from Player player where player.id = :playerId")
    Optional<Player> findByIdForUpdate(@Param("playerId") Long playerId);

    Optional<Player> findByUsername(String username);

    boolean existsByUsername(String username);
}
