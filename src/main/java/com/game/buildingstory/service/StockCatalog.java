package com.game.buildingstory.service;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class StockCatalog {
    /*
     * 주식 종목 원본 목록이다.
     *
     * 각 종목은 업종, 이름, 위험도, 최초 기준가, 발행주식 수와 회사 설명을 가진다.
     * 가격 이력은 플레이어별 StockPriceHistory에 저장되므로 이 카탈로그 값은 초기 생성 기준으로만 사용된다.
     */
    private final List<StockSpec> specs = List.of(
            new StockSpec("bytecore", "IT", "바이트코어", StockRiskType.SAFE, 0.88, 0.85, 0.90, 82_000L, 300_000_000L, "기업용 반도체와 데이터센터 장비를 공급하는 대형 IT 기업"),
            new StockSpec("neonsoft", "IT", "네온소프트", StockRiskType.NORMAL, 1.08, 1.05, 1.80, 64_000L, 120_000_000L, "업무용 소프트웨어와 클라우드 서비스를 개발하는 성장 기업"),
            new StockSpec("cloudnine", "IT", "클라우드나인", StockRiskType.AGGRESSIVE, 1.42, 1.40, 3.50, 118_000L, 30_000_000L, "인공지능 인프라와 고성능 연산 서비스를 개발하는 기술 기업"),
            new StockSpec("freshmill", "식품", "프레시밀", StockRiskType.SAFE, 0.72, 0.75, 0.80, 28_000L, 400_000_000L, "가정간편식과 냉장 유통망을 운영하는 종합 식품 기업"),
            new StockSpec("goldenfood", "식품", "골든푸드", StockRiskType.NORMAL, 0.92, 0.95, 1.60, 36_000L, 150_000_000L, "가공식품과 외식 브랜드를 함께 운영하는 중견 식품 기업"),
            new StockSpec("dailybrew", "식품", "데일리브루", StockRiskType.AGGRESSIVE, 1.18, 1.25, 3.00, 19_000L, 80_000_000L, "커피 원두와 소형 매장 브랜드를 확장하는 소비재 기업"),
            new StockSpec("marketway", "유통", "마켓웨이", StockRiskType.SAFE, 0.80, 0.80, 0.90, 43_000L, 350_000_000L, "전국 물류망과 대형 유통 채널을 보유한 종합 유통 기업"),
            new StockSpec("quickbox", "유통", "퀵박스", StockRiskType.NORMAL, 1.05, 1.10, 1.90, 57_000L, 100_000_000L, "당일 배송과 도심 물류 거점을 운영하는 배송 전문 기업"),
            new StockSpec("hubstore", "유통", "허브스토어", StockRiskType.AGGRESSIVE, 1.32, 1.35, 3.40, 31_000L, 50_000_000L, "지역 판매자를 연결하는 온라인 상거래 플랫폼 기업"),
            new StockSpec("ironworks", "제조", "아이언웍스", StockRiskType.SAFE, 0.95, 0.90, 1.00, 74_000L, 250_000_000L, "산업용 강재와 대형 설비를 생산하는 기반 제조 기업"),
            new StockSpec("motorline", "제조", "모터라인", StockRiskType.NORMAL, 1.18, 1.15, 2.00, 91_000L, 80_000_000L, "자동차 부품과 산업용 구동장치를 생산하는 제조 기업"),
            new StockSpec("nextchem", "제조", "넥스트켐", StockRiskType.AGGRESSIVE, 1.48, 1.40, 3.80, 53_000L, 40_000_000L, "배터리 소재와 특수 화학제품을 연구하는 첨단소재 기업"),
            new StockSpec("signalnet", "통신", "시그널넷", StockRiskType.SAFE, 0.68, 0.70, 0.70, 47_000L, 500_000_000L, "전국 유선망과 기업 통신 서비스를 운영하는 대형 통신사"),
            new StockSpec("bluewave", "통신", "블루웨이브", StockRiskType.NORMAL, 0.98, 1.00, 1.70, 69_000L, 100_000_000L, "무선 통신과 미디어 서비스를 결합한 종합 통신 기업"),
            new StockSpec("linktel", "통신", "링크텔", StockRiskType.AGGRESSIVE, 1.28, 1.30, 3.20, 39_000L, 50_000_000L, "도심 특화망과 저가 통신 서비스를 확대하는 신흥 통신사")
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
