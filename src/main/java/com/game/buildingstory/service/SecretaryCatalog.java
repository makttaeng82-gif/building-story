package com.game.buildingstory.service;

import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class SecretaryCatalog {
    private final List<SecretarySpec> specs = List.of(
            new SecretarySpec("설아름", "회사 후배", "/assets/secretaries/secretary-1.png", 1000, 3_000_000L, 1, "수리요청 자동확인", "숙련도 10마다 월세보너스 3% 증가, 수리비 5% 감소", "1월 3일 이벤트 후 입주 상태에서 영입 가능"),
            new SecretarySpec("설하은", "회사 후배 쌍둥이", "/assets/secretaries/secretary-2.png", 5000, 5_000_000L, 5, "수리요청 자동확인", "숙련도 5마다 월세보너스 2% 증가", "추후 추가예정"),
            new SecretarySpec("이다은", "공인중개사", "/assets/secretaries/secretary-3.png", 30000, 11_000_000L, 10, "수리요청 자동확인", "추후 추가예정", "추후 추가예정"),
            new SecretarySpec("권아리", "세무사", "/assets/secretaries/secretary-4.png", 100000, 18_000_000L, 15, "수리요청 자동확인", "추후 추가예정", "추후 추가예정"),
            new SecretarySpec("김채린", "재무설계사", "/assets/secretaries/secretary-5.png", 1_000_000, 31_000_000L, 20, "수리요청 자동확인", "추후 추가예정", "추후 추가예정"),
            new SecretarySpec("신수아", "변호사", "/assets/secretaries/secretary-6.png", 60_000_000, 70_000_000L, 25, "수리요청 자동확인", "추후 추가예정", "추후 추가예정")
    );

    public List<SecretarySpec> all() {
        return specs;
    }
}
