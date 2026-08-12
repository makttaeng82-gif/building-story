package com.game.buildingstory.service;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class StockCatalog {
    private static final int INITIAL_COMPANY_COUNT = 15;
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
            new StockSpec("linktel", "통신", "링크텔", StockRiskType.AGGRESSIVE, 1.28, 1.30, 3.20, 39_000L, 50_000_000L, "도심 특화망과 저가 통신 서비스를 확대하는 신흥 통신사"),
            new StockSpec("corevision", "IT", "코어비전", StockRiskType.NORMAL, 1.08, 1.10, 1.90, 43_600L, 120_000_000L, "기업용 영상 분석과 산업 안전 AI를 공급하는 기술 기업"),
            new StockSpec("greentable", "식품", "그린테이블", StockRiskType.SAFE, 0.76, 0.78, 0.90, 34_200L, 300_000_000L, "건강 간편식과 냉장 식품 유통망을 운영하는 식품 기업"),
            new StockSpec("logibridge", "유통", "로지브릿지", StockRiskType.NORMAL, 1.07, 1.12, 2.00, 45_500L, 140_000_000L, "기업 간 물류 중개와 통합 창고 관리 서비스를 제공하는 유통 기업"),
            new StockSpec("hanbitrobotics", "제조", "한빛로보틱스", StockRiskType.AGGRESSIVE, 1.42, 1.35, 3.40, 55_400L, 70_000_000L, "스마트공장용 협동로봇과 제어장치를 개발하는 제조 기업"),
            new StockSpec("orbitalnet", "통신", "오비탈넷", StockRiskType.AGGRESSIVE, 1.50, 1.45, 3.80, 31_300L, 100_000_000L, "저궤도 위성통신과 원격지역 통신망을 구축하는 통신 기업"),
            new StockSpec("dataforge", "IT", "데이터포지", StockRiskType.SAFE, 0.84, 0.90, 1.00, 73_400L, 240_000_000L, "기업 데이터 보관과 가공 인프라를 운영하는 IT 기업"),
            new StockSpec("wellnessfood", "식품", "웰니스푸드", StockRiskType.NORMAL, 0.98, 1.00, 1.80, 30_400L, 140_000_000L, "기능성 식품과 개인 맞춤 영양 서비스를 제공하는 식품 기업"),
            new StockSpec("metrocommerce", "유통", "메트로커머스", StockRiskType.SAFE, 0.78, 0.82, 0.90, 25_500L, 500_000_000L, "도심 생활유통망과 기업 식자재 공급망을 운영하는 유통 기업"),
            new StockSpec("solidcell", "제조", "솔리드셀", StockRiskType.NORMAL, 1.12, 1.15, 2.10, 56_600L, 140_000_000L, "산업용 전력저장장치와 배터리 설비를 생산하는 제조 기업"),
            new StockSpec("skylink", "통신", "스카이링크", StockRiskType.SAFE, 0.74, 0.76, 0.80, 40_700L, 400_000_000L, "기업 전용망과 데이터 회선을 운영하는 통신 기업"),
            new StockSpec("cubelogic", "IT", "큐브로직", StockRiskType.AGGRESSIVE, 1.48, 1.45, 3.90, 89_900L, 50_000_000L, "엣지 AI 반도체와 최적화 소프트웨어를 개발하는 기술 기업"),
            new StockSpec("tastelab", "식품", "테이스트랩", StockRiskType.AGGRESSIVE, 1.30, 1.25, 3.20, 33_700L, 140_000_000L, "대체식품과 식품 소재를 연구하는 성장형 식품 기업"),
            new StockSpec("pickandgo", "유통", "픽앤고", StockRiskType.AGGRESSIVE, 1.38, 1.40, 3.60, 37_100L, 140_000_000L, "실시간 상거래와 초고속 배송 플랫폼을 운영하는 유통 기업"),
            new StockSpec("ecometal", "제조", "에코메탈", StockRiskType.SAFE, 0.90, 0.90, 1.00, 62_100L, 320_000_000L, "재생금속 정련과 산업 소재 공급을 담당하는 제조 기업"),
            new StockSpec("nexusmobile", "통신", "넥서스모바일", StockRiskType.NORMAL, 1.02, 1.05, 1.80, 37_900L, 200_000_000L, "알뜰통신과 기업용 모바일 서비스를 제공하는 통신 기업"),
            new StockSpec("auroralabs", "IT", "오로라랩스", StockRiskType.AGGRESSIVE, 1.55, 1.50, 4.10, 81_700L, 60_000_000L, "생성형 미디어 AI와 콘텐츠 제작 도구를 개발하는 기술 기업")
    );

    /** 게임 시작부터 거래되는 기존 15개 종목이다. */
    public List<StockSpec> initial() {
        return specs.subList(0, INITIAL_COMPANY_COUNT);
    }

    /** 상장 예정 후보까지 포함한 전체 정의다. 실제 화면 노출 여부는 StockUniverseService가 결정한다. */
    public List<StockSpec> all() {
        return specs;
    }

    public Optional<StockSpec> find(String key) {
        return specs.stream()
                .filter(spec -> spec.key().equals(key))
                .findFirst();
    }

    public Optional<StockSpec> findInitial(String key) {
        return initial().stream().filter(spec -> spec.key().equals(key)).findFirst();
    }
}
