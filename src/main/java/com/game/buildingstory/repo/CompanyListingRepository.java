package com.game.buildingstory.repo;

import com.game.buildingstory.domain.CompanyListing;
import com.game.buildingstory.domain.PlayerCompany;
import com.game.buildingstory.domain.Player;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CompanyListingRepository extends JpaRepository<CompanyListing, Long> {
    Optional<CompanyListing> findByCompany(PlayerCompany company);
    Optional<CompanyListing> findByCompany_Player(Player player);
}
