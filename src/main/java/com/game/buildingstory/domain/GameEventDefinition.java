package com.game.buildingstory.domain;

public record GameEventDefinition(
        String key,
        int month,
        int day,
        String title,
        String body,
        String imageLabel,
        String effectKey
) {
}
