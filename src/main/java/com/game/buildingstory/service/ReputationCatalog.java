package com.game.buildingstory.service;

import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

@Component
public class ReputationCatalog {
    /*
     * 평판에 따른 칭호와 도시 해금 조건을 관리한다.
     *
     * 플레이어의 reputation 숫자를 사람이 읽을 수 있는 칭호와 컨텐츠 해금 여부로 변환한다.
     */
    private final List<ReputationTier> tiers = List.of(
            new ReputationTier("첫 건물주", 0, false, null, 0),
            new ReputationTier("임대업 입문", 50, false, "청주", 2),
            new ReputationTier("소형주택 임대인", 120, false, "청주", 3),
            new ReputationTier("청주 임대사업자", 250, false, "청주", 4),
            new ReputationTier("광역 투자 준비", 500, false, "세종", 1),
            new ReputationTier("세종 임대인", 800, true, "세종", 2),
            new ReputationTier("다주택 임대사업자", 1_200, false, "세종", 3),
            new ReputationTier("현금흐름 투자자", 1_700, false, "세종", 4),
            new ReputationTier("상가 투자자", 2_300, false, "대전", 1),
            new ReputationTier("대전 자산가", 3_000, false, "대전", 2),
            new ReputationTier("전문 자산관리자", 3_800, false, "대전", 3),
            new ReputationTier("지역 투자자", 4_800, false, "대전", 4),
            new ReputationTier("상업용 부동산 투자자", 6_000, false, "부산", 1),
            new ReputationTier("부산 자산가", 7_500, false, "부산", 2),
            new ReputationTier("중견 임대사업자", 9_000, false, "부산", 3),
            new ReputationTier("광역권 큰손", 11_000, false, "부산", 4),
            new ReputationTier("대형 자산 보유자", 13_500, false, "인천", 1),
            new ReputationTier("인천 투자자", 16_000, false, "인천", 2),
            new ReputationTier("복합자산 운용자", 19_000, false, "인천", 3),
            new ReputationTier("전국 자산가", 22_500, false, "인천", 4),
            new ReputationTier("서울 투자자", 26_000, false, "서울", 1),
            new ReputationTier("대형 빌딩 소유주", 30_000, false, "서울", 2),
            new ReputationTier("자본시장 투자자", 35_000, false, "서울", 3),
            new ReputationTier("주요 투자자", 42_000, false, "서울", 4),
            new ReputationTier("기업가 자격", 50_000, false, null, 0)
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
