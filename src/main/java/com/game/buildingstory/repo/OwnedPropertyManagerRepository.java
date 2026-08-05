package com.game.buildingstory.repo;

import com.game.buildingstory.domain.OwnedPropertyManager;
import com.game.buildingstory.domain.Player;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/** 플레이어가 도시별로 고용한 부동산 관리직원을 저장하고 조회한다. */
public interface OwnedPropertyManagerRepository extends JpaRepository<OwnedPropertyManager, Long> {
    List<OwnedPropertyManager> findByPlayerOrderById(Player player);

    Optional<OwnedPropertyManager> findByPlayerAndCity(Player player, String city);
}
