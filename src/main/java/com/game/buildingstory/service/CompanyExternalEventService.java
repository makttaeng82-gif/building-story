package com.game.buildingstory.service;

import com.game.buildingstory.domain.CompanyCompetitor;
import com.game.buildingstory.domain.CompanyDepartmentType;
import com.game.buildingstory.domain.CompanyExternalEvent;
import com.game.buildingstory.domain.CompanyExternalEventCategory;
import com.game.buildingstory.domain.CompanyNewsArticle;
import com.game.buildingstory.domain.CompanyNewsCategory;
import com.game.buildingstory.domain.PlayerCompany;
import com.game.buildingstory.repo.CompanyCompetitorRepository;
import com.game.buildingstory.repo.CompanyDepartmentRepository;
import com.game.buildingstory.repo.CompanyExternalEventRepository;
import com.game.buildingstory.repo.CompanyNewsArticleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 월별 시장·산업 이슈를 만들고 활성 이슈의 계산 보정값을 제공한다.
 * 사건 결과를 직접 현금으로 지급하지 않고 각 도메인의 기존 계산식만 제한적으로 보정한다.
 */
@Service
public class CompanyExternalEventService {
    private static final int MONTHLY_EVENT_CHANCE = 65;
    private static final int MAXIMUM_ACTIVE_EVENTS = 2;

    private static final List<EventSpec> CATALOG = List.of(
            spec("market-adoption", CompanyExternalEventCategory.AI_MARKET, 3,
                    "생성형 AI 대중 이용 확산", "개인과 소규모 사업자의 AI 서비스 이용이 빠르게 늘고 있다."),
            spec("market-paid-conversion", CompanyExternalEventCategory.AI_MARKET, 3,
                    "업무용 AI 유료전환 증가", "업무 생산성 도구에 비용을 지불하려는 이용자가 증가했다."),
            spec("market-fatigue", CompanyExternalEventCategory.AI_MARKET, 2,
                    "AI 구독 피로감 확산", "다수의 구독 서비스에 대한 이용자 부담이 커지고 있다."),
            spec("market-premium-slowdown", CompanyExternalEventCategory.AI_MARKET, 2,
                    "고가 AI 요금제 수요 둔화", "고가 요금제의 비용 대비 효용을 재검토하는 고객이 늘었다."),

            targeted("competitor-next-model", CompanyExternalEventCategory.COMPETITOR, 1, "frontier",
                    "기술선도사 차세대 모델 발표", "프론티어AI가 성능을 높인 차세대 모델을 공개했다."),
            targeted("competitor-marketing", CompanyExternalEventCategory.COMPETITOR, 3, "popular",
                    "대중화기업 대규모 홍보", "모두AI가 전 채널을 활용한 대규모 이용자 확보에 나섰다."),
            targeted("competitor-security", CompanyExternalEventCategory.COMPETITOR, 1, "trust",
                    "신뢰중심사 보안 인증 획득", "트러스트랩이 기업 고객 대상 보안 인증을 획득했다."),
            targeted("competitor-outage", CompanyExternalEventCategory.COMPETITOR, 2, "frontier",
                    "경쟁사 대규모 서비스 장애", "프론티어AI의 서비스 장애가 장시간 이어지고 있다."),

            spec("infra-cloud-price-up", CompanyExternalEventCategory.INFRASTRUCTURE, 3,
                    "클라우드 사업자 요금 인상", "AI 연산 수요 증가로 외부 클라우드 이용료가 인상됐다."),
            spec("infra-cloud-discount", CompanyExternalEventCategory.INFRASTRUCTURE, 3,
                    "장기계약 할인 경쟁", "클라우드 사업자들이 기업 고객 장기계약 할인 경쟁에 들어갔다."),
            spec("infra-accelerator-shortage", CompanyExternalEventCategory.INFRASTRUCTURE, 2,
                    "AI 가속기 공급난", "고성능 가속기 공급 부족으로 연산망 구축 일정과 비용 부담이 커졌다."),
            spec("infra-accelerator-supply", CompanyExternalEventCategory.INFRASTRUCTURE, 2,
                    "차세대 가속기 공급 확대", "차세대 가속기 출하가 늘면서 연산망 구축 여건이 개선됐다."),

            spec("regulation-security-review", CompanyExternalEventCategory.REGULATION, 3,
                    "기업 AI 보안심사 강화", "기업용 AI 도입 과정에서 보안 검증 기준이 강화됐다."),
            spec("regulation-copyright-uncertain", CompanyExternalEventCategory.REGULATION, 2,
                    "AI 저작권 기준 불확실성", "AI 학습과 생성물의 권리 기준이 불명확해 개발 검토가 늘었다."),
            spec("regulation-copyright-guide", CompanyExternalEventCategory.REGULATION, 2,
                    "AI 저작권 가이드라인 확정", "AI 학습과 생성물 이용에 관한 실무 가이드라인이 확정됐다."),
            spec("regulation-public-support", CompanyExternalEventCategory.REGULATION, 2,
                    "공공 AI 실증사업 지원", "공공기관의 AI 실증사업 예산과 참여기업 지원이 확대됐다."),

            spec("demand-manufacturing", CompanyExternalEventCategory.ENTERPRISE_DEMAND, 2,
                    "제조업 AI 자동화 투자 확대", "제조기업이 공정 자동화를 위한 AI 도입 예산을 확대했다."),
            spec("demand-finance", CompanyExternalEventCategory.ENTERPRISE_DEMAND, 2,
                    "금융권 생성형 AI 도입 확대", "금융회사의 내부 업무용 생성형 AI 도입이 늘고 있다."),
            spec("demand-public-cut", CompanyExternalEventCategory.ENTERPRISE_DEMAND, 2,
                    "공공기관 AI 예산 축소", "공공기관의 신규 AI 도입 예산이 조정되며 발주가 줄었다."),
            spec("demand-enterprise-tightening", CompanyExternalEventCategory.ENTERPRISE_DEMAND, 2,
                    "대기업 IT 투자 긴축", "대기업들이 신규 IT 투자 심사를 강화하고 계약 규모를 낮추고 있다.")
    );

    private final CompanyExternalEventRepository eventRepository;
    private final CompanyNewsArticleRepository newsRepository;
    private final CompanyCompetitorRepository competitorRepository;
    private final CompanyDepartmentRepository departmentRepository;
    private final CompanyWorkforceService workforceService;

    public CompanyExternalEventService(
            CompanyExternalEventRepository eventRepository,
            CompanyNewsArticleRepository newsRepository,
            CompanyCompetitorRepository competitorRepository,
            CompanyDepartmentRepository departmentRepository,
            CompanyWorkforceService workforceService
    ) {
        this.eventRepository = eventRepository;
        this.newsRepository = newsRepository;
        this.competitorRepository = competitorRepository;
        this.departmentRepository = departmentRepository;
        this.workforceService = workforceService;
    }

    @Transactional
    public void processMonth(PlayerCompany company) {
        int month = company.getMarketMonthsProcessed() + 1;
        if (eventRepository.existsByCompanyAndStartMarketMonth(company, month)) {
            return;
        }
        List<CompanyExternalEvent> active = activeEvents(company, month);
        if (active.size() >= MAXIMUM_ACTIVE_EVENTS || roll(company, month, 11, 100) >= MONTHLY_EVENT_CHANCE) {
            return;
        }
        CompanyExternalEventCategory category = availableCategory(company, month, active);
        if (category == null) {
            return;
        }
        List<EventSpec> candidates = CATALOG.stream()
                .filter(spec -> spec.category() == category)
                .toList();
        EventSpec selected = candidates.get(roll(company, month, 29, candidates.size()));
        int variant = roll(company, month, 47, 3);
        String eventKey = "external:" + month + ":" + selected.key();
        CompanyExternalEvent event = eventRepository.save(new CompanyExternalEvent(
                company,
                eventKey,
                selected.key(),
                selected.category(),
                month,
                month + selected.durationMonths() - 1,
                variant
        ));
        applyImmediateEffect(company, selected);
        createNews(company, event, selected, variant);
    }

    @Transactional(readOnly = true)
    public List<CompanyExternalEvent> activeEvents(PlayerCompany company) {
        return activeEvents(company, Math.max(1, company.getMarketMonthsProcessed()));
    }

    public double marketGrowthMultiplier(PlayerCompany company, int marketMonth) {
        return multiplier(company, marketMonth, "market-adoption", 1.15)
                * multiplier(company, marketMonth, "market-fatigue", 0.85);
    }

    public double planMixSpeedMultiplier(PlayerCompany company, int marketMonth) {
        return multiplier(company, marketMonth, "market-paid-conversion", 1.25)
                * multiplier(company, marketMonth, "market-premium-slowdown", 0.75);
    }

    public double cloudCostMultiplier(PlayerCompany company) {
        return multiplier(company, "infra-cloud-price-up", 1.12)
                * multiplier(company, "infra-cloud-discount", 0.90);
    }

    public double constructionCostMultiplier(PlayerCompany company) {
        return multiplier(company, "infra-accelerator-shortage", 1.15)
                * multiplier(company, "infra-accelerator-supply", 0.90);
    }

    public double constructionWorkMultiplier(PlayerCompany company) {
        return multiplier(company, "infra-accelerator-shortage", 1.10)
                * multiplier(company, "infra-accelerator-supply", 0.90);
    }

    public double productWorkMultiplier(PlayerCompany company) {
        return multiplier(company, "regulation-copyright-uncertain", 1.15)
                * multiplier(company, "regulation-copyright-guide", 0.90);
    }

    public int contractSecurityRequirementBonus(PlayerCompany company) {
        return active(company, "regulation-security-review") ? 10 : 0;
    }

    public double contractCandidateChanceMultiplier(PlayerCompany company) {
        return multiplier(company, "demand-manufacturing", 1.50)
                * multiplier(company, "demand-public-cut", 0.60);
    }

    public double contractFeeMultiplier(PlayerCompany company) {
        return multiplier(company, "demand-finance", 1.10)
                * multiplier(company, "demand-enterprise-tightening", 0.90);
    }

    public boolean guaranteesPublicProject(PlayerCompany company) {
        return active(company, "regulation-public-support");
    }

    public double competitorScoreModifier(PlayerCompany company, String competitorKey, int marketMonth) {
        double modifier = 0;
        if ("popular".equals(competitorKey)
                && active(company, marketMonth, "competitor-marketing")) {
            modifier += 15;
        }
        if ("frontier".equals(competitorKey)
                && active(company, marketMonth, "competitor-outage")) {
            modifier -= 12;
        }
        return modifier;
    }

    private void applyImmediateEffect(PlayerCompany company, EventSpec spec) {
        if (spec.targetCompetitorKey() == null) {
            return;
        }
        competitorRepository.findByCompanyOrderById(company).stream()
                .filter(competitor -> competitor.getCompetitorKey().equals(spec.targetCompetitorKey()))
                .findFirst()
                .ifPresent(competitor -> {
                    if ("competitor-next-model".equals(spec.key())) {
                        competitor.improveBenchmark(0.08);
                    } else if ("competitor-security".equals(spec.key())) {
                        competitor.improveSecurity(10);
                    }
                });
    }

    private void createNews(
            PlayerCompany company,
            CompanyExternalEvent event,
            EventSpec spec,
            int variant
    ) {
        String[] leads = {"시장 조사기관은 ", "업계 관계자들은 ", "기업 고객 현장에서는 "};
        String[] outlooks = {
                " 관련 영향은 당분간 이어질 전망이다.",
                " 기업별 대응에 따라 실적 차이가 벌어질 수 있다.",
                " 현재 사업계획의 비용과 수요 가정을 다시 확인할 필요가 있다."
        };
        newsRepository.save(new CompanyNewsArticle(
                company,
                event.getEventKey(),
                newsCategory(spec.category()),
                titleVariant(spec.title(), variant),
                leads[variant] + spec.body() + outlooks[variant]
                        + " 경영 영향: " + analyzedImpactText(company, spec.key()),
                source(spec.category()),
                event.getStartMarketMonth()
        ));
    }

    String analyzedImpactText(PlayerCompany company, String key) {
        if (departmentRepository.findByCompanyAndDepartmentType(
                company, CompanyDepartmentType.STRATEGY_FINANCE).isEmpty()) {
            return impactDirection(key);
        }
        int expertise = workforceService.departmentExpertise(
                company, CompanyDepartmentType.STRATEGY_FINANCE);
        var load = workforceService.departmentLoad(company, CompanyDepartmentType.STRATEGY_FINANCE);
        return expertise >= 60 && load.capacity() > 0 && load.utilizationPercent() <= 110
                ? impactText(key)
                : approximateImpactText(key);
    }

    private String impactDirection(String key) {
        return positiveImpact(key) ? "긍정적 영향 예상." : "부정적 영향 예상.";
    }

    private String approximateImpactText(String key) {
        return positiveImpact(key)
                ? "중간 수준의 긍정적 영향 예상."
                : "중간 수준의 부정적 영향 예상.";
    }

    private boolean positiveImpact(String key) {
        return switch (key) {
            case "market-adoption", "market-paid-conversion", "competitor-outage",
                    "infra-cloud-discount", "infra-accelerator-supply",
                    "regulation-copyright-guide", "regulation-public-support",
                    "demand-manufacturing", "demand-finance" -> true;
            default -> false;
        };
    }

    private String titleVariant(String title, int variant) {
        return switch (variant) {
            case 1 -> title + "…업계 대응 주목";
            case 2 -> title + "에 사업환경 변화";
            default -> title;
        };
    }

    private String impactText(String key) {
        return switch (key) {
            case "market-adoption" -> "AI 유료시장 성장률 15% 증가.";
            case "market-paid-conversion" -> "프로·맥스 요금제 전환속도 25% 증가.";
            case "market-fatigue" -> "AI 유료시장 성장률 15% 감소.";
            case "market-premium-slowdown" -> "프로·맥스 요금제 전환속도 25% 감소.";
            case "competitor-next-model" -> "프론티어AI 벤치마크 8% 상승.";
            case "competitor-marketing" -> "모두AI 경쟁점수에 마케팅 15점 반영.";
            case "competitor-security" -> "트러스트랩 보안점수 10점 상승.";
            case "competitor-outage" -> "프론티어AI 경쟁점수 12점 감소.";
            case "infra-cloud-price-up" -> "외부 클라우드 이용료 12% 증가.";
            case "infra-cloud-discount" -> "외부 클라우드 이용료 10% 감소.";
            case "infra-accelerator-shortage" -> "신규 연산망 비용 15%, 공사기간 10% 증가.";
            case "infra-accelerator-supply" -> "신규 연산망 비용과 공사기간 10% 감소.";
            case "regulation-security-review" -> "신규 기업계약 보안 요구치 10점 증가.";
            case "regulation-copyright-uncertain" -> "신규 제품 프로젝트 작업량 15% 증가.";
            case "regulation-copyright-guide" -> "신규 제품 프로젝트 작업량 10% 감소.";
            case "regulation-public-support" -> "공공 AI 실증사업 후보 1건 보장.";
            case "demand-manufacturing" -> "기업계약 후보 발생률 50% 증가.";
            case "demand-finance" -> "신규 기업계약 월 이용료 10% 증가.";
            case "demand-public-cut" -> "기업계약 후보 발생률 40% 감소.";
            case "demand-enterprise-tightening" -> "신규 기업계약 월 이용료 10% 감소.";
            default -> "관련 경영지표에 반영.";
        };
    }

    private CompanyNewsCategory newsCategory(CompanyExternalEventCategory category) {
        return switch (category) {
            case AI_MARKET -> CompanyNewsCategory.AI_MARKET;
            case COMPETITOR -> CompanyNewsCategory.COMPETITOR;
            case INFRASTRUCTURE -> CompanyNewsCategory.INFRASTRUCTURE;
            case REGULATION -> CompanyNewsCategory.REGULATION;
            case ENTERPRISE_DEMAND -> CompanyNewsCategory.ENTERPRISE_DEMAND;
        };
    }

    private String source(CompanyExternalEventCategory category) {
        return switch (category) {
            case AI_MARKET -> "AI산업동향";
            case COMPETITOR -> "테크비즈";
            case INFRASTRUCTURE -> "인프라리포트";
            case REGULATION -> "정책브리핑";
            case ENTERPRISE_DEMAND -> "기업수요조사";
        };
    }

    private CompanyExternalEventCategory availableCategory(
            PlayerCompany company,
            int month,
            List<CompanyExternalEvent> active
    ) {
        CompanyExternalEventCategory[] weighted = {
                CompanyExternalEventCategory.AI_MARKET,
                CompanyExternalEventCategory.AI_MARKET,
                CompanyExternalEventCategory.AI_MARKET,
                CompanyExternalEventCategory.AI_MARKET,
                CompanyExternalEventCategory.AI_MARKET,
                CompanyExternalEventCategory.COMPETITOR,
                CompanyExternalEventCategory.COMPETITOR,
                CompanyExternalEventCategory.COMPETITOR,
                CompanyExternalEventCategory.COMPETITOR,
                CompanyExternalEventCategory.COMPETITOR,
                CompanyExternalEventCategory.INFRASTRUCTURE,
                CompanyExternalEventCategory.INFRASTRUCTURE,
                CompanyExternalEventCategory.INFRASTRUCTURE,
                CompanyExternalEventCategory.INFRASTRUCTURE,
                CompanyExternalEventCategory.REGULATION,
                CompanyExternalEventCategory.REGULATION,
                CompanyExternalEventCategory.REGULATION,
                CompanyExternalEventCategory.ENTERPRISE_DEMAND,
                CompanyExternalEventCategory.ENTERPRISE_DEMAND,
                CompanyExternalEventCategory.ENTERPRISE_DEMAND
        };
        int start = roll(company, month, 19, weighted.length);
        for (int offset = 0; offset < weighted.length; offset++) {
            CompanyExternalEventCategory candidate = weighted[(start + offset) % weighted.length];
            if (active.stream().noneMatch(event -> event.getCategory() == candidate)) {
                return candidate;
            }
        }
        return null;
    }

    private List<CompanyExternalEvent> activeEvents(PlayerCompany company, int month) {
        return eventRepository.findByCompanyOrderByStartMarketMonthDescIdDesc(company).stream()
                .filter(event -> event.isActiveAt(month))
                .toList();
    }

    private boolean active(PlayerCompany company, String catalogKey) {
        return activeEvents(company).stream()
                .anyMatch(event -> event.getCatalogKey().equals(catalogKey));
    }

    private boolean active(PlayerCompany company, int marketMonth, String catalogKey) {
        return activeEvents(company, marketMonth).stream()
                .anyMatch(event -> event.getCatalogKey().equals(catalogKey));
    }

    private double multiplier(PlayerCompany company, String catalogKey, double value) {
        return active(company, catalogKey) ? value : 1.0;
    }

    private double multiplier(
            PlayerCompany company,
            int marketMonth,
            String catalogKey,
            double value
    ) {
        return active(company, marketMonth, catalogKey) ? value : 1.0;
    }

    private int roll(PlayerCompany company, int month, int salt, int bound) {
        long value = company.getId() * 1_000_003L + month * 65_537L + salt * 31_337L;
        value ^= value >>> 29;
        value *= 0x9E3779B97F4A7C15L;
        return Math.floorMod(value, bound);
    }

    private static EventSpec spec(
            String key,
            CompanyExternalEventCategory category,
            int durationMonths,
            String title,
            String body
    ) {
        return new EventSpec(key, category, durationMonths, title, body, null);
    }

    private static EventSpec targeted(
            String key,
            CompanyExternalEventCategory category,
            int durationMonths,
            String targetCompetitorKey,
            String title,
            String body
    ) {
        return new EventSpec(key, category, durationMonths, title, body, targetCompetitorKey);
    }

    private record EventSpec(
            String key,
            CompanyExternalEventCategory category,
            int durationMonths,
            String title,
            String body,
            String targetCompetitorKey
    ) {
    }
}
