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
        long maximumMonthlySalary,
        int baseProficiency,
        String effect,
        String growthBonus,
        String growthDetail,
        String specialEffect,
        String specialEffectValue,
        String specialEffectDetail,
        String unlockNote
) {
    private static final long MINIMUM_MONTHLY_SALARY = 1_500_000L;
    public int autoCheckDays(int proficiency) {
        int clamped = Math.max(1, Math.min(30, proficiency));
        return 30 - Math.floorDiv((clamped - 1) * 23, 29);
    }

    public long monthlySalaryForProficiency(int proficiency) {
        int clamped = Math.max(1, Math.min(30, proficiency));
        long progress = clamped - 1L;
        long salaryRange = maximumMonthlySalary - MINIMUM_MONTHLY_SALARY;
        return MINIMUM_MONTHLY_SALARY + salaryRange * progress * progress / (29L * 29L);
    }

    public String salaryDetail() {
        return "모든 비서는 숙련도 1에서 월 150만원으로 시작하며 숙련도가 높아질수록 인상 폭이 커집니다. 숙련도 30의 월급은 모두 1,750만원입니다.";
    }

    public String specialEffectSummary() {
        return specialEffect + " " + specialEffectValue;
    }
}
