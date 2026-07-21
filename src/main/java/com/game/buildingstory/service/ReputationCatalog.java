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
            new ReputationTier("임대업 입문", 40, false, "청주", 2),
            new ReputationTier("소형주택 임대인", 90, false, "청주", 3),
            new ReputationTier("청주 임대사업자", 190, false, "청주", 4),
            new ReputationTier("광역 투자 준비", 375, false, "세종", 1),
            new ReputationTier("세종 임대인", 600, true, "세종", 2),
            new ReputationTier("다주택 임대사업자", 900, false, "세종", 3),
            new ReputationTier("현금흐름 투자자", 1_275, false, "세종", 4),
            new ReputationTier("상가 투자자", 1_725, false, "대전", 1),
            new ReputationTier("대전 자산가", 2_250, false, "대전", 2),
            new ReputationTier("전문 자산관리자", 2_850, false, "대전", 3),
            new ReputationTier("지역 투자자", 3_600, false, "대전", 4),
            new ReputationTier("상업용 부동산 투자자", 4_500, false, "부산", 1),
            new ReputationTier("부산 자산가", 5_625, false, "부산", 2),
            new ReputationTier("중견 임대사업자", 6_750, false, "부산", 3),
            new ReputationTier("광역권 큰손", 8_250, false, "부산", 4),
            new ReputationTier("대형 자산 보유자", 10_125, false, "인천", 1),
            new ReputationTier("인천 투자자", 12_000, false, "인천", 2),
            new ReputationTier("복합자산 운용자", 14_250, false, "인천", 3),
            new ReputationTier("전국 자산가", 16_875, false, "인천", 4),
            new ReputationTier("서울 투자자", 19_500, false, "서울", 1),
            new ReputationTier("대형 빌딩 소유주", 22_500, false, "서울", 2),
            new ReputationTier("자본시장 투자자", 26_250, false, "서울", 3),
            new ReputationTier("주요 투자자", 31_500, false, "서울", 4),
            new ReputationTier("기업가 자격", 37_500, false, null, 0)
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
