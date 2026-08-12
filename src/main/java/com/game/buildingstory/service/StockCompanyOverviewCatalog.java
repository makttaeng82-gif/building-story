package com.game.buildingstory.service;

import org.springframework.stereotype.Component;

import java.util.List;

/** 초기 15개와 신규상장 후보 16개의 대표·본사·수익원·핵심 위험을 관리한다. */
@Component
public class StockCompanyOverviewCatalog {
    private final List<StockCompanyOverview> overviews = List.of(
            overview("bytecore", "한도윤", "22년차", "서울", "데이터센터용 반도체", "설비투자·수율·대형 고객 의존", "중간"),
            overview("neonsoft", "윤서진", "14년차", "서울", "기업용 소프트웨어 구독", "대형 고객 계약 갱신·인건비", "높음"),
            overview("cloudnine", "강이준", "8년차", "대전", "AI 연산 인프라 사용료", "전력비·장비투자·기술 변화", "매우 높음"),
            overview("freshmill", "박수현", "27년차", "청주", "가정간편식·냉장 유통", "원재료 가격·재고 폐기", "낮음"),
            overview("goldenfood", "정하린", "19년차", "서울", "가공식품·외식 브랜드", "원가 전가·브랜드 경쟁", "중간"),
            overview("dailybrew", "오재민", "9년차", "부산", "원두 납품·가맹점 수수료", "점포 확장·원두 가격", "높음"),
            overview("marketway", "김태성", "31년차", "서울", "대형 유통·물류 서비스", "소비 둔화·낮은 영업이익률", "중간"),
            overview("quickbox", "이선우", "11년차", "인천", "당일배송 계약·물류 대행", "인건비·차량비·물동량 변동", "높음"),
            overview("hubstore", "최유나", "7년차", "부산", "판매 수수료·온라인 광고", "판매자 이탈·플랫폼 규제", "매우 높음"),
            overview("ironworks", "장현석", "38년차", "부산", "산업용 강재·대형 설비", "원자재 가격·대규모 설비", "높음"),
            overview("motorline", "문지훈", "24년차", "대전", "자동차 부품 장기 공급", "완성차 수요·품질 보증", "높음"),
            overview("nextchem", "서민재", "10년차", "세종", "배터리 소재·특수 화학제품", "연구개발비·양산 수율", "매우 높음"),
            overview("signalnet", "임준호", "34년차", "서울", "기업 회선·유선망 이용료", "망 투자·규제 비용", "낮음"),
            overview("bluewave", "배지윤", "17년차", "인천", "무선통신·미디어 구독", "가입자 경쟁·콘텐츠 비용", "중간"),
            overview("linktel", "조은찬", "8년차", "부산", "도심 특화망·저가 요금제", "가입자 확보비·망 임차료", "매우 높음"),
            overview("corevision", "김도현", "9년차", "서울", "산업 안전 AI 구독", "대형 고객 의존·AI 규제", "높음"),
            overview("greentable", "윤가영", "18년차", "청주", "건강 간편식·냉장 유통", "원재료 가격·재고 관리", "낮음"),
            overview("logibridge", "박준서", "12년차", "인천", "물류 중개·창고 관리", "물동량·인건비", "높음"),
            overview("hanbitrobotics", "이현우", "7년차", "대전", "협동로봇·제어장치", "수주 변동·양산 수율", "매우 높음"),
            overview("orbitalnet", "정유진", "6년차", "세종", "위성 회선·통신망 이용료", "발사 일정·대규모 설비", "매우 높음"),
            overview("dataforge", "서지훈", "15년차", "서울", "데이터 보관·가공 사용료", "전력비·보안 사고", "중간"),
            overview("wellnessfood", "한수빈", "10년차", "부산", "기능성 식품·영양 구독", "인허가·브랜드 경쟁", "중간"),
            overview("metrocommerce", "최민석", "26년차", "서울", "생활유통·식자재 공급", "낮은 이익률·소비 둔화", "낮음"),
            overview("solidcell", "송재원", "11년차", "대전", "전력저장장치·설비 공급", "원자재 가격·품질 보증", "높음"),
            overview("skylink", "강혜진", "29년차", "서울", "기업 전용망·데이터 회선", "망 투자·요금 규제", "낮음"),
            overview("cubelogic", "문태윤", "5년차", "성남", "AI 반도체·최적화 SW", "개발비·고객사 채택", "매우 높음"),
            overview("tastelab", "임채린", "6년차", "세종", "대체식품·식품 소재", "연구개발·시장 수용성", "매우 높음"),
            overview("pickandgo", "조시우", "5년차", "서울", "상거래 수수료·배송", "마케팅비·배송 원가", "매우 높음"),
            overview("ecometal", "배성호", "33년차", "인천", "재생금속·산업 소재", "원자재 시황·설비 투자", "중간"),
            overview("nexusmobile", "오지아", "13년차", "서울", "알뜰통신·기업 모바일", "가입자 경쟁·망 임차료", "중간"),
            overview("auroralabs", "류하진", "4년차", "서울", "생성형 미디어 AI 구독", "저작권·높은 연산비", "매우 높음")
    );

    public StockCompanyOverview require(String stockKey) {
        return overviews.stream()
                .filter(overview -> overview.stockKey().equals(stockKey))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("기업 개요가 없습니다: " + stockKey));
    }

    public List<StockCompanyOverview> all() {
        return overviews;
    }

    private StockCompanyOverview overview(
            String stockKey,
            String chiefExecutive,
            String foundedText,
            String headquarters,
            String mainRevenueSource,
            String keyRisk,
            String cyclicality
    ) {
        return new StockCompanyOverview(
                stockKey, chiefExecutive, foundedText, headquarters,
                mainRevenueSource, keyRisk, cyclicality
        );
    }
}
