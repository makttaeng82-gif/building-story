# OwnedSecretary.java 코드 주석형 해설

원본 파일: `src/main/java/com/game/buildingstory/domain/OwnedSecretary.java`

형식:
- 원본 코드를 그대로 배치한다.
- 필요한 코드 바로 아래에 해설 주석을 붙인다.
- import/package/단순 선언은 과하게 설명하지 않는다.

```java
package com.game.buildingstory.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

@Entity
// 해설: JPA 엔티티다. 이 클래스의 객체는 DB 테이블 행과 연결된다.
public class OwnedSecretary {
// 해설: class 선언이다. 이 파일의 핵심 타입을 정의한다.
    /*
     * 플레이어가 고용한 비서의 성장 상태다.
     *
     * SecretarySpec은 변하지 않는 카탈로그 정보이고, OwnedSecretary는 플레이어별 숙련도,
     * 호감도, 배치 도시, 자동 수리 쿨다운처럼 플레이 중 변하는 값을 저장한다.
     */
    @Id
    // 해설: 엔티티의 기본키 필드다. DB에서 한 행을 식별한다.
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    // 해설: DB가 기본키 값을 자동 증가 방식으로 생성한다.
    private Long id;
    // 해설: 엔티티 또는 객체의 상태 필드다. 서비스 로직은 주로 이 값을 읽거나 도메인 메서드로 변경한다.

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Player player;
    // 해설: 엔티티 또는 객체의 상태 필드다. 서비스 로직은 주로 이 값을 읽거나 도메인 메서드로 변경한다.

    private String secretaryKey;
    // 해설: 엔티티 또는 객체의 상태 필드다. 서비스 로직은 주로 이 값을 읽거나 도메인 메서드로 변경한다.
    private String assignedCity;
    // 해설: 엔티티 또는 객체의 상태 필드다. 서비스 로직은 주로 이 값을 읽거나 도메인 메서드로 변경한다.
    private int proficiency;
    // 해설: 엔티티 또는 객체의 상태 필드다. 서비스 로직은 주로 이 값을 읽거나 도메인 메서드로 변경한다.
    private Integer proficiencyExperience = 0;
    // 해설: 엔티티 또는 객체의 상태 필드다. 서비스 로직은 주로 이 값을 읽거나 도메인 메서드로 변경한다.
    private Integer affinity = 1;
    // 해설: 엔티티 또는 객체의 상태 필드다. 서비스 로직은 주로 이 값을 읽거나 도메인 메서드로 변경한다.
    private Integer affinityExperience = 0;
    // 해설: 엔티티 또는 객체의 상태 필드다. 서비스 로직은 주로 이 값을 읽거나 도메인 메서드로 변경한다.
    private Integer nextAutoRepairDay = 1;
    // 해설: 엔티티 또는 객체의 상태 필드다. 서비스 로직은 주로 이 값을 읽거나 도메인 메서드로 변경한다.
    private Integer autoRepairsUsedInCooldown = 0;
    // 해설: 엔티티 또는 객체의 상태 필드다. 서비스 로직은 주로 이 값을 읽거나 도메인 메서드로 변경한다.

    protected OwnedSecretary() {
    // 해설: 생성자다. 객체 생성 시 필요한 초기 필드 값을 받거나 JPA용 기본 생성자로 사용된다.
    }

    public OwnedSecretary(Player player, String secretaryKey, int proficiency) {
    // 해설: 생성자다. 객체 생성 시 필요한 초기 필드 값을 받거나 JPA용 기본 생성자로 사용된다.
        this.player = player;
        this.secretaryKey = secretaryKey;
        this.proficiency = proficiency;
    }

    public Long getId() {
    // 해설: `getId` 메서드 시작이다. 이 블록 안에서 해당 기능의 검증과 상태 변경이 이뤄진다.
        return id;
        // 해설: 호출자에게 결과를 반환하고 현재 메서드를 종료한다.
    }

    public Player getPlayer() {
    // 해설: `getPlayer` 메서드 시작이다. 이 블록 안에서 해당 기능의 검증과 상태 변경이 이뤄진다.
        return player;
        // 해설: 호출자에게 결과를 반환하고 현재 메서드를 종료한다.
    }

    public String getSecretaryKey() {
    // 해설: `getSecretaryKey` 메서드 시작이다. 이 블록 안에서 해당 기능의 검증과 상태 변경이 이뤄진다.
        return secretaryKey;
        // 해설: 호출자에게 결과를 반환하고 현재 메서드를 종료한다.
    }

    public String getAssignedCity() {
    // 해설: `getAssignedCity` 메서드 시작이다. 이 블록 안에서 해당 기능의 검증과 상태 변경이 이뤄진다.
        return assignedCity;
        // 해설: 호출자에게 결과를 반환하고 현재 메서드를 종료한다.
    }

    public int getProficiency() {
    // 해설: `getProficiency` 메서드 시작이다. 이 블록 안에서 해당 기능의 검증과 상태 변경이 이뤄진다.
        return proficiency;
        // 해설: 호출자에게 결과를 반환하고 현재 메서드를 종료한다.
    }

    public int getProficiencyExperience() {
    // 해설: `getProficiencyExperience` 메서드 시작이다. 이 블록 안에서 해당 기능의 검증과 상태 변경이 이뤄진다.
        return proficiencyExperience == null ? 0 : proficiencyExperience;
        // 해설: 호출자에게 결과를 반환하고 현재 메서드를 종료한다.
    }

    public int getRequiredProficiencyExperience() {
    // 해설: `getRequiredProficiencyExperience` 메서드 시작이다. 이 블록 안에서 해당 기능의 검증과 상태 변경이 이뤄진다.
        return proficiency >= 30 ? 0 : proficiency + 2;
        // 해설: 호출자에게 결과를 반환하고 현재 메서드를 종료한다.
    }

    public int getProficiencyExperiencePercent() {
    // 해설: `getProficiencyExperiencePercent` 메서드 시작이다. 이 블록 안에서 해당 기능의 검증과 상태 변경이 이뤄진다.
        int required = getRequiredProficiencyExperience();
        if (required == 0) {
        // 해설: 조건 분기다. 괄호 안 조건이 참일 때만 내부 코드가 실행된다.
            return 100;
            // 해설: 호출자에게 결과를 반환하고 현재 메서드를 종료한다.
        }
        return Math.max(0, Math.min(100, getProficiencyExperience() * 100 / required));
        // 해설: 호출자에게 결과를 반환하고 현재 메서드를 종료한다.
    }

    public int getAffinity() {
    // 해설: `getAffinity` 메서드 시작이다. 이 블록 안에서 해당 기능의 검증과 상태 변경이 이뤄진다.
        return affinity == null ? 1 : affinity;
        // 해설: 호출자에게 결과를 반환하고 현재 메서드를 종료한다.
    }

    public int getAffinityExperience() {
    // 해설: `getAffinityExperience` 메서드 시작이다. 이 블록 안에서 해당 기능의 검증과 상태 변경이 이뤄진다.
        return affinityExperience == null ? 0 : affinityExperience;
        // 해설: 호출자에게 결과를 반환하고 현재 메서드를 종료한다.
    }

    public int getRequiredAffinityExperience() {
    // 해설: `getRequiredAffinityExperience` 메서드 시작이다. 이 블록 안에서 해당 기능의 검증과 상태 변경이 이뤄진다.
        return getAffinity() >= 30 ? 0 : getAffinity() + 2;
        // 해설: 호출자에게 결과를 반환하고 현재 메서드를 종료한다.
    }

    public int getAffinityExperiencePercent() {
    // 해설: `getAffinityExperiencePercent` 메서드 시작이다. 이 블록 안에서 해당 기능의 검증과 상태 변경이 이뤄진다.
        int required = getRequiredAffinityExperience();
        if (required == 0) {
        // 해설: 조건 분기다. 괄호 안 조건이 참일 때만 내부 코드가 실행된다.
            return 100;
            // 해설: 호출자에게 결과를 반환하고 현재 메서드를 종료한다.
        }
        return Math.max(0, Math.min(100, getAffinityExperience() * 100 / required));
        // 해설: 호출자에게 결과를 반환하고 현재 메서드를 종료한다.
    }

    public int getNextAutoRepairDay() {
    // 해설: `getNextAutoRepairDay` 메서드 시작이다. 이 블록 안에서 해당 기능의 검증과 상태 변경이 이뤄진다.
        return nextAutoRepairDay == null ? 1 : nextAutoRepairDay;
        // 해설: 호출자에게 결과를 반환하고 현재 메서드를 종료한다.
    }

    public int getAutoRepairsUsedInCooldown() {
    // 해설: `getAutoRepairsUsedInCooldown` 메서드 시작이다. 이 블록 안에서 해당 기능의 검증과 상태 변경이 이뤄진다.
        return autoRepairsUsedInCooldown == null ? 0 : autoRepairsUsedInCooldown;
        // 해설: 호출자에게 결과를 반환하고 현재 메서드를 종료한다.
    }

    public boolean canAutoRepair(int elapsedDays) {
    // 해설: `canAutoRepair` 메서드 시작이다. 이 블록 안에서 해당 기능의 검증과 상태 변경이 이뤄진다.
        return elapsedDays >= getNextAutoRepairDay();
        // 해설: 호출자에게 결과를 반환하고 현재 메서드를 종료한다.
    }

    public boolean canAutoRepair(int elapsedDays, int maxRepairsPerCooldown) {
    // 해설: `canAutoRepair` 메서드 시작이다. 이 블록 안에서 해당 기능의 검증과 상태 변경이 이뤄진다.
        int safeMaxRepairs = Math.max(1, maxRepairsPerCooldown);
        return elapsedDays >= getNextAutoRepairDay()
        // 해설: 호출자에게 결과를 반환하고 현재 메서드를 종료한다.
                || (safeMaxRepairs > 1 && getAutoRepairsUsedInCooldown() < safeMaxRepairs);
    }

    public int autoRepairCooldownDaysLeft(int elapsedDays) {
    // 해설: `autoRepairCooldownDaysLeft` 메서드 시작이다. 이 블록 안에서 해당 기능의 검증과 상태 변경이 이뤄진다.
        return Math.max(0, getNextAutoRepairDay() - elapsedDays);
        // 해설: 호출자에게 결과를 반환하고 현재 메서드를 종료한다.
    }

    public int autoRepairCooldownPercent(int elapsedDays, int cooldownDays) {
    // 해설: `autoRepairCooldownPercent` 메서드 시작이다. 이 블록 안에서 해당 기능의 검증과 상태 변경이 이뤄진다.
        int safeCooldown = Math.max(1, cooldownDays);
        int daysLeft = Math.min(safeCooldown, autoRepairCooldownDaysLeft(elapsedDays));
        return Math.max(0, Math.min(100, (safeCooldown - daysLeft) * 100 / safeCooldown));
        // 해설: 호출자에게 결과를 반환하고 현재 메서드를 종료한다.
    }

    public void startAutoRepairCooldown(int currentDay, int cooldownDays) {
    // 해설: `startAutoRepairCooldown` 메서드 시작이다. 이 블록 안에서 해당 기능의 검증과 상태 변경이 이뤄진다.
        this.nextAutoRepairDay = currentDay + Math.max(1, cooldownDays);
    }

    public void recordAutoRepair(int currentDay, int cooldownDays) {
    // 해설: `recordAutoRepair` 메서드 시작이다. 이 블록 안에서 해당 기능의 검증과 상태 변경이 이뤄진다.
        if (currentDay >= getNextAutoRepairDay()) {
        // 해설: 조건 분기다. 괄호 안 조건이 참일 때만 내부 코드가 실행된다.
            this.nextAutoRepairDay = currentDay + Math.max(1, cooldownDays);
            this.autoRepairsUsedInCooldown = 1;
            return;
        }
        this.autoRepairsUsedInCooldown = getAutoRepairsUsedInCooldown() + 1;
    }

    public void addProficiencyExperience(int amount) {
    // 해설: `addProficiencyExperience` 메서드 시작이다. 이 블록 안에서 해당 기능의 검증과 상태 변경이 이뤄진다.
        if (amount <= 0 || proficiency >= 30) {
        // 해설: 조건 분기다. 괄호 안 조건이 참일 때만 내부 코드가 실행된다.
            return;
        }
        proficiencyExperience = getProficiencyExperience() + amount;
        while (proficiency < 30 && getProficiencyExperience() >= getRequiredProficiencyExperience()) {
            proficiencyExperience = getProficiencyExperience() - getRequiredProficiencyExperience();
            proficiency++;
        }
        if (proficiency >= 30) {
        // 해설: 조건 분기다. 괄호 안 조건이 참일 때만 내부 코드가 실행된다.
            proficiencyExperience = 0;
        }
    }

    public void addAffinityExperience(int amount) {
    // 해설: `addAffinityExperience` 메서드 시작이다. 이 블록 안에서 해당 기능의 검증과 상태 변경이 이뤄진다.
        if (amount <= 0 || getAffinity() >= 30) {
        // 해설: 조건 분기다. 괄호 안 조건이 참일 때만 내부 코드가 실행된다.
            return;
        }
        affinityExperience = getAffinityExperience() + amount;
        while (getAffinity() < 30 && getAffinityExperience() >= getRequiredAffinityExperience()) {
            affinityExperience = getAffinityExperience() - getRequiredAffinityExperience();
            affinity = getAffinity() + 1;
        }
        if (getAffinity() >= 30) {
        // 해설: 조건 분기다. 괄호 안 조건이 참일 때만 내부 코드가 실행된다.
            affinity = 30;
            affinityExperience = 0;
        }
    }

    public void setProficiencyForTest(int proficiency) {
    // 해설: `setProficiencyForTest` 메서드 시작이다. 이 블록 안에서 해당 기능의 검증과 상태 변경이 이뤄진다.
        this.proficiency = Math.max(1, Math.min(30, proficiency));
        this.proficiencyExperience = 0;
    }

    public boolean isAssignedTo(String city) {
    // 해설: `isAssignedTo` 메서드 시작이다. 이 블록 안에서 해당 기능의 검증과 상태 변경이 이뤄진다.
        return city != null && city.equals(assignedCity);
        // 해설: 호출자에게 결과를 반환하고 현재 메서드를 종료한다.
    }

    public void assignTo(String city) {
    // 해설: `assignTo` 메서드 시작이다. 이 블록 안에서 해당 기능의 검증과 상태 변경이 이뤄진다.
        this.assignedCity = city;
    }
}
```