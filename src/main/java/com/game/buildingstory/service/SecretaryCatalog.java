package com.game.buildingstory.service;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class SecretaryCatalog {
    private static final String COMMON_EFFECT = "수리요청 자동확인";
    private static final String PROFICIENCY_BONUS = "자동확인 주기 30일 -> 7일";
    private static final String PROFICIENCY_DETAIL = "숙련도 1 기준 30일, 숙련도 30 기준 7일. 공식: 30 - floor((숙련도 - 1) * 23 / 29)";

    private final List<SecretarySpec> specs = List.of(
            new SecretarySpec("secretary-1", "설아름", "회사 후배", "/assets/secretaries/secretary-1.png", 1000, 2_000_000L, 1, COMMON_EFFECT, PROFICIENCY_BONUS, PROFICIENCY_DETAIL, "수리요청 확률 감소", "0.5%", "호감도 1 기준 수리요청 확률 0.5% 감소. 추후 호감도 상승 시 강화 예정.", "1월 3일 이벤트 후 입주 상태에서 영입 가능"),
            new SecretarySpec("secretary-2", "설하은", "연구원", "/assets/secretaries/secretary-2.png", 5000, 2_000_000L, 5, COMMON_EFFECT, PROFICIENCY_BONUS, PROFICIENCY_DETAIL, "퇴거확률 감소", "0.2%", "호감도 1 기준 퇴거확률 0.2% 감소. 추후 호감도 상승 시 강화 예정.", "추후 추가 예정"),
            new SecretarySpec("secretary-3", "이다은", "공인중개사", "/assets/secretaries/secretary-3.png", 30000, 2_000_000L, 10, COMMON_EFFECT, PROFICIENCY_BONUS, PROFICIENCY_DETAIL, "입주확률 증가", "0.5%", "호감도 1 기준 입주확률 0.5% 증가. 추후 호감도 상승 시 강화 예정.", "추후 추가 예정"),
            new SecretarySpec("secretary-4", "한아리", "세무사", "/assets/secretaries/secretary-4.png", 100000, 2_000_000L, 15, COMMON_EFFECT, PROFICIENCY_BONUS, PROFICIENCY_DETAIL, "월세 증가", "0.3%", "호감도 1 기준 월세 0.3% 증가. 추후 호감도 상승 시 강화 예정.", "추후 추가 예정"),
            new SecretarySpec("secretary-5", "김채린", "재무설계사", "/assets/secretaries/secretary-5.png", 1_000_000, 2_000_000L, 20, COMMON_EFFECT, PROFICIENCY_BONUS, PROFICIENCY_DETAIL, "월세 증가 · 건물대기시간 감소", "0.25% · 0.5%", "호감도 1 기준 월세 0.25% 증가, 건물대기시간 0.5% 감소. 추후 호감도 상승 시 강화 예정.", "추후 추가 예정"),
            new SecretarySpec("secretary-6", "신수아", "변호사", "/assets/secretaries/secretary-6-main.png", 60_000_000, 2_000_000L, 25, COMMON_EFFECT, PROFICIENCY_BONUS, PROFICIENCY_DETAIL, "건물대기시간 감소", "1%", "호감도 1 기준 건물대기시간 1% 감소. 추후 호감도 상승 시 강화 예정.", "추후 추가 예정")
    );

    public List<SecretarySpec> all() {
        return specs;
    }

    public Optional<SecretarySpec> find(String key) {
        return specs.stream()
                .filter(spec -> spec.key().equals(key))
                .findFirst();
    }
}
