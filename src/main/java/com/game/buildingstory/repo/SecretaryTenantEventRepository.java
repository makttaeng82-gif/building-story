package com.game.buildingstory.repo;

import com.game.buildingstory.domain.OwnedBuilding;
import com.game.buildingstory.domain.Player;
import com.game.buildingstory.domain.SecretaryTenantEvent;
import com.game.buildingstory.domain.SecretaryTenantEventStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SecretaryTenantEventRepository extends JpaRepository<SecretaryTenantEvent, Long> {
    Optional<SecretaryTenantEvent> findByPlayerAndSecretaryKey(Player player, String secretaryKey);

    Optional<SecretaryTenantEvent> findByBuildingAndStatusNot(OwnedBuilding building, SecretaryTenantEventStatus status);

    @EntityGraph(attributePaths = "building")
    List<SecretaryTenantEvent> findByPlayerAndStatusNot(Player player, SecretaryTenantEventStatus status);

    List<SecretaryTenantEvent> findByPlayerAndStatus(Player player, SecretaryTenantEventStatus status);
}
