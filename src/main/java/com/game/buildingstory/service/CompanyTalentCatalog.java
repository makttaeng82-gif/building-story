package com.game.buildingstory.service;

import com.game.buildingstory.domain.CompanyDepartmentType;
import com.game.buildingstory.domain.CompanyGrowthStage;
import com.game.buildingstory.domain.CompanyTalentCandidate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/** 창업 이력서와 회사 성장단계에 따라 개방되는 후기 핵심인재 후보를 관리한다. */
@Component
public class CompanyTalentCatalog {
    private static final Set<String> FOUNDING_CANDIDATE_KEYS = Set.of(
            "dev-01", "dev-02", "dev-03", "dev-04", "dev-05",
            "dev-06", "dev-07", "dev-08", "dev-09", "dev-10",
            "sales-01", "sales-02", "sales-03", "sales-04", "sales-05",
            "ops-01", "ops-02", "ops-03", "ops-04", "ops-05"
    );
    private static final Map<String, CompanyGrowthStage> LATE_CANDIDATE_STAGES = Map.ofEntries(
            Map.entry("dev-11", CompanyGrowthStage.GROWTH),
            Map.entry("dev-12", CompanyGrowthStage.LARGE),
            Map.entry("sales-06", CompanyGrowthStage.GROWTH),
            Map.entry("sales-07", CompanyGrowthStage.LARGE),
            Map.entry("ops-06", CompanyGrowthStage.GROWTH),
            Map.entry("ops-07", CompanyGrowthStage.LARGE),
            Map.entry("hr-04", CompanyGrowthStage.GROWTH),
            Map.entry("hr-05", CompanyGrowthStage.LARGE),
            Map.entry("finance-04", CompanyGrowthStage.GROWTH),
            Map.entry("finance-05", CompanyGrowthStage.LARGE)
    );

    private final List<CompanyTalentCandidate> candidates = List.of(
            candidate("dev-01", "김도윤", CompanyDepartmentType.AI_DEVELOPMENT, "모델 설계", 2, 72, 78, 65, 360_000_000L),
            candidate("dev-02", "이서준", CompanyDepartmentType.AI_DEVELOPMENT, "추론 최적화", 2, 69, 82, 61, 340_000_000L),
            candidate("dev-03", "박지호", CompanyDepartmentType.AI_DEVELOPMENT, "데이터 파이프라인", 1, 61, 86, 74, 230_000_000L),
            candidate("dev-04", "최하린", CompanyDepartmentType.AI_DEVELOPMENT, "모델 평가", 1, 59, 91, 68, 220_000_000L),
            candidate("dev-05", "정시우", CompanyDepartmentType.AI_DEVELOPMENT, "백엔드", 1, 64, 72, 80, 240_000_000L),
            candidate("dev-06", "한유진", CompanyDepartmentType.AI_DEVELOPMENT, "보안", 1, 58, 88, 77, 215_000_000L),
            candidate("dev-07", "윤재현", CompanyDepartmentType.AI_DEVELOPMENT, "학습 인프라", 1, 63, 70, 66, 235_000_000L),
            candidate("dev-08", "송나은", CompanyDepartmentType.AI_DEVELOPMENT, "제품 품질", 1, 56, 93, 82, 210_000_000L),
            candidate("dev-09", "임건우", CompanyDepartmentType.AI_DEVELOPMENT, "API 플랫폼", 1, 60, 79, 73, 225_000_000L),
            candidate("dev-10", "오세린", CompanyDepartmentType.AI_DEVELOPMENT, "데이터 품질", 1, 57, 90, 85, 215_000_000L),
            candidate("sales-01", "강민준", CompanyDepartmentType.SALES_MARKETING, "기업 영업", 2, 71, 75, 72, 350_000_000L),
            candidate("sales-02", "문서아", CompanyDepartmentType.SALES_MARKETING, "브랜드 전략", 1, 62, 84, 79, 235_000_000L),
            candidate("sales-03", "배현우", CompanyDepartmentType.SALES_MARKETING, "파트너십", 1, 59, 89, 70, 220_000_000L),
            candidate("sales-04", "신예원", CompanyDepartmentType.SALES_MARKETING, "그로스", 1, 61, 81, 76, 230_000_000L),
            candidate("sales-05", "조태윤", CompanyDepartmentType.SALES_MARKETING, "시장 분석", 1, 55, 94, 83, 205_000_000L),
            candidate("ops-01", "권수빈", CompanyDepartmentType.SERVICE_OPERATIONS, "서비스 운영", 2, 70, 76, 88, 345_000_000L),
            candidate("ops-02", "백승민", CompanyDepartmentType.SERVICE_OPERATIONS, "클라우드", 1, 63, 80, 75, 235_000_000L),
            candidate("ops-03", "남지안", CompanyDepartmentType.SERVICE_OPERATIONS, "고객 지원", 1, 57, 92, 90, 215_000_000L),
            candidate("ops-04", "서준영", CompanyDepartmentType.SERVICE_OPERATIONS, "장애 대응", 1, 62, 74, 81, 230_000_000L),
            candidate("ops-05", "류가은", CompanyDepartmentType.SERVICE_OPERATIONS, "운영 자동화", 1, 58, 87, 84, 220_000_000L),
            candidate("hr-01", "김지윤", CompanyDepartmentType.HR_ORGANIZATION, "조직 설계", 2, 70, 81, 86, 340_000_000L),
            candidate("hr-02", "박서현", CompanyDepartmentType.HR_ORGANIZATION, "인재 개발", 1, 62, 90, 88, 230_000_000L),
            candidate("hr-03", "윤하진", CompanyDepartmentType.HR_ORGANIZATION, "채용 운영", 1, 58, 85, 82, 215_000_000L),
            candidate("finance-01", "이준혁", CompanyDepartmentType.STRATEGY_FINANCE, "재무 전략", 2, 72, 78, 80, 355_000_000L),
            candidate("finance-02", "정유나", CompanyDepartmentType.STRATEGY_FINANCE, "사업 분석", 1, 64, 88, 84, 240_000_000L),
            candidate("finance-03", "최민석", CompanyDepartmentType.STRATEGY_FINANCE, "계약·법무", 1, 60, 83, 79, 225_000_000L),
            candidate("dev-11", "서이안", CompanyDepartmentType.AI_DEVELOPMENT, "모델 아키텍처", 3, 84, 91, 82, 650_000_000L),
            candidate("dev-12", "장태오", CompanyDepartmentType.AI_DEVELOPMENT, "대규모 학습", 4, 92, 86, 78, 1_100_000_000L),
            candidate("sales-06", "한채원", CompanyDepartmentType.SALES_MARKETING, "글로벌 성장", 3, 83, 89, 86, 620_000_000L),
            candidate("sales-07", "유현석", CompanyDepartmentType.SALES_MARKETING, "전략 제휴", 4, 91, 84, 80, 1_050_000_000L),
            candidate("ops-06", "차은솔", CompanyDepartmentType.SERVICE_OPERATIONS, "대규모 서비스", 3, 84, 90, 88, 630_000_000L),
            candidate("ops-07", "백도현", CompanyDepartmentType.SERVICE_OPERATIONS, "글로벌 인프라", 4, 92, 85, 82, 1_080_000_000L),
            candidate("hr-04", "송지아", CompanyDepartmentType.HR_ORGANIZATION, "조직 확장", 3, 86, 92, 88, 650_000_000L),
            candidate("hr-05", "강현우", CompanyDepartmentType.HR_ORGANIZATION, "글로벌 인사", 4, 92, 87, 90, 1_100_000_000L),
            candidate("finance-04", "오지민", CompanyDepartmentType.STRATEGY_FINANCE, "성장 재무", 3, 87, 90, 86, 670_000_000L),
            candidate("finance-05", "문태경", CompanyDepartmentType.STRATEGY_FINANCE, "글로벌 자본전략", 4, 93, 85, 88, 1_150_000_000L)
    );

    public List<CompanyTalentCandidate> all() {
        return candidates;
    }

    public Optional<CompanyTalentCandidate> find(String key) {
        return candidates.stream().filter(candidate -> candidate.key().equals(key)).findFirst();
    }

    public List<CompanyTalentCandidate> foundingCandidates() {
        return candidates.stream()
                .filter(candidate -> FOUNDING_CANDIDATE_KEYS.contains(candidate.key()))
                .toList();
    }

    public boolean isFoundingCandidate(String key) {
        return FOUNDING_CANDIDATE_KEYS.contains(key);
    }

    public boolean isAvailableAt(String key, CompanyGrowthStage stage) {
        CompanyGrowthStage required = LATE_CANDIDATE_STAGES.getOrDefault(key, CompanyGrowthStage.FOUNDED);
        return stage.ordinal() >= required.ordinal();
    }

    public CompanyGrowthStage requiredStage(String key) {
        return LATE_CANDIDATE_STAGES.getOrDefault(key, CompanyGrowthStage.FOUNDED);
    }

    private CompanyTalentCandidate candidate(
            String key,
            String name,
            CompanyDepartmentType department,
            String specialty,
            int grade,
            int ability,
            int potential,
            int fit,
            long annualSalary
    ) {
        int leadership = Math.max(30, Math.min(80, (ability + fit) / 2));
        return new CompanyTalentCandidate(key, name, department, specialty, grade, ability, potential, fit, leadership,
                annualSalary, annualSalary * 30 / 100);
    }
}
