package com.game.buildingstory.repo;

import com.game.buildingstory.domain.Player;
import com.game.buildingstory.domain.PurchaseCooldown;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * 건물 재구매 쿨다운 저장소다.
 *
 * <p>건물을 산 직후 같은 도시/슬롯을 바로 다시 사지 못하게 elapsedDays 기준 대기일을 기록한다.</p>
 */
public interface PurchaseCooldownRepository extends JpaRepository<PurchaseCooldown, Long> {
    Optional<PurchaseCooldown> findByPlayerAndCityAndBuildingSlot(Player player, String city, int buildingSlot);

    boolean existsByPlayerAndCity(Player player, String city);
}
