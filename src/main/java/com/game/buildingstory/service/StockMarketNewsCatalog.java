package com.game.buildingstory.service;

import org.springframework.stereotype.Component;

import java.util.List;

import static com.game.buildingstory.service.StockNewsCertainty.CONFIRMED;
import static com.game.buildingstory.service.StockNewsCertainty.OUTLOOK;
import static com.game.buildingstory.service.StockNewsCertainty.RUMOR;
import static com.game.buildingstory.service.StockNewsDirection.NEGATIVE;
import static com.game.buildingstory.service.StockNewsDirection.NEUTRAL;
import static com.game.buildingstory.service.StockNewsDirection.POSITIVE;

/** 금리·정책·수급 등 12개 거시 사건과 3개 경기 국면 전환 기사를 관리한다. */
@Component
public class StockMarketNewsCatalog {
    private final List<StockMarketNewsDefinition> definitions = List.of(
            event("market-rate-cut", "금리", POSITIVE, OUTLOOK, "통화정책 관계자", 85, 2, 70,
                    "기준금리 인하 가능성 확대",
                    "물가 상승세가 둔화되면서 통화당국이 기준금리 인하 시점을 검토한다는 관측이 나왔다.",
                    "할인율과 기업 자금조달 비용이 낮아질 수 있다는 기대가 주식시장 전반에 반영되고 있다."),
            event("market-rate-hike", "금리", NEGATIVE, OUTLOOK, "통화정책 관계자", 90, 2, 65,
                    "추가 금리 인상 경계감 확산",
                    "예상보다 높은 물가 지표로 통화당국이 추가 긴축을 논의할 수 있다는 전망이 제기됐다.",
                    "차입 비용과 적정가치 할인율 상승 우려로 성장주와 고부채 기업의 변동성이 커질 수 있다."),
            event("market-fiscal-stimulus", "재정", POSITIVE, CONFIRMED, "정부 발표", 70, 3, 100,
                    "경기 보강 재정안 확정",
                    "정부가 설비투자와 소비 회복을 지원하는 추가 재정 집행안을 확정했다.",
                    "내수와 기업 주문 개선 기대가 커지면서 시장 전반의 이익 전망이 상향될 가능성이 있다."),
            event("market-budget-delay", "재정", NEGATIVE, RUMOR, "정책 협의 자료", 55, 2, 55,
                    "주요 경기지원 예산 집행 지연설",
                    "부처 간 협의가 길어지면서 예정된 경기지원 예산의 집행 시점이 늦춰질 수 있다는 이야기가 나왔다.",
                    "지연이 사실이면 정책 수혜를 기대했던 기업의 단기 수주와 투자 일정이 영향을 받을 수 있다."),
            event("market-credit-spread", "신용", NEGATIVE, CONFIRMED, "채권시장 통계", 80, 3, 100,
                    "회사채 신용 스프레드 확대",
                    "회사채 금리가 국채 금리보다 빠르게 오르며 기업 신용 위험에 대한 경계가 높아졌다.",
                    "차환 부담이 커질 수 있어 부채 비중이 높은 기업을 중심으로 투자심리가 약해지고 있다."),
            event("market-credit-easing", "신용", POSITIVE, CONFIRMED, "채권시장 통계", 65, 3, 100,
                    "기업 자금시장 경색 완화",
                    "회사채 발행 수요가 회복되고 신용 스프레드가 축소되면서 기업 자금조달 여건이 개선됐다.",
                    "투자계획 지연 가능성이 낮아졌다는 평가가 시장 전반에 긍정적으로 작용하고 있다."),
            event("market-foreign-inflow", "수급", POSITIVE, OUTLOOK, "시장 수급 동향", 60, 2, 70,
                    "외국계 자금 순유입 확대",
                    "대형주를 중심으로 외국계 투자자금의 순매수 규모가 늘고 있다.",
                    "유입이 이어지면 시가총액 상위 종목을 중심으로 시장 수급이 안정될 가능성이 있다."),
            event("market-foreign-outflow", "수급", NEGATIVE, OUTLOOK, "시장 수급 동향", 65, 2, 65,
                    "외국계 자금 이탈 경계",
                    "환율 변동과 대외 불확실성으로 외국계 투자자의 순매도 규모가 확대됐다.",
                    "대형주 매도 압력이 지속되면 종합지수의 단기 변동성이 높아질 수 있다."),
            event("market-currency-stable", "환율", POSITIVE, CONFIRMED, "외환시장 통계", 45, 2, 100,
                    "환율 변동성 완화",
                    "외환시장의 급격한 변동이 잦아들며 수입 원가와 외화부채에 대한 불확실성이 낮아졌다.",
                    "기업의 비용 예측 가능성이 개선됐다는 평가가 투자심리를 지지하고 있다."),
            event("market-currency-spike", "환율", NEGATIVE, RUMOR, "외환시장 관계자", 70, 2, 55,
                    "환율 급등 가능성 경계",
                    "대외 자금 흐름이 불안정해지면서 단기간 환율이 크게 오를 수 있다는 관측이 확산됐다.",
                    "원재료 수입 비용과 외화부채 부담이 커질 수 있다는 우려가 시장에 반영되고 있다."),
            event("market-confidence-up", "경기지표", POSITIVE, CONFIRMED, "경제심리 조사", 55, 2, 100,
                    "소비·기업 심리지표 동반 개선",
                    "소비자심리와 기업경기 전망이 함께 상승하며 내수 회복 신호가 확인됐다.",
                    "매출 회복 기대가 유통·식품뿐 아니라 설비투자 관련 업종으로 확산되고 있다."),
            event("market-inflation-up", "경기지표", NEGATIVE, CONFIRMED, "물가 통계", 75, 3, 100,
                    "물가 상승률 예상치 상회",
                    "소비자물가와 생산자물가가 시장 예상보다 높은 상승률을 기록했다.",
                    "원가와 금리 부담이 동시에 커질 수 있다는 우려로 시장 전반의 적정가치 기대가 낮아지고 있다."),
            regime("market-regime-expansion", POSITIVE, "경기 확장 국면 진입",
                    "생산과 소비 지표가 함께 개선되면서 시장 경기 국면이 확장으로 전환됐다.",
                    "기업 매출 성장과 투자 확대 가능성이 높아졌지만 업종별 경기 민감도에 따라 실적 차이가 발생할 수 있다."),
            regime("market-regime-neutral", NEUTRAL, "경기 중립 국면 진입",
                    "경기 선행지표의 방향성이 엇갈리면서 시장 경기 국면이 중립으로 전환됐다.",
                    "시장 공통 동력보다 개별 실적과 업종 사건이 주가에 더 큰 영향을 줄 가능성이 있다."),
            regime("market-regime-recession", NEGATIVE, "경기 침체 국면 진입",
                    "생산과 수요 지표가 둔화되면서 시장 경기 국면이 침체로 전환됐다.",
                    "경기민감 업종의 매출과 이익률 하락 가능성이 커져 기업별 재무 체력 점검이 필요하다.")
    );

    public List<StockMarketNewsDefinition> all() {
        return definitions;
    }

    public StockMarketNewsDefinition require(String key) {
        return definitions.stream().filter(event -> event.key().equals(key)).findFirst().orElseThrow();
    }

    private static StockMarketNewsDefinition event(
            String key,
            String family,
            StockNewsDirection direction,
            StockNewsCertainty certainty,
            String source,
            int priceImpactBasisPoints,
            int durationRefreshes,
            int confirmationChancePercent,
            String title,
            String firstParagraph,
            String secondParagraph
    ) {
        return definition(
                key, family, direction, certainty, source, priceImpactBasisPoints,
                durationRefreshes, confirmationChancePercent, true, title, firstParagraph, secondParagraph
        );
    }

    private static StockMarketNewsDefinition regime(
            String key,
            StockNewsDirection direction,
            String title,
            String firstParagraph,
            String secondParagraph
    ) {
        return definition(key, "경기국면", direction, CONFIRMED, "시장 종합지표", 0, 0, 100,
                false, title, firstParagraph, secondParagraph);
    }

    private static StockMarketNewsDefinition definition(
            String key,
            String family,
            StockNewsDirection direction,
            StockNewsCertainty certainty,
            String source,
            int priceImpactBasisPoints,
            int durationRefreshes,
            int confirmationChancePercent,
            boolean randomPublication,
            String title,
            String firstParagraph,
            String secondParagraph
    ) {
        return new StockMarketNewsDefinition(
                key, family, direction, certainty, source, priceImpactBasisPoints, durationRefreshes,
                confirmationChancePercent, randomPublication,
                List.of(
                        new StockMarketNewsDefinition.StockNewsVariant(title, firstParagraph, secondParagraph),
                        new StockMarketNewsDefinition.StockNewsVariant(
                                "시장 점검 · " + title,
                                firstParagraph + " 세부 지표의 지속 여부를 확인하려는 움직임이 이어지고 있다.",
                                secondParagraph
                        ),
                        new StockMarketNewsDefinition.StockNewsVariant(
                                "거시 브리핑 · " + title,
                                firstParagraph,
                                secondParagraph + " 실제 영향은 향후 발표되는 지표와 정책 강도에 따라 달라질 수 있다."
                        )
                )
        );
    }
}
