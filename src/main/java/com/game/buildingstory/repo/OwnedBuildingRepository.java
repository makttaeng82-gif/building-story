package com.game.buildingstory.repo;

import com.game.buildingstory.domain.OwnedBuilding;
import com.game.buildingstory.domain.Player;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * 플레이어 보유 건물 저장소다.
 *
 * <p>도시별 보유 건물 수, 입주 중인 건물, 수리 요청 건물처럼 게임 진행에서
 * 자주 필요한 상태를 조회한다.</p>
 */
public interface OwnedBuildingRepository extends JpaRepository<OwnedBuilding, Long> {
    List<OwnedBuilding> findByPlayerOrderById(Player player);

    List<OwnedBuilding> findByPlayerAndCityOrderById(Player player, String city);

    long countByPlayerAndCity(Player player, String city);
}
