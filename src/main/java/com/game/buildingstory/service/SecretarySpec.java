package com.game.buildingstory.service;

/**
 * 비서 카탈로그의 원본 스펙이다.
 *
 * <p>고용 조건, 이미지, 기본 설명, 특수 효과 계산에 필요한 값을 담는다.
 * 실제 플레이어가 보유한 비서 상태는 OwnedSecretary 엔티티에 저장된다.</p>
 */
public record SecretarySpec(
        String key,
        String name,
        String origin,
        String imagePath,
        int requiredReputation,
        long monthlySalary,
        int baseProficiency,
        String effect,
        String growthBonus,
        String growthDetail,
        String specialEffect,
        String specialEffectValue,
        String specialEffectDetail,
        String unlockNote
) {
    public int autoCheckDays(int proficiency) {
        int clamped = Math.max(1, Math.min(30, proficiency));
        return 30 - Math.floorDiv((clamped - 1) * 23, 29);
    }

    public long monthlySalaryForProficiency(int proficiency) {
        int clamped = Math.max(1, Math.min(30, proficiency));
        int salaryPercent = 100 + (clamped - 1) * 5;
        return monthlySalary * salaryPercent / 100;
    }

    public String salaryDetail() {
        return "기본 월급에서 숙련도 1단계마다 5%씩 선형 인상. 숙련도 30은 기본 월급의 245%.";
    }

    public String specialEffectSummary() {
        return specialEffect + " " + specialEffectValue;
    }
}
