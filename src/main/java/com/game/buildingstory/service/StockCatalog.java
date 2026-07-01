package com.game.buildingstory.service;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class StockCatalog {
    private final List<StockSpec> specs = List.of(
            new StockSpec("bytecore", "IT", "바이트코어", StockRiskType.SAFE, 82_000L),
            new StockSpec("neonsoft", "IT", "네온소프트", StockRiskType.NORMAL, 64_000L),
            new StockSpec("cloudnine", "IT", "클라우드나인", StockRiskType.AGGRESSIVE, 118_000L),
            new StockSpec("freshmill", "식품", "프레시밀", StockRiskType.SAFE, 28_000L),
            new StockSpec("goldenfood", "식품", "골든푸드", StockRiskType.NORMAL, 36_000L),
            new StockSpec("dailybrew", "식품", "데일리브루", StockRiskType.AGGRESSIVE, 19_000L),
            new StockSpec("marketway", "유통", "마켓웨이", StockRiskType.SAFE, 43_000L),
            new StockSpec("quickbox", "유통", "퀵박스", StockRiskType.NORMAL, 57_000L),
            new StockSpec("hubstore", "유통", "허브스토어", StockRiskType.AGGRESSIVE, 31_000L),
            new StockSpec("ironworks", "제조", "아이언웍스", StockRiskType.SAFE, 74_000L),
            new StockSpec("motorline", "제조", "모터라인", StockRiskType.NORMAL, 91_000L),
            new StockSpec("nextchem", "제조", "넥스트켐", StockRiskType.AGGRESSIVE, 53_000L),
            new StockSpec("signalnet", "통신", "시그널넷", StockRiskType.SAFE, 47_000L),
            new StockSpec("bluewave", "통신", "블루웨이브", StockRiskType.NORMAL, 69_000L),
            new StockSpec("linktel", "통신", "링크텔", StockRiskType.AGGRESSIVE, 39_000L)
    );

    public List<StockSpec> all() {
        return specs;
    }

    public Optional<StockSpec> find(String key) {
        return specs.stream()
                .filter(spec -> spec.key().equals(key))
                .findFirst();
    }
}
