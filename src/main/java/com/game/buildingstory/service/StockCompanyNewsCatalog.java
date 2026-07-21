package com.game.buildingstory.service;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

import static com.game.buildingstory.service.StockNewsCertainty.CONFIRMED;
import static com.game.buildingstory.service.StockNewsCertainty.OUTLOOK;
import static com.game.buildingstory.service.StockNewsCertainty.RUMOR;
import static com.game.buildingstory.service.StockNewsDirection.NEGATIVE;
import static com.game.buildingstory.service.StockNewsDirection.POSITIVE;

/** 15개 상장기업의 사업 특성을 반영한 기업 고유 사건 90개를 관리한다. */
@Component
public class StockCompanyNewsCatalog {
    private final List<StockCompanyNewsDefinition> definitions;

    public StockCompanyNewsCatalog() {
        List<CompanyProfile> profiles = List.of(
                profile("bytecore", "바이트코어", "IT",
                        "데이터센터용 칩 장기 공급계약 협의", "해외 데이터센터 운영사와 기업용 반도체 공급 조건을 협의 중인 것으로 알려졌다.",
                        "첨단 패키징 공정 수율 개선", "신규 패키징 공정의 수율이 목표 수준에 도달했다고 공시했다.",
                        "서버용 칩 결함 조사설", "일부 서버용 반도체의 결함 가능성을 조사한다는 제보가 시장에 퍼졌다.",
                        "차세대 생산라인 투자비 부담", "차세대 생산라인 증설에 필요한 자금을 차입으로 조달하는 방안을 검토하고 있다."),
                profile("neonsoft", "네온소프트", "IT",
                        "대기업 업무 플랫폼 전환 수주 추진", "대기업 계열사의 업무 플랫폼을 자사 클라우드로 전환하는 계약을 논의 중이다.",
                        "클라우드 운영 자동화 완료", "서버 배치와 장애 대응 절차를 자동화해 운영 인력을 효율화했다고 밝혔다.",
                        "핵심 고객 계약 갱신 지연설", "주요 고객사의 장기 소프트웨어 계약 갱신이 늦어지고 있다는 이야기가 나왔다.",
                        "해외 서비스 확장비용 증가", "해외 데이터센터와 현지 영업조직 구축비를 외부 자금으로 충당할 계획이다."),
                profile("cloudnine", "클라우드나인", "IT",
                        "AI 연산서비스 대형 예약계약 추진", "연구기관과 고성능 AI 연산자원 장기 예약계약을 협의하고 있다.",
                        "신형 가속기 전력효율 개선", "신형 연산 가속기의 전력당 처리량이 기존 제품을 웃돌았다고 발표했다.",
                        "AI 인프라 납품 일정 지연설", "핵심 장비 조달 문제로 대형 인프라 납품이 늦어질 수 있다는 소문이 확산됐다.",
                        "연구개발 차입 확대 가능성", "차세대 모델과 전용 장비 개발비를 마련하기 위해 추가 차입을 검토하고 있다."),
                profile("freshmill", "프레시밀", "식품",
                        "대형 급식업체 간편식 공급 협의", "전국 급식망에 냉장 간편식을 공급하는 계약을 협의 중인 것으로 알려졌다.",
                        "냉장 생산라인 폐기율 감소", "공정 개선으로 유통기한 내 폐기되는 제품 비율을 낮췄다고 밝혔다.",
                        "일부 냉장제품 회수 검토설", "특정 생산일자의 냉장제품을 대상으로 품질 확인이 진행 중이라는 제보가 나왔다.",
                        "신규 냉장물류센터 비용 부담", "신규 물류센터 건설비 일부를 장기 차입으로 조달할 계획이다."),
                profile("goldenfood", "골든푸드", "식품",
                        "외식 브랜드 해외 가맹계약 추진", "해외 유통사와 외식 브랜드 마스터프랜차이즈 계약을 논의하고 있다.",
                        "원재료 공동구매 체계 확대", "계열 브랜드의 원재료 구매를 통합해 매입단가를 낮췄다고 공시했다.",
                        "주력 가공식품 판매 둔화설", "주력 제품의 재주문 물량이 예상보다 적다는 유통업계 관측이 나왔다.",
                        "노후 공장 교체투자 부담", "생산설비 교체와 안전시설 보강에 필요한 자금을 차입으로 마련할 예정이다."),
                profile("dailybrew", "데일리브루", "식품",
                        "편의점 전용 원두 공급계약 협의", "대형 편의점 체인과 자체브랜드 원두 공급계약을 추진하고 있다.",
                        "소형 매장 표준화로 운영비 절감", "매장 장비와 메뉴를 표준화해 점포당 운영비를 낮췄다고 밝혔다.",
                        "신규 가맹점 폐점 증가설", "최근 출점한 일부 가맹점의 매출 부진과 폐점 가능성이 제기됐다.",
                        "직영점 확대자금 조달 부담", "핵심 상권 직영점 확보를 위해 단기 차입을 늘리는 방안을 검토하고 있다."),
                profile("marketway", "마켓웨이", "유통",
                        "공공 생필품 유통계약 추진", "지역 공공기관과 생필품 통합 유통계약을 협의하고 있다.",
                        "전국 물류망 통합배차 완료", "권역별 배차시스템을 통합해 공차 운행과 배송 지연을 줄였다고 발표했다.",
                        "대형 점포 매출 감소설", "일부 대형 점포의 방문객과 객단가가 동시에 낮아졌다는 관측이 나왔다.",
                        "노후 물류거점 재건축 부담", "핵심 물류거점 재건축 비용을 회사채와 장기 차입으로 조달할 계획이다."),
                profile("quickbox", "퀵박스", "유통",
                        "대형 온라인몰 당일배송 계약 협의", "대형 온라인몰의 수도권 당일배송 물량을 전담하는 계약을 추진 중이다.",
                        "도심 거점 분류시간 단축", "자동분류 장비 개선으로 주문 마감 이후 처리시간을 줄였다고 밝혔다.",
                        "배송기사 이탈 증가설", "일부 지역에서 배송기사 계약 해지가 늘고 있다는 현장 제보가 나왔다.",
                        "도심 물류거점 임차비 부담", "신규 도심 거점의 보증금과 설비비를 차입으로 조달하는 방안을 검토하고 있다."),
                profile("hubstore", "허브스토어", "유통",
                        "대형 브랜드 공식몰 입점 협의", "복수의 소비재 브랜드와 공식 온라인몰 입점 조건을 논의하고 있다.",
                        "광고 추천시스템 전환율 개선", "개인화 추천시스템 개편 이후 광고 구매전환율이 개선됐다고 발표했다.",
                        "판매자 정산 지연설", "일부 판매자의 정산일이 늦어졌다는 주장이 커뮤니티를 통해 확산됐다.",
                        "시장점유율 확보 마케팅비 부담", "신규 이용자 확보를 위한 마케팅비를 외부 조달자금으로 충당할 계획이다."),
                profile("ironworks", "아이언웍스", "제조",
                        "대형 플랜트용 강재 공급협의", "해외 플랜트 사업에 산업용 강재를 장기 공급하는 계약을 협의 중이다.",
                        "고로 에너지 사용량 절감", "공정 열회수 설비를 개선해 제품당 에너지 사용량을 낮췄다고 공시했다.",
                        "주요 설비 가동중단 조사설", "핵심 생산설비의 이상으로 정밀점검이 진행 중이라는 소문이 나왔다.",
                        "친환경 설비전환 자금 부담", "배출저감 설비와 전기로 전환에 필요한 자금을 장기 차입으로 마련할 예정이다."),
                profile("motorline", "모터라인", "제조",
                        "차세대 구동장치 공급계약 추진", "완성차 업체와 차세대 구동장치 장기 공급계약을 협상 중인 것으로 알려졌다.",
                        "부품 공용화로 생산비 절감", "차종별로 달랐던 핵심 부품을 공용화해 조달비와 조립시간을 줄였다고 밝혔다.",
                        "납품 부품 내구성 조사설", "일부 납품 부품의 내구성 기준을 재검토한다는 업계 제보가 나왔다.",
                        "해외 생산거점 증설 부담", "해외 조립공장 증설비를 현지 금융기관 차입으로 조달할 계획이다."),
                profile("nextchem", "넥스트켐", "제조",
                        "고밀도 배터리 소재 공동개발 추진", "배터리 제조사와 고밀도 양극재 공동개발 및 공급계약을 논의하고 있다.",
                        "촉매 회수공정 원가 절감", "희소금속 촉매 회수율을 높여 원재료 투입비를 줄였다고 발표했다.",
                        "시험생산 수율 부진설", "신규 배터리 소재의 시험생산 수율이 목표에 못 미친다는 소문이 확산됐다.",
                        "상업생산 설비투자 차입 부담", "연구단계 제품의 상업생산 설비를 마련하기 위해 대규모 차입을 검토하고 있다."),
                profile("signalnet", "시그널넷", "통신",
                        "공공기관 전용회선 계약협의", "복수의 공공기관과 전국 전용회선 통합계약을 논의하고 있다.",
                        "유선망 장애예측 시스템 도입", "장비 이상을 사전에 탐지하는 시스템으로 현장 출동비를 줄였다고 밝혔다.",
                        "노후 교환기 장애 조사설", "일부 지역 노후 교환기의 반복 장애를 조사 중이라는 제보가 나왔다.",
                        "전국 광망 교체비용 부담", "노후 광망과 교환설비 교체비를 장기 회사채로 조달할 계획이다."),
                profile("bluewave", "블루웨이브", "통신",
                        "미디어 결합요금제 제휴 추진", "대형 콘텐츠 사업자와 통신·미디어 결합상품 출시를 협의하고 있다.",
                        "기지국 전력관리 비용 절감", "트래픽에 따라 기지국 전력을 조절하는 시스템으로 운영비를 낮췄다고 발표했다.",
                        "가입자 순감 확대설", "주력 요금제의 번호이동 이탈이 예상보다 많다는 시장 관측이 나왔다.",
                        "콘텐츠 판권 확보비 부담", "장기 콘텐츠 판권 확보에 필요한 선급금을 외부 자금으로 조달할 계획이다."),
                profile("linktel", "링크텔", "통신",
                        "도심 특화망 운영계약 추진", "대형 복합시설의 전용 통신망을 운영하는 계약을 협의 중이다.",
                        "저가요금제 개통 자동화", "가입과 개통 절차를 자동화해 고객당 처리비용을 낮췄다고 밝혔다.",
                        "망 임차계약 조건 악화설", "핵심 통신망 임차단가가 크게 오를 수 있다는 업계 소문이 퍼졌다.",
                        "가입자 확보 보조금 부담", "신규 가입자 유치를 위한 단말기 지원비 일부를 단기 차입으로 충당할 계획이다.")
        );
        List<StockCompanyNewsDefinition> built = new ArrayList<>();
        profiles.forEach(profile -> built.addAll(events(profile)));
        this.definitions = List.copyOf(built);
    }

    public List<StockCompanyNewsDefinition> all() {
        return definitions;
    }

    private List<StockCompanyNewsDefinition> events(CompanyProfile profile) {
        SupplementalProfile supplemental = supplemental(profile.stockKey());
        return List.of(
                event(profile, "growth", "수주", POSITIVE, OUTLOOK, "업계 관계자", 90, 2, 70,
                        new StockCompanyFinancialEffect(450, 0, 0, 0), profile.growthTitle(), profile.growthLead(),
                        "계약이 확정되면 다음 분기 매출과 생산 가동률이 개선될 수 있다."),
                event(profile, "efficiency", "생산성", POSITIVE, CONFIRMED, "회사 공시", 65, 3, 100,
                        new StockCompanyFinancialEffect(0, -80, 0, 0), profile.efficiencyTitle(), profile.efficiencyLead(),
                        "개선 효과는 다음 분기 영업비용률에 반영될 전망이다."),
                event(profile, "setback", "운영위험", NEGATIVE, RUMOR, "미확인 제보", 100, 2, 55,
                        new StockCompanyFinancialEffect(-350, 70, -80, 0), profile.setbackTitle(), profile.setbackLead(),
                        "사실로 확인되면 매출 지연과 추가 대응비용이 함께 발생할 수 있다."),
                event(profile, "finance", "재무", NEGATIVE, OUTLOOK, "금융업계", 75, 2, 65,
                        new StockCompanyFinancialEffect(-100, 40, -50, 400), profile.financeTitle(), profile.financeLead(),
                        "조달이 실행되면 현금 여력은 늘지만 이자비용과 부채비율이 함께 상승한다."),
                event(profile, "product", "신사업", POSITIVE, OUTLOOK, "산업계", 80, 2, 65,
                        new StockCompanyFinancialEffect(250, 30, 0, 0), supplemental.productTitle(), supplemental.productLead(),
                        "사업화가 예정대로 진행되면 다음 분기 매출이 늘지만 초기 출시비용도 함께 발생한다."),
                event(profile, "compliance", "규제품질", NEGATIVE, RUMOR, "업계 제보", 85, 2, 60,
                        new StockCompanyFinancialEffect(-200, 50, -40, 0), supplemental.complianceTitle(), supplemental.complianceLead(),
                        "사실로 확인되면 판매 차질과 점검·보상비용이 다음 분기 실적에 반영될 수 있다.")
        );
    }

    private SupplementalProfile supplemental(String stockKey) {
        return switch (stockKey) {
            case "bytecore" -> new SupplementalProfile(
                    "저전력 AI 추론칩 시제품 공개", "데이터센터 전력 사용량을 줄이는 AI 추론칩 시제품을 주요 고객사에 공개한 것으로 알려졌다.",
                    "반도체 수출승인 심사 지연설", "일부 고성능 제품의 수출승인 심사가 예상보다 길어지고 있다는 업계 제보가 나왔다.");
            case "neonsoft" -> new SupplementalProfile(
                    "기업용 생성형 AI 서비스 시험 운영", "기존 업무 플랫폼에 생성형 AI 기능을 결합한 서비스를 주요 고객사와 시험 운영 중이다.",
                    "고객정보 접근권한 관리 조사설", "클라우드 관리계정의 접근권한 절차를 내부 조사하고 있다는 제보가 퍼졌다.");
            case "cloudnine" -> new SupplementalProfile(
                    "산업별 AI 모델 장터 개설 추진", "기업 고객이 검증된 AI 모델을 선택해 사용하는 전용 장터 출시를 준비하고 있다.",
                    "연산서비스 표시성능 검증설", "일부 상품의 표시성능과 실제 처리량 차이를 점검한다는 업계 관측이 나왔다.");
            case "freshmill" -> new SupplementalProfile(
                    "고단백 냉장 간편식 출시 추진", "운동·건강 수요를 겨냥한 고단백 냉장 간편식 제품군의 납품을 협의하고 있다.",
                    "냉장 유통온도 관리 점검설", "일부 배송구간의 냉장 유통온도 기록을 다시 확인하고 있다는 제보가 나왔다.");
            case "goldenfood" -> new SupplementalProfile(
                    "식물성 외식 브랜드 시험 출시", "식물성 원료를 사용한 신규 외식 브랜드를 핵심 상권에서 시험 운영할 계획이다.",
                    "가맹점 원산지 표시 조사설", "일부 가맹점의 원산지 표시 절차를 본사가 점검하고 있다는 소문이 확산됐다.");
            case "dailybrew" -> new SupplementalProfile(
                    "무인 소형매장 모델 공개", "주문과 결제를 자동화한 소형매장을 오피스 상권에 시험 출점할 예정이다.",
                    "가맹계약 정보공개 점검설", "신규 가맹계약의 비용 안내 절차를 관계기관이 살펴보고 있다는 관측이 나왔다.");
            case "marketway" -> new SupplementalProfile(
                    "지역 생산자 직거래관 확대", "지역 농수산물 생산자와 소비자를 연결하는 직거래 매장을 전국으로 확대할 계획이다.",
                    "납품대금 정산절차 조사설", "일부 협력사의 납품대금 정산기간을 내부 점검하고 있다는 업계 제보가 나왔다.");
            case "quickbox" -> new SupplementalProfile(
                    "신선식품 새벽배송 진출 추진", "기존 도심 물류망을 활용한 신선식품 새벽배송 서비스를 시험할 예정이다.",
                    "배송 안전기준 준수 점검설", "일부 지역 배송거점의 작업·운행 안전기준을 조사한다는 제보가 나왔다.");
            case "hubstore" -> new SupplementalProfile(
                    "판매자 금융서비스 출시 추진", "입점 판매자의 매출채권을 기반으로 운전자금을 제공하는 서비스를 준비하고 있다.",
                    "검색광고 표시방식 조사설", "광고상품과 일반 검색결과의 구분 표시를 내부 점검 중이라는 관측이 나왔다.");
            case "ironworks" -> new SupplementalProfile(
                    "저탄소 강재 제품군 공개", "재생원료와 전기로를 활용한 저탄소 강재를 건설·조선 고객사에 공개했다.",
                    "산업시설 배출기준 점검설", "주요 생산시설의 대기배출 측정자료를 관계기관이 확인 중이라는 소문이 퍼졌다.");
            case "motorline" -> new SupplementalProfile(
                    "상용차 전동화 부품 시험 공급", "전기 상용차용 통합 구동모듈을 완성차 업체에 시험 공급할 계획이다.",
                    "부품 인증자료 재검토설", "일부 차종에 납품한 부품의 인증자료를 다시 검토하고 있다는 업계 제보가 나왔다.");
            case "nextchem" -> new SupplementalProfile(
                    "배터리 재활용 소재 사업 진출", "폐배터리에서 회수한 금속을 양극재 원료로 재사용하는 시험사업을 추진하고 있다.",
                    "화학물질 보관절차 점검설", "시험공장의 화학물질 보관·기록 절차를 내부 점검 중이라는 소문이 확산됐다.");
            case "signalnet" -> new SupplementalProfile(
                    "산업단지 전용 5G망 출시 추진", "제조설비 연결에 특화된 산업단지 전용 통신망 서비스를 준비하고 있다.",
                    "통신장애 보고절차 조사설", "최근 지역 장애의 보고와 복구 절차를 관계기관이 확인한다는 관측이 나왔다.");
            case "bluewave" -> new SupplementalProfile(
                    "스포츠 중계 구독상품 출시 추진", "통신요금과 실시간 스포츠 중계를 묶은 신규 구독상품을 준비하고 있다.",
                    "결합상품 해지조건 점검설", "일부 결합상품의 해지·환급 안내가 적절했는지 내부 점검 중이라는 제보가 나왔다.");
            case "linktel" -> new SupplementalProfile(
                    "중소사업자 보안통신 상품 출시", "소형 사업장의 회선과 보안관제를 묶은 정액형 통신상품을 준비하고 있다.",
                    "망 품질 고지기준 조사설", "일부 지역의 실제 통신품질이 고지기준을 충족하는지 조사한다는 관측이 나왔다.");
            default -> throw new IllegalArgumentException("기업 보조 사건 정보 없음: " + stockKey);
        };
    }

    private StockCompanyNewsDefinition event(
            CompanyProfile profile,
            String suffix,
            String family,
            StockNewsDirection direction,
            StockNewsCertainty certainty,
            String source,
            int priceImpactBasisPoints,
            int durationRefreshes,
            int confirmationChancePercent,
            StockCompanyFinancialEffect effect,
            String title,
            String firstParagraph,
            String secondParagraph
    ) {
        String fullTitle = profile.name() + ", " + title;
        return new StockCompanyNewsDefinition(
                profile.stockKey() + "-" + suffix,
                profile.stockKey(),
                profile.name(),
                profile.industry(),
                family,
                direction,
                certainty,
                source,
                priceImpactBasisPoints,
                durationRefreshes,
                confirmationChancePercent,
                effect,
                List.of(
                        new StockCompanyNewsDefinition.StockNewsVariant(fullTitle, firstParagraph, secondParagraph),
                        new StockCompanyNewsDefinition.StockNewsVariant(
                                "기업 점검 · " + fullTitle,
                                firstParagraph + " 시장은 구체적인 규모와 적용 시점을 확인하고 있다.",
                                secondParagraph
                        ),
                        new StockCompanyNewsDefinition.StockNewsVariant(
                                fullTitle + "에 투자자 주목",
                                firstParagraph,
                                secondParagraph + " 실제 영향은 후속 공시와 분기 실적에서 확인해야 한다."
                        )
                )
        );
    }

    private static CompanyProfile profile(
            String stockKey, String name, String industry,
            String growthTitle, String growthLead,
            String efficiencyTitle, String efficiencyLead,
            String setbackTitle, String setbackLead,
            String financeTitle, String financeLead
    ) {
        return new CompanyProfile(
                stockKey, name, industry,
                growthTitle, growthLead, efficiencyTitle, efficiencyLead,
                setbackTitle, setbackLead, financeTitle, financeLead
        );
    }

    private record CompanyProfile(
            String stockKey,
            String name,
            String industry,
            String growthTitle,
            String growthLead,
            String efficiencyTitle,
            String efficiencyLead,
            String setbackTitle,
            String setbackLead,
            String financeTitle,
            String financeLead
    ) {
    }

    private record SupplementalProfile(
            String productTitle,
            String productLead,
            String complianceTitle,
            String complianceLead
    ) {
    }
}
