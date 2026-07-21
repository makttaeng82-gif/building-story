package com.game.buildingstory.service;

import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class ListedCompanyValuationCatalog {
    private final Map<String, ListedCompanyValuationRule> rules = Map.of(
            "IT", new ListedCompanyValuationRule(1_800, 220, 180, 6_000, 2_000, 2_000),
            "식품", new ListedCompanyValuationRule(1_400, 160, 70, 6_000, 3_000, 1_000),
            "유통", new ListedCompanyValuationRule(1_600, 180, 60, 5_500, 2_500, 2_000),
            "제조", new ListedCompanyValuationRule(1_400, 150, 70, 5_500, 4_000, 500),
            "통신", new ListedCompanyValuationRule(1_300, 140, 80, 5_000, 4_500, 500)
    );

    public ListedCompanyValuationRule require(String industry) {
        ListedCompanyValuationRule rule = rules.get(industry);
        if (rule == null) {
            throw new IllegalArgumentException("가치평가 규칙이 없는 업종입니다: " + industry);
        }
        return rule;
    }
}
