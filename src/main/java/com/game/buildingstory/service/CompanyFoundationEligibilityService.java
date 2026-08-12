package com.game.buildingstory.service;

import com.game.buildingstory.domain.OwnedPropertyManager;
import com.game.buildingstory.domain.OwnedSecretary;
import com.game.buildingstory.domain.Player;
import com.game.buildingstory.repo.OwnedPropertyManagerRepository;
import com.game.buildingstory.repo.OwnedSecretaryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

/** 기업 설립 직전에 다시 검사해야 하는 비서 성장과 부동산 인계 조건을 계산한다. */
@Service
@Transactional(readOnly = true)
public class CompanyFoundationEligibilityService {
    private static final Set<String> REQUIRED_MANAGER_CITIES =
            Set.of("청주", "세종", "대전", "부산", "인천", "서울");

    private final OwnedSecretaryRepository ownedSecretaryRepository;
    private final OwnedPropertyManagerRepository propertyManagerRepository;

    public CompanyFoundationEligibilityService(
            OwnedSecretaryRepository ownedSecretaryRepository,
            OwnedPropertyManagerRepository propertyManagerRepository
    ) {
        this.ownedSecretaryRepository = ownedSecretaryRepository;
        this.propertyManagerRepository = propertyManagerRepository;
    }

    public Status status(Player player) {
        List<OwnedSecretary> secretaries = ownedSecretaryRepository.findByPlayerOrderById(player);
        int readySecretaries = (int) secretaries.stream()
                .filter(this::isSecretaryReady)
                .count();
        List<OwnedPropertyManager> managers = propertyManagerRepository.findByPlayerOrderById(player);
        int readyManagers = (int) managers.stream()
                .filter(OwnedPropertyManager::isActive)
                .map(OwnedPropertyManager::getCity)
                .filter(REQUIRED_MANAGER_CITIES::contains)
                .distinct()
                .count();
        return new Status(secretaries.size(), readySecretaries, managers.size(), readyManagers);
    }

    public String failureMessage(Player player) {
        Status status = status(player);
        if (!status.secretariesReady()) {
            return "비서 6명 전원의 숙련도와 호감도 30이 필요합니다.";
        }
        if (!status.propertyHandoverReady()) {
            return "청주부터 서울까지 6개 도시의 관리직원이 정상 근무 중이어야 합니다.";
        }
        return null;
    }

    private boolean isSecretaryReady(OwnedSecretary secretary) {
        return secretary.getProficiency() >= 30
                && secretary.getAffinity() >= 30
                && secretary.getUnpaidSalaryMonths() == 0;
    }

    public record Status(
            int ownedSecretaryCount,
            int readySecretaryCount,
            int propertyManagerCount,
            int readyPropertyManagerCount
    ) {
        public boolean secretariesReady() {
            return ownedSecretaryCount == 6 && readySecretaryCount == 6;
        }

        public boolean propertyHandoverReady() {
            return readyPropertyManagerCount == 6;
        }
    }
}
