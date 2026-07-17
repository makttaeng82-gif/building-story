package com.game.buildingstory.service;

import com.game.buildingstory.domain.CityMarketIndex;
import com.game.buildingstory.domain.OwnedBuilding;
import com.game.buildingstory.domain.Player;
import com.game.buildingstory.repo.CityMarketIndexRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Random;

@Service
@Transactional
public class CityMarketIndexService {
    /*
     * 도시지수 생성, 월간 변동, 뉴스 충격, 현재평가액 계산을 한곳에서 담당한다.
     * 거래·대출·경매가 이 서비스를 공유하므로 서로 다른 시장가를 사용하는 문제를 막는다.
     */
    private final Random random = new Random();
    private final CityMarketIndexRepository cityMarketIndexRepository;
    private final BuildingCatalog buildingCatalog;

    public CityMarketIndexService(CityMarketIndexRepository cityMarketIndexRepository, BuildingCatalog buildingCatalog) {
        this.cityMarketIndexRepository = cityMarketIndexRepository;
        this.buildingCatalog = buildingCatalog;
    }

    public void ensureIndexes(Player player) {
        buildingCatalog.cities().forEach(city -> index(player, city));
    }

    public void updateMonthlyIndexes(Player player) {
        // 매월 모든 도시를 함께 갱신해야 현재 머무는 도시만 가격이 변하는 편향이 생기지 않는다.
        ensureIndexes(player);
        cityMarketIndexRepository.findByPlayerOrderById(player)
                .forEach(index -> index.updateMonthly(player.getElapsedDays(), random.nextInt(121) - 60));
    }

    public void recordNewsImpact(Player player, String city, String trend) {
        // 0.5~1.5% 충격의 방향은 뉴스 종류가 결정하고 크기만 무작위로 정한다.
        int magnitude = random.nextInt(101) + 50;
        index(player, city).recordNewsImpact(SettlementService.MARKET_NEWS_RISE.equals(trend) ? magnitude : -magnitude);
    }

    @Transactional(readOnly = true)
    public int indexPoints(Player player, String city) {
        return cityMarketIndexRepository.findByPlayerAndCity(player, city)
                .map(CityMarketIndex::getIndexPoints)
                .orElse(CityMarketIndex.BASE_INDEX);
    }

    @Transactional(readOnly = true)
    public long marketValue(Player player, String city, Integer slot, long fallbackBasePrice) {
        // 슬롯이 있으면 카탈로그 기준가를 원본으로 사용한다. 저장된 과거 시장가를 계속 복리 반영하지 않기 위해서다.
        long basePrice = slot == null ? fallbackBasePrice : buildingCatalog.byCity(city).stream()
                .filter(spec -> spec.slot() == slot)
                .mapToLong(BuildingSpec::marketPrice)
                .findFirst()
                .orElse(fallbackBasePrice);
        return Math.multiplyExact(basePrice, indexPoints(player, city)) / CityMarketIndex.BASE_INDEX;
    }

    @Transactional(readOnly = true)
    public long marketValue(Player player, OwnedBuilding building) {
        return marketValue(player, building.getCity(), building.getBuildingSlot(), building.getMarketPrice());
    }

    @Transactional(readOnly = true)
    public long valuationProfit(Player player, OwnedBuilding building) {
        return marketValue(player, building) - building.getPurchasePrice();
    }

    @Transactional(readOnly = true)
    public String indexText(Player player, String city) {
        return String.format(Locale.ROOT, "%.2f", indexPoints(player, city) / 100.0);
    }

    private CityMarketIndex index(Player player, String city) {
        return cityMarketIndexRepository.findByPlayerAndCity(player, city)
                .orElseGet(() -> cityMarketIndexRepository.save(new CityMarketIndex(player, city)));
    }
}
