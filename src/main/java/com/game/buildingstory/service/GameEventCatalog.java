package com.game.buildingstory.service;

import com.game.buildingstory.domain.GameEventDefinition;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class GameEventCatalog {
    public static final String EFFECT_FIRST_TENANT_MOVE_IN = "FIRST_TENANT_MOVE_IN";

    private final List<GameEventDefinition> definitions = List.of(
            new GameEventDefinition(
                    "first_tenant_intro",
                    1,
                    3,
                    "오래된 후배의 연락",
                    "회사 후배였던 사람이 찾아왔다. 친구가 원룸을 구한다며 소개시켜주러 왔다고 한다. 훗날 비서로도 함께할 수 있는 인연의 시작이다.",
                    "AI 이벤트 이미지",
                    EFFECT_FIRST_TENANT_MOVE_IN
            )
    );

    public Optional<GameEventDefinition> findDueEvent(int month, int day) {
        return definitions.stream()
                .filter(definition -> definition.month() == month && definition.day() == day)
                .findFirst();
    }
}
