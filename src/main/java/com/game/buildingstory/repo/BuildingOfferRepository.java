package com.game.buildingstory.repo;

import com.game.buildingstory.domain.BuildingOffer;
import com.game.buildingstory.domain.Player;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * 현재 시장에 나온 부동산 매물 저장소다.
 *
 * <p>매물은 플레이어와 도시별로 따로 존재한다. 도시 화면은 현재 도시의 매물만 보여주므로
 * {@code findByPlayerAndCityOrderById} 같은 조회가 자주 쓰인다.</p>
 */
public interface BuildingOfferRepository extends JpaRepository<BuildingOffer, Long> {
    List<BuildingOffer> findByPlayerAndCityOrderById(Player player, String city);

    boolean existsByPlayerAndCity(Player player, String city);

    void deleteByPlayerAndCity(Player player, String city);
}
