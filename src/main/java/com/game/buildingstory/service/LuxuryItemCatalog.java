package com.game.buildingstory.service;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class LuxuryItemCatalog {
    private final List<LuxuryItemSpec> items = List.of(
            new LuxuryItemSpec("bicycle", "자전거", 300_000L, "/assets/shop/bicycle.png"),
            new LuxuryItemSpec("k5-ilguner", "K5-ilguner", 30_000_000L, "/assets/shop/k5-ilguner.png"),
            new LuxuryItemSpec("grandeur", "그렌져", 50_000_000L, "/assets/shop/grandeur.png"),
            new LuxuryItemSpec("montblanc-watch", "몽블랑 시계", 70_000_000L, "/assets/shop/montblanc-watch.png"),
            new LuxuryItemSpec("supercar", "슈퍼카", 300_000_000L, "/assets/shop/supercar.png"),
            new LuxuryItemSpec("yacht", "요트", 3_000_000_000L, "/assets/shop/yacht.png"),
            new LuxuryItemSpec("private-prop-plane", "개인용 경비행기", 15_000_000_000L, "/assets/shop/private-prop-plane.png"),
            new LuxuryItemSpec("private-jet", "전용기", 50_000_000_000L, "/assets/shop/private-jet.png")
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
