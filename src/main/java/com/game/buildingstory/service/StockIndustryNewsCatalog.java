package com.game.buildingstory.service;

import org.springframework.stereotype.Component;

import java.util.List;

import static com.game.buildingstory.service.StockNewsCertainty.CONFIRMED;
import static com.game.buildingstory.service.StockNewsCertainty.OUTLOOK;
import static com.game.buildingstory.service.StockNewsCertainty.RUMOR;
import static com.game.buildingstory.service.StockNewsDirection.NEGATIVE;
import static com.game.buildingstory.service.StockNewsDirection.POSITIVE;

/**
 * 업종별 사건 40개의 원본 문구와 수치 규칙을 관리한다.
 * 각 사건은 의미가 다른 원인과 결과를 가지며, 같은 사건이 장기간 뒤 다시 발생하더라도
 * 제목과 기사 관점이 그대로 반복되지 않도록 세 가지 편집 문구를 만든다.
 */
@Component
public class StockIndustryNewsCatalog {
    private final List<StockIndustryNewsDefinition> definitions = List.of(
            event("it-data-center", "IT", "수요", POSITIVE, OUTLOOK, "업계 관계자", 90, 240, 3, 70,
                    "데이터센터 증설 논의, 서버 장비 발주 회복 기대",
                    "주요 사업자들이 미뤄왔던 데이터센터 증설 계획을 다시 검토하고 있다.",
                    "실제 발주로 이어지면 서버와 네트워크 장비 기업의 다음 분기 매출이 늘어날 수 있다."),
            event("it-public-cloud", "IT", "정책", POSITIVE, CONFIRMED, "정부 발표", 70, 180, 3, 100,
                    "공공 클라우드 전환 예산 확정",
                    "정부가 공공 정보시스템의 클라우드 전환 예산을 확정했다.",
                    "관련 소프트웨어와 인프라 공급사는 순차적으로 수주 기회를 확보할 전망이다."),
            event("it-chip-shortage", "IT", "공급망", NEGATIVE, CONFIRMED, "산업 통계", 100, 260, 3, 100,
                    "기업용 반도체 공급 차질 장기화",
                    "주요 생산시설의 가동 차질로 기업용 반도체 출하량이 감소했다.",
                    "부품 조달비와 납기 부담이 함께 증가해 IT 장비 업계의 수익성이 낮아질 수 있다."),
            event("it-license-cost", "IT", "원가", NEGATIVE, OUTLOOK, "증권가 분석", 65, 150, 2, 65,
                    "핵심 소프트웨어 사용료 인상 가능성",
                    "해외 기술 공급사들이 기업용 소프트웨어 사용료 조정을 검토 중인 것으로 알려졌다.",
                    "인상안이 확정되면 국내 IT 서비스 기업의 운영비 부담이 커질 가능성이 있다."),
            event("it-ai-productivity", "IT", "기술", POSITIVE, CONFIRMED, "기업 공시 종합", 80, 190, 4, 100,
                    "자동화 도입 확산으로 개발 생산성 개선",
                    "주요 IT 기업들이 반복 개발 업무에 자동화 도구를 본격 적용하고 있다.",
                    "프로젝트 수행 기간 단축과 인건비 효율 개선이 다음 분기부터 반영될 전망이다."),
            event("it-security-rule", "IT", "규제", NEGATIVE, CONFIRMED, "관계부처", 55, 120, 3, 100,
                    "보안 인증 기준 강화, 개발비 부담 증가",
                    "기업용 서비스에 적용되는 보안 인증 기준이 강화됐다.",
                    "대형사는 대응 여력이 있지만 중소형 IT 기업은 추가 개발비와 심사 지연을 부담하게 된다."),
            event("it-export-order", "IT", "수요", POSITIVE, RUMOR, "해외 매체", 75, 220, 2, 55,
                    "국산 장비 대규모 수출 협의설 확산",
                    "해외 통신사업자가 국내 IT 장비 기업들과 공급 조건을 논의한다는 보도가 나왔다.",
                    "계약 당사자와 규모가 확인되지 않아 실제 매출 반영 여부는 아직 불확실하다."),
            event("it-project-delay", "IT", "수요", NEGATIVE, RUMOR, "업계 제보", 70, 200, 2, 50,
                    "대형 전산 구축 사업 연기설",
                    "복수의 대형 전산 구축 사업이 예산 문제로 연기될 수 있다는 이야기가 퍼지고 있다.",
                    "일정이 실제 변경되면 관련 기업의 단기 수주와 매출 인식이 늦어질 수 있다."),

            event("food-grain-drop", "식품", "원가", POSITIVE, CONFIRMED, "원자재 통계", 65, 180, 3, 100,
                    "국제 곡물 가격 하락, 식품 원가 부담 완화",
                    "밀과 옥수수의 평균 수입가격이 전월보다 하락했다.",
                    "재고 교체가 진행되면 식품기업의 원재료비 부담이 점진적으로 낮아질 전망이다."),
            event("food-grain-surge", "식품", "원가", NEGATIVE, CONFIRMED, "원자재 통계", 85, 230, 4, 100,
                    "곡물 가격 급등으로 제조원가 상승",
                    "기상 악화와 수출 제한이 겹치며 주요 곡물 가격이 빠르게 올랐다.",
                    "제품 가격을 즉시 조정하기 어려운 기업은 영업이익률이 하락할 가능성이 크다."),
            event("food-school-meal", "식품", "수요", POSITIVE, CONFIRMED, "교육부 발표", 55, 130, 3, 100,
                    "공공 급식 예산 확대안 확정",
                    "학교와 공공기관 급식 예산이 확대됐다.",
                    "대량 납품망을 보유한 식품기업을 중심으로 안정적인 매출 증가가 예상된다."),
            event("food-label-rule", "식품", "규제", NEGATIVE, OUTLOOK, "관계부처", 45, 110, 2, 75,
                    "식품 표시 기준 개정 예고",
                    "원산지와 영양정보 표시 항목을 확대하는 개정안이 예고됐다.",
                    "포장 교체와 품질관리 비용이 발생해 단기 수익성에 부담이 될 수 있다."),
            event("food-export-demand", "식품", "수요", POSITIVE, OUTLOOK, "무역 통계", 70, 190, 3, 70,
                    "가공식품 수출 주문 증가세",
                    "주요 시장의 가공식품 주문 문의와 통관 물량이 함께 늘고 있다.",
                    "증가세가 유지되면 생산설비 가동률과 다음 분기 매출이 개선될 가능성이 있다."),
            event("food-cold-chain", "식품", "공급망", NEGATIVE, CONFIRMED, "물류업계", 55, 140, 3, 100,
                    "냉장 운송 단가 인상 확정",
                    "전력비와 차량 유지비 상승을 이유로 냉장 운송 단가가 인상됐다.",
                    "신선식품 비중이 높은 기업일수록 물류비 증가 영향을 크게 받을 전망이다."),
            event("food-franchise-boom", "식품", "수요", POSITIVE, RUMOR, "가맹업계", 60, 170, 2, 55,
                    "외식 가맹점 출점 확대설",
                    "복수의 식품 브랜드가 신규 가맹점 모집 규모를 늘린다는 이야기가 나오고 있다.",
                    "실제 계약 수와 초기 출점 비용은 확인되지 않아 수익 기여도는 불확실하다."),
            event("food-recall-rumor", "식품", "품질", NEGATIVE, RUMOR, "온라인 제보", 80, 210, 2, 45,
                    "일부 가공식품 품질 조사설 확산",
                    "감독기관이 특정 원재료를 사용하는 제품을 조사 중이라는 소문이 퍼졌다.",
                    "공식 회수 명령은 없지만 사실로 확인되면 업종 전반의 판매와 비용에 영향을 줄 수 있다."),

            event("retail-consumption", "유통", "수요", POSITIVE, OUTLOOK, "소비동향 조사", 65, 170, 3, 70,
                    "소비심리 회복 조짐, 유통 매출 개선 기대",
                    "가계의 선택적 소비 의향과 카드 사용액이 함께 반등했다.",
                    "회복세가 유지되면 오프라인 매장과 온라인 플랫폼 모두 거래액이 증가할 수 있다."),
            event("retail-delivery-cost", "유통", "원가", NEGATIVE, CONFIRMED, "물류업계", 70, 190, 3, 100,
                    "택배 단가 인상, 유통업계 배송비 부담 확대",
                    "주요 물류사들이 계약 배송 단가를 인상했다.",
                    "무료배송 비중이 높은 기업은 비용을 판매자나 소비자에게 전가하기 전까지 수익성이 낮아진다."),
            event("retail-holiday-sales", "유통", "수요", POSITIVE, CONFIRMED, "결제 통계", 75, 210, 3, 100,
                    "연휴 판매액 예상 상회",
                    "연휴 기간 온·오프라인 결제액이 사전 예상치를 웃돌았다.",
                    "재고 회전과 플랫폼 수수료 수입이 개선돼 이번 분기 실적에 긍정적으로 반영될 전망이다."),
            event("retail-platform-rule", "유통", "규제", NEGATIVE, OUTLOOK, "공정당국", 55, 140, 3, 70,
                    "온라인 플랫폼 수수료 규제 검토",
                    "당국이 판매자 수수료와 광고 노출 기준을 손보는 방안을 검토하고 있다.",
                    "규제가 시행되면 플랫폼 기업의 수수료 수익과 운영 방식이 제약받을 수 있다."),
            event("retail-warehouse-auto", "유통", "기술", POSITIVE, CONFIRMED, "기업 공시 종합", 55, 150, 4, 100,
                    "자동화 물류센터 가동률 상승",
                    "주요 유통기업의 자동화 물류센터가 안정화 단계에 들어섰다.",
                    "처리량 증가와 오배송 감소로 주문당 물류비가 낮아질 전망이다."),
            event("retail-inventory", "유통", "재고", NEGATIVE, CONFIRMED, "유통 통계", 65, 180, 3, 100,
                    "계절상품 재고 증가, 할인판매 압력 확대",
                    "예상보다 낮은 판매량으로 계절상품 재고가 늘었다.",
                    "재고 소진을 위한 할인과 보관비 증가가 다음 분기 이익률에 부담이 된다."),
            event("retail-merchant-growth", "유통", "수요", POSITIVE, RUMOR, "판매자 커뮤니티", 55, 150, 2, 55,
                    "대형 판매자 플랫폼 이동설",
                    "복수의 대형 판매자가 새로운 유통 플랫폼으로 입점을 검토한다는 소문이 나왔다.",
                    "입점이 확정되면 거래액이 늘지만 현재까지 계약 사실은 공개되지 않았다."),
            event("retail-data-leak", "유통", "운영", NEGATIVE, RUMOR, "보안업계", 75, 200, 2, 45,
                    "유통 플랫폼 고객정보 사고 조사설",
                    "한 유통 플랫폼의 고객정보 관리 실태를 조사 중이라는 이야기가 확산됐다.",
                    "사고가 확인되면 보상비와 이용자 이탈이 발생할 수 있으나 당국 발표는 아직 없다."),

            event("manufacturing-export", "제조", "수요", POSITIVE, CONFIRMED, "무역 통계", 80, 220, 4, 100,
                    "산업재 수출 주문 증가",
                    "기계와 자동차 부품의 수출 계약액이 전분기보다 증가했다.",
                    "수주잔고가 늘면서 제조기업의 설비 가동률과 매출 가시성이 개선됐다."),
            event("manufacturing-steel", "제조", "원가", NEGATIVE, CONFIRMED, "원자재 통계", 85, 230, 4, 100,
                    "산업용 강재 가격 급등",
                    "주요 강재의 현물가격과 장기 공급계약 가격이 함께 상승했다.",
                    "원가 전가가 늦은 부품기업을 중심으로 영업이익률 하락이 예상된다."),
            event("manufacturing-subsidy", "제조", "정책", POSITIVE, CONFIRMED, "산업부 발표", 65, 170, 3, 100,
                    "첨단 제조설비 투자지원 확정",
                    "정부가 자동화와 친환경 설비 투자에 대한 지원안을 확정했다.",
                    "투자 부담이 낮아지고 생산 효율 개선 시점도 앞당겨질 전망이다."),
            event("manufacturing-strike", "제조", "생산", NEGATIVE, OUTLOOK, "노사협의 자료", 70, 190, 2, 65,
                    "주요 생산단지 노사협상 난항",
                    "근무체계와 임금 조정을 둘러싼 협상이 합의점을 찾지 못하고 있다.",
                    "파업으로 이어지면 생산량 감소와 납품 지연이 발생할 수 있다."),
            event("manufacturing-automation", "제조", "기술", POSITIVE, OUTLOOK, "설비업계", 55, 150, 4, 75,
                    "스마트공장 전환 속도 빨라져",
                    "주요 제조사가 검사와 조립 공정의 자동화 투자를 확대하고 있다.",
                    "초기 비용은 발생하지만 불량률과 단위 생산비가 낮아질 가능성이 있다."),
            event("manufacturing-safety", "제조", "규제", NEGATIVE, CONFIRMED, "고용당국", 50, 130, 3, 100,
                    "산업안전 점검 확대, 생산 일정 조정",
                    "고위험 제조시설을 대상으로 정기 점검 범위가 확대됐다.",
                    "설비 보완과 일시 가동중단이 필요한 기업은 단기 생산비가 증가할 전망이다."),
            event("manufacturing-contract", "제조", "수요", POSITIVE, RUMOR, "해외 산업지", 75, 220, 2, 55,
                    "국내 부품사 대형 공급계약설",
                    "해외 완성품 업체가 국내 부품기업과 장기 공급을 협의한다는 보도가 나왔다.",
                    "협상 대상과 계약 금액이 공개되지 않아 실적 기여 여부는 아직 확인할 수 없다."),
            event("manufacturing-defect", "제조", "품질", NEGATIVE, RUMOR, "협력사 제보", 80, 220, 2, 45,
                    "수출 부품 품질검사 확대설",
                    "일부 수출 부품의 품질 기준 충족 여부를 재검토한다는 소문이 돌고 있다.",
                    "대규모 재검사로 이어지면 납기 지연과 보증비용이 발생할 수 있다."),

            event("telecom-data-demand", "통신", "수요", POSITIVE, CONFIRMED, "통신 통계", 55, 140, 4, 100,
                    "기업 데이터 사용량 증가세 지속",
                    "기업용 회선과 데이터센터 연결 트래픽이 꾸준히 증가했다.",
                    "고용량 회선 수요가 늘면서 통신사의 기업서비스 매출이 개선될 전망이다."),
            event("telecom-spectrum", "통신", "원가", NEGATIVE, CONFIRMED, "규제기관", 70, 190, 4, 100,
                    "주파수 재할당 비용 인상 확정",
                    "차기 주파수 재할당 대가가 기존 예상보다 높은 수준으로 결정됐다.",
                    "통신사는 현금 지출과 감가상각 부담이 늘어 수익성이 낮아질 수 있다."),
            event("telecom-government-network", "통신", "정책", POSITIVE, CONFIRMED, "정부 발표", 60, 160, 3, 100,
                    "공공 초고속망 구축사업 확대",
                    "지역 공공시설의 초고속 통신망 구축 예산이 확정됐다.",
                    "유선망과 기업영업 조직을 보유한 통신기업의 신규 수주가 예상된다."),
            event("telecom-price-cut", "통신", "규제", NEGATIVE, OUTLOOK, "정책 간담회", 55, 150, 3, 70,
                    "통신요금 인하 압력 확대",
                    "가계 통신비 부담을 낮추기 위한 요금제 개편 논의가 시작됐다.",
                    "인하 폭이 커지면 가입자당 매출과 마케팅 전략의 조정이 필요하다."),
            event("telecom-network-share", "통신", "원가", POSITIVE, OUTLOOK, "업계 협의체", 45, 120, 4, 75,
                    "통신망 공동투자 협의 진전",
                    "중복 설비투자를 줄이기 위한 공동 구축 기준이 논의되고 있다.",
                    "협의가 확정되면 신규망 투자비와 유지비를 절감할 수 있다."),
            event("telecom-outage", "통신", "운영", NEGATIVE, CONFIRMED, "장애 보고서", 65, 170, 2, 100,
                    "지역 통신장애로 보상비 발생",
                    "대규모 지역 통신장애가 발생해 이용자 보상 절차가 시작됐다.",
                    "직접 보상비와 품질 개선 투자가 단기 비용으로 반영될 전망이다."),
            event("telecom-enterprise-deal", "통신", "수요", POSITIVE, RUMOR, "업계 관계자", 55, 150, 2, 55,
                    "대형 기업전용망 계약설",
                    "복수의 대기업이 통신망 운영사를 변경하는 협상을 진행한다는 이야기가 나왔다.",
                    "계약이 성사되면 장기 매출이 늘지만 현재는 협상 당사자가 확인되지 않았다."),
            event("telecom-security", "통신", "운영", NEGATIVE, RUMOR, "보안업계", 65, 180, 2, 45,
                    "통신망 보안 취약점 조사설",
                    "핵심 통신장비의 보안 취약점을 점검하고 있다는 소문이 확산됐다.",
                    "문제가 확인되면 장비 교체와 고객 대응 비용이 발생할 수 있다."
            )
    );

    public List<StockIndustryNewsDefinition> all() {
        return definitions;
    }

    private static StockIndustryNewsDefinition event(
            String key,
            String industry,
            String family,
            StockNewsDirection direction,
            StockNewsCertainty certainty,
            String source,
            int priceImpactBasisPoints,
            int financialImpactBasisPoints,
            int durationRefreshes,
            int confirmationChancePercent,
            String title,
            String firstParagraph,
            String secondParagraph
    ) {
        return new StockIndustryNewsDefinition(
                key, industry, family, direction, certainty, source,
                priceImpactBasisPoints, financialImpactBasisPoints, durationRefreshes,
                confirmationChancePercent,
                List.of(
                        new StockIndustryNewsDefinition.StockNewsVariant(title, firstParagraph, secondParagraph),
                        new StockIndustryNewsDefinition.StockNewsVariant(
                                "업계 점검 · " + title,
                                firstParagraph + " 관련 지표가 이전 관측과 다른 흐름을 보이면서 업계가 대응책을 검토하고 있다.",
                                secondParagraph
                        ),
                        new StockIndustryNewsDefinition.StockNewsVariant(
                                industry + " 업종, " + title,
                                firstParagraph,
                                secondParagraph + " 실제 영향의 크기는 기업별 비용 구조와 대응 속도에 따라 달라질 수 있다."
                        )
                )
        );
    }
}
