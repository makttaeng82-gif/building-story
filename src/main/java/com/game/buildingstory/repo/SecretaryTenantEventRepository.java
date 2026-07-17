package com.game.buildingstory.repo;

import com.game.buildingstory.domain.OwnedBuilding;
import com.game.buildingstory.domain.Player;
import com.game.buildingstory.domain.SecretaryTenantEvent;
import com.game.buildingstory.domain.SecretaryTenantEventStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * 비서 임차인 이벤트 저장소다.
 *
 * <p>특정 비서가 건물에 임차인으로 등장하고, 요청을 해결하면 고용 가능 상태로 바뀌는 흐름을 저장한다.</p>
 */
public interface SecretaryTenantEventRepository extends JpaRepository<SecretaryTenantEvent, Long> {
    Optional<SecretaryTenantEvent> findByPlayerAndSecretaryKey(Player player, String secretaryKey);

    Optional<SecretaryTenantEvent> findByBuildingAndStatusNot(OwnedBuilding building, SecretaryTenantEventStatus status);

    @EntityGraph(attributePaths = "building")
    List<SecretaryTenantEvent> findByPlayerAndStatusNot(Player player, SecretaryTenantEventStatus status);

    List<SecretaryTenantEvent> findByPlayerAndStatus(Player player, SecretaryTenantEventStatus status);
}
