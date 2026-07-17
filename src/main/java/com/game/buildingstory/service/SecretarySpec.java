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
        long salary = monthlySalary;
        int clamped = Math.max(1, Math.min(30, proficiency));
        for (int level = 2; level <= clamped; level++) {
            salary = roundDownToThousands((long) (salary * (1.0 + salaryRaiseRate(level))));
        }
        return salary;
    }

    private double salaryRaiseRate(int level) {
        if (level <= 10) {
            return 0.08;
        }
        if (level <= 15) {
            return 0.12;
        }
        if (level <= 20) {
            return 0.16;
        }
        if (level <= 25) {
            return 0.20;
        }
        return 0.25;
    }

    private long roundDownToThousands(long amount) {
        return amount / 1000 * 1000;
    }

    public String salaryDetail() {
        return "숙련도별 월급 인상률: 1~10 구간 레벨당 8%, 11~15 구간 12%, 16~20 구간 16%, 21~25 구간 20%, 26~30 구간 25%. 레벨마다 계산 후 천원단위 버림.";
    }

    public String specialEffectSummary() {
        return specialEffect + " " + specialEffectValue;
    }
}
