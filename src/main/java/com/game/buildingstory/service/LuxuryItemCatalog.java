package com.game.buildingstory.service;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class LuxuryItemCatalog {
    /*
     * 명품/고가 소비 아이템 목록이다.
     *
     * 플레이어가 현금을 평판이나 장기 목표 달성으로 전환할 때 사용하는 상점 데이터다.
     */
    private final List<LuxuryItemSpec> items = List.of(
            new LuxuryItemSpec("bicycle", "자전거", 300_000L, 10, "/assets/shop/bicycle.png"),
            new LuxuryItemSpec("k5-ilguner", "K5-ilguner", 30_000_000L, 100, "/assets/shop/k5-ilguner.png"),
            new LuxuryItemSpec("grandeur", "그랜저", 50_000_000L, 150, "/assets/shop/grandeur.png"),
            new LuxuryItemSpec("montblanc-watch", "몽블랑 시계", 70_000_000L, 250, "/assets/shop/montblanc-watch.png"),
            new LuxuryItemSpec("supercar", "슈퍼카", 300_000_000L, 700, "/assets/shop/supercar.png"),
            new LuxuryItemSpec("yacht", "요트", 3_000_000_000L, 1_500, "/assets/shop/yacht.png"),
            new LuxuryItemSpec("private-prop-plane", "개인용 경비행기", 15_000_000_000L, 2_500, "/assets/shop/private-prop-plane.png"),
            new LuxuryItemSpec("private-jet", "전용기", 50_000_000_000L, 4_000, "/assets/shop/private-jet.png")
    );

    public List<LuxuryItemSpec> all() {
        return items;
    }

    public Optional<LuxuryItemSpec> find(String key) {
        return items.stream()
                .filter(item -> item.key().equals(key))
                .findFirst();
    }
}
