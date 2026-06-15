package com.game.buildingstory.service;

import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

@Component
public class ReputationCatalog {
    private final List<ReputationTier> tiers = List.of(
            new ReputationTier("회사의 최하급노예", 0, false, null, 0),
            new ReputationTier("회사의 하급노예", 200, false, "청주", 2),
            new ReputationTier("회사의 중급노예", 400, false, "청주", 3),
            new ReputationTier("회사의 고급노예", 1000, false, "청주", 4),
            new ReputationTier("회사의 노예장", 2000, false, "세종", 1),
            new ReputationTier("먹고 살만 해졌나", 5000, true, "세종", 2),
            new ReputationTier("월천족", 8500, false, "세종", 3),
            new ReputationTier("경제적 자유인", 15000, false, "세종", 4),
            new ReputationTier("부자로의 길", 23000, false, "대전", 1),
            new ReputationTier("일반인이 생각하는 부자", 31000, false, "대전", 2),
            new ReputationTier("통계적인 부자", 38000, false, "대전", 3),
            new ReputationTier("대한민국 상위 15%", 50000, false, "대전", 4),
            new ReputationTier("지방시 큰손", 70000, false, "부산", 1),
            new ReputationTier("부자들이 인정하는 부자", 120000, false, "부산", 2),
            new ReputationTier("광역시 큰손", 250000, false, "부산", 3),
            new ReputationTier("절대적 부자", 450000, false, "부산", 4),
            new ReputationTier("재력가", 700000, false, "인천", 1),
            new ReputationTier("대한민국 상위 5%", 1_300_000, false, "인천", 2),
            new ReputationTier("억만장자", 6_000_000, false, "인천", 3),
            new ReputationTier("대한민국 200대 재벌", 17_000_000, false, "인천", 4),
            new ReputationTier("1조클럽", 40_000_000, false, "서울", 1),
            new ReputationTier("대한민국 경제실세", 70_000_000, false, "서울", 2),
            new ReputationTier("역사적인 부호", 140_000_000, false, "서울", 3),
            new ReputationTier("경제의 왕", 300_000_000, false, "서울", 4),
            new ReputationTier("기업 사냥꾼 1급", 500_000_000, false, null, 0)
    );

    public ReputationTier currentTier(int reputation, boolean resigned) {
        return tiers.stream()
                .filter(tier -> reputation >= tier.requiredReputation())
                .filter(tier -> !tier.requiresResignation() || resigned)
                .max(Comparator.comparingInt(ReputationTier::requiredReputation))
                .orElse(tiers.get(0));
    }

    public List<ReputationTier> all() {
        return tiers;
    }

    public boolean isBuildingUnlocked(String city, int slot, int reputation, boolean resigned) {
        if ("청주".equals(city) && slot == 1) {
            return true;
        }
        return tiers.stream()
                .filter(tier -> reputation >= tier.requiredReputation())
                .filter(tier -> !tier.requiresResignation() || resigned)
                .anyMatch(tier -> city.equals(tier.unlockCity()) && slot <= tier.unlockBuildingSlot());
    }

    public boolean isCityUnlocked(String city, int reputation, boolean resigned) {
        return isBuildingUnlocked(city, 1, reputation, resigned);
    }

    public List<ReputationTier> newlyUnlocked(int oldReputation, int newReputation, boolean resigned) {
        return tiers.stream()
                .filter(tier -> tier.unlockCity() != null)
                .filter(tier -> oldReputation < tier.requiredReputation() && newReputation >= tier.requiredReputation())
                .filter(tier -> !tier.requiresResignation() || resigned)
                .toList();
    }
}
