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
@Table(uniqueConstraints = @UniqueConstraint(name = "uk_city_market_index_player_city", columnNames = {"player_id", "city"}))
public class CityMarketIndex {
    /*
     * 플레이어 한 명의 도시 한 곳에 대한 부동산 가격지수 원장이다.
     * 10,000이 카탈로그 기준가의 100%이며 10,200이면 같은 건물을 기준가의 102%로 평가한다.
     * 건물마다 시장가를 직접 덮어쓰지 않고 이 값 하나를 움직여 같은 도시의 모든 자산을 일관되게 평가한다.
     */
    public static final int BASE_INDEX = 10_000;
    private static final int MIN_INDEX = 7_000;
    private static final int MAX_INDEX = 16_000;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Player player;

    private String city;
    // 100배 정수로 저장해 부동소수점 누적 오차 없이 소수 둘째 자리 지수를 표현한다.
    private int indexPoints = BASE_INDEX;
    // 월중 발생한 부동산 뉴스는 즉시 가격을 바꾸지 않고 다음 월간 갱신 때 한 번만 합산한다.
    private int pendingNewsBasisPoints;
    // 같은 게임 날짜에 정산이 중복 호출되어도 지수를 두 번 변경하지 않기 위한 기준값이다.
    private Integer lastUpdatedElapsedDays;

    protected CityMarketIndex() {
    }

    public CityMarketIndex(Player player, String city) {
        this.player = player;
        this.city = city;
    }

    public String getCity() {
        return city;
    }

    public int getIndexPoints() {
        return indexPoints;
    }

    public int getPendingNewsBasisPoints() {
        return pendingNewsBasisPoints;
    }

    public void recordNewsImpact(int basisPoints) {
        // 뉴스 한 건의 영향은 설계 상 최대 ±1.5%이므로 입력 단계에서 범위를 제한한다.
        pendingNewsBasisPoints = Math.max(-150, Math.min(150, basisPoints));
    }

    public boolean updateMonthly(int elapsedDays, int ordinaryBasisPoints) {
        if (lastUpdatedElapsedDays != null && lastUpdatedElapsedDays == elapsedDays) {
            return false;
        }
        // 일반 변동과 뉴스 충격을 합쳐도 한 달 전체 변동은 ±2%를 넘을 수 없다.
        int totalBasisPoints = Math.max(-200, Math.min(200, ordinaryBasisPoints + pendingNewsBasisPoints));
        long nextIndex = Math.round(indexPoints * (10_000.0 + totalBasisPoints) / 10_000.0);
        indexPoints = (int) Math.max(MIN_INDEX, Math.min(MAX_INDEX, nextIndex));
        pendingNewsBasisPoints = 0;
        lastUpdatedElapsedDays = elapsedDays;
        return true;
    }
}
