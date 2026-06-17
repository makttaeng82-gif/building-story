package com.game.buildingstory.service;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class GiftItemCatalog {
    private final List<GiftItemSpec> items = List.of(
            new GiftItemSpec("coffee-beans", "고급 원두세트", 150_000L, "/assets/shop/gift-coffee-beans.png", 1, 10, 1),
            new GiftItemSpec("fountain-pen", "만년필", 1_000_000L, "/assets/shop/gift-fountain-pen.png", 11, 15, 1),
            new GiftItemSpec("premium-perfume", "프리미엄 향수", 10_000_000L, "/assets/shop/gift-premium-perfume.png", 16, 20, 1),
            new GiftItemSpec("jewelry", "최고급 보석", 150_000_000L, "/assets/shop/gift-jewelry.png", 21, 25, 1),
            new GiftItemSpec("incentive", "인센티브", 1_500_000_000L, "/assets/shop/gift-incentive.png", 26, 30, 1)
    );

    public List<GiftItemSpec> all() {
        return items;
    }

    public Optional<GiftItemSpec> find(String key) {
        return items.stream()
                .filter(item -> item.key().equals(key))
                .findFirst();
    }
}
