package com.game.buildingstory.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "owned_secretary", uniqueConstraints =
        @UniqueConstraint(name = "uk_owned_secretary_player_key", columnNames = {"player_id", "secretary_key"}))
public class OwnedSecretary {
    /*
     * 플레이어가 고용한 비서의 성장 상태다.
     *
     * SecretarySpec은 변하지 않는 카탈로그 정보이고, OwnedSecretary는 플레이어별 숙련도,
     * 호감도, 배치 도시, 자동 수리 쿨다운처럼 플레이 중 변하는 값을 저장한다.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Player player;

    private String secretaryKey;
    private String assignedCity;
    private int proficiency;
    private Integer proficiencyExperience = 0;
    private Integer affinity = 1;
    private Integer affinityExperience = 0;
    private Integer unpaidSalaryMonths = 0;
    private Integer nextAutoRepairDay = 1;
    private Integer autoRepairsUsedInCooldown = 0;

    protected OwnedSecretary() {
    }

    public OwnedSecretary(Player player, String secretaryKey, int proficiency) {
        this.player = player;
        this.secretaryKey = secretaryKey;
        this.proficiency = proficiency;
    }

    public Long getId() {
        return id;
    }

    public Player getPlayer() {
        return player;
    }

    public String getSecretaryKey() {
        return secretaryKey;
    }

    public String getAssignedCity() {
        return assignedCity;
    }

    public int getProficiency() {
        return proficiency;
    }

    public int getProficiencyExperience() {
        return proficiencyExperience == null ? 0 : proficiencyExperience;
    }

    public int getRequiredProficiencyExperience() {
        return proficiency >= 30 ? 0 : proficiency + 2;
    }

    public int getProficiencyExperiencePercent() {
        int required = getRequiredProficiencyExperience();
        if (required == 0) {
            return 100;
        }
        return Math.max(0, Math.min(100, getProficiencyExperience() * 100 / required));
    }

    public int getAffinity() {
        return affinity == null ? 1 : affinity;
    }

    public int getAffinityExperience() {
        return affinityExperience == null ? 0 : affinityExperience;
    }

    public int getUnpaidSalaryMonths() {
        return unpaidSalaryMonths == null ? 0 : unpaidSalaryMonths;
    }

    public void recordSalaryPaid() {
        unpaidSalaryMonths = 0;
    }

    public void recordUnpaidSalary() {
        unpaidSalaryMonths = getUnpaidSalaryMonths() + 1;
        assignedCity = null;
    }

    public int getRequiredAffinityExperience() {
        return getAffinity() >= 30 ? 0 : getAffinity() + 2;
    }

    public int getAffinityExperiencePercent() {
        int required = getRequiredAffinityExperience();
        if (required == 0) {
            return 100;
        }
        return Math.max(0, Math.min(100, getAffinityExperience() * 100 / required));
    }

    public int getNextAutoRepairDay() {
        return nextAutoRepairDay == null ? 1 : nextAutoRepairDay;
    }

    public int getAutoRepairsUsedInCooldown() {
        return autoRepairsUsedInCooldown == null ? 0 : autoRepairsUsedInCooldown;
    }

    public boolean canAutoRepair(int elapsedDays) {
        return elapsedDays >= getNextAutoRepairDay();
    }

    public boolean canAutoRepair(int elapsedDays, int maxRepairsPerCooldown) {
        int safeMaxRepairs = Math.max(1, maxRepairsPerCooldown);
        return elapsedDays >= getNextAutoRepairDay()
                || (safeMaxRepairs > 1 && getAutoRepairsUsedInCooldown() < safeMaxRepairs);
    }

    public int autoRepairCooldownDaysLeft(int elapsedDays) {
        return Math.max(0, getNextAutoRepairDay() - elapsedDays);
    }

    public int autoRepairCooldownPercent(int elapsedDays, int cooldownDays) {
        int safeCooldown = Math.max(1, cooldownDays);
        int daysLeft = Math.min(safeCooldown, autoRepairCooldownDaysLeft(elapsedDays));
        return Math.max(0, Math.min(100, (safeCooldown - daysLeft) * 100 / safeCooldown));
    }

    public void startAutoRepairCooldown(int currentDay, int cooldownDays) {
        this.nextAutoRepairDay = currentDay + Math.max(1, cooldownDays);
    }

    public void recordAutoRepair(int currentDay, int cooldownDays) {
        if (currentDay >= getNextAutoRepairDay()) {
            this.nextAutoRepairDay = currentDay + Math.max(1, cooldownDays);
            this.autoRepairsUsedInCooldown = 1;
            return;
        }
        this.autoRepairsUsedInCooldown = getAutoRepairsUsedInCooldown() + 1;
    }

    public void addProficiencyExperience(int amount) {
        if (amount <= 0 || proficiency >= 30) {
            return;
        }
        proficiencyExperience = getProficiencyExperience() + amount;
        while (proficiency < 30 && getProficiencyExperience() >= getRequiredProficiencyExperience()) {
            proficiencyExperience = getProficiencyExperience() - getRequiredProficiencyExperience();
            proficiency++;
        }
        if (proficiency >= 30) {
            proficiencyExperience = 0;
        }
    }

    public void addAffinityExperience(int amount) {
        if (amount <= 0 || getAffinity() >= 30) {
            return;
        }
        affinityExperience = getAffinityExperience() + amount;
        while (getAffinity() < 30 && getAffinityExperience() >= getRequiredAffinityExperience()) {
            affinityExperience = getAffinityExperience() - getRequiredAffinityExperience();
            affinity = getAffinity() + 1;
        }
        if (getAffinity() >= 30) {
            affinity = 30;
            affinityExperience = 0;
        }
    }

    public void setProficiencyForTest(int proficiency) {
        this.proficiency = Math.max(1, Math.min(30, proficiency));
        this.proficiencyExperience = 0;
    }

    public boolean isAssignedTo(String city) {
        return city != null && city.equals(assignedCity);
    }

    public void assignTo(String city) {
        this.assignedCity = city;
    }
}
