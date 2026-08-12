package com.game.buildingstory.repo;

import com.game.buildingstory.domain.NpcCompanyListing;
import com.game.buildingstory.domain.NpcCompanyListingStage;
import com.game.buildingstory.domain.Player;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface NpcCompanyListingRepository extends JpaRepository<NpcCompanyListing, Long> {
    List<NpcCompanyListing> findByPlayerOrderByTargetElapsedDayAsc(Player player);
    List<NpcCompanyListing> findByPlayerAndStage(Player player, NpcCompanyListingStage stage);
    Optional<NpcCompanyListing> findByPlayerAndStockKey(Player player, String stockKey);
    long countByPlayer(Player player);
}
