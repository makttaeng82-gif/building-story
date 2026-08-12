package com.game.buildingstory.service;

import com.game.buildingstory.domain.OwnedPropertyManager;
import com.game.buildingstory.domain.OwnedSecretary;
import com.game.buildingstory.domain.Player;
import com.game.buildingstory.repo.OwnedPropertyManagerRepository;
import com.game.buildingstory.repo.OwnedSecretaryRepository;
import com.game.buildingstory.repo.PlayerRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

/** 기업 기능 테스트가 최종 설립 선행조건을 명시적으로 준비하도록 돕는다. */
@Component
public class CompanyFoundationTestSupport {
    private static final List<String> CITIES = List.of("청주", "세종", "대전", "부산", "인천", "서울");

    private final OwnedSecretaryRepository secretaryRepository;
    private final OwnedPropertyManagerRepository propertyManagerRepository;
    private final PlayerRepository playerRepository;

    public CompanyFoundationTestSupport(
            OwnedSecretaryRepository secretaryRepository,
            OwnedPropertyManagerRepository propertyManagerRepository,
            PlayerRepository playerRepository
    ) {
        this.secretaryRepository = secretaryRepository;
        this.propertyManagerRepository = propertyManagerRepository;
        this.playerRepository = playerRepository;
    }

    public void prepare(Player player) {
        player.unlockCompanyContent();
        playerRepository.save(player);
        for (int index = 1; index <= 6; index++) {
            String key = "secretary-" + index;
            OwnedSecretary secretary = secretaryRepository.findByPlayerAndSecretaryKey(player, key)
                    .orElseGet(() -> new OwnedSecretary(player, key, 30));
            secretary.setProficiencyForTest(30);
            secretary.addAffinityExperience(100_000);
            secretary.recordSalaryPaid();
            secretaryRepository.save(secretary);
        }
        for (String city : CITIES) {
            propertyManagerRepository.findByPlayerAndCity(player, city)
                    .orElseGet(() -> propertyManagerRepository.save(new OwnedPropertyManager(player, city)));
        }
    }

    public void clearPreparationStaff(Player player, String... retainedSecretaryKeys) {
        Set<String> retained = Set.of(retainedSecretaryKeys);
        propertyManagerRepository.deleteAll(propertyManagerRepository.findByPlayerOrderById(player));
        secretaryRepository.deleteAll(secretaryRepository.findByPlayerOrderById(player).stream()
                .filter(secretary -> !retained.contains(secretary.getSecretaryKey()))
                .toList());
    }
}
