package com.game.buildingstory.repo;

import com.game.buildingstory.domain.OwnedSecretary;
import com.game.buildingstory.domain.Player;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * 고용한 비서 저장소다.
 *
 * <p>비서는 플레이어별로 보유되며, 배치 도시 기준 보너스 계산을 위해
 * secretaryKey와 assignedCity 조회가 사용된다.</p>
 */
public interface OwnedSecretaryRepository extends JpaRepository<OwnedSecretary, Long> {
    List<OwnedSecretary> findByPlayerOrderById(Player player);

    List<OwnedSecretary> findByPlayerAndAssignedCityOrderById(Player player, String assignedCity);

    Optional<OwnedSecretary> findByPlayerAndSecretaryKey(Player player, String secretaryKey);
}
