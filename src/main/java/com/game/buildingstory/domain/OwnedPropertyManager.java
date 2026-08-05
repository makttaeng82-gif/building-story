package com.game.buildingstory.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * 한 도시의 부동산 자동수리를 인계받은 관리직원이다.
 *
 * <p>관리직원은 후반부의 반복 수리를 줄이는 장치이므로 개인 이름, 능력치, 성장과 이동 상태를
 * 저장하지 않는다. 플레이어와 도시 조합을 유일하게 만들어 같은 도시에 두 번 채용되는 것도
 * 데이터베이스 수준에서 막는다.</p>
 */
@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"player_id", "city"}))
public class OwnedPropertyManager {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Player player;

    private String city;

    @Enumerated(EnumType.STRING)
    private PropertyManagerStatus status;

    private int unpaidSalaryMonths;

    protected OwnedPropertyManager() {
    }

    public OwnedPropertyManager(Player player, String city) {
        this.player = player;
        this.city = city;
        this.status = PropertyManagerStatus.ACTIVE;
        this.unpaidSalaryMonths = 0;
    }

    public Long getId() {
        return id;
    }

    public Player getPlayer() {
        return player;
    }

    public String getCity() {
        return city;
    }

    public PropertyManagerStatus getStatus() {
        return status;
    }

    public int getUnpaidSalaryMonths() {
        return unpaidSalaryMonths;
    }

    public boolean isActive() {
        return status == PropertyManagerStatus.ACTIVE;
    }

    /** 체불액을 모두 지급했을 때 자동수리를 재개하고 체불개월을 초기화한다. */
    public void recordSalaryPaid() {
        status = PropertyManagerStatus.ACTIVE;
        unpaidSalaryMonths = 0;
    }

    /** 급여일에 합산 급여를 지급하지 못하면 모든 관리직원을 같은 방식으로 중단시킨다. */
    public void recordUnpaidSalary() {
        status = PropertyManagerStatus.SALARY_SUSPENDED;
        unpaidSalaryMonths++;
    }
}
