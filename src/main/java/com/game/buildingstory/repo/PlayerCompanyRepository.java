package com.game.buildingstory.repo;

import com.game.buildingstory.domain.Player;
import com.game.buildingstory.domain.PlayerCompany;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/** 플레이어별 단 하나의 법인을 저장하고 조회한다. */
public interface PlayerCompanyRepository extends JpaRepository<PlayerCompany, Long> {
    Optional<PlayerCompany> findByPlayer(Player player);
}
