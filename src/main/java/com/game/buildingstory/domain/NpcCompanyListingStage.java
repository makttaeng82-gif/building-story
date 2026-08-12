package com.game.buildingstory.domain;

/** NPC 신규상장이 현재 어느 공개 단계까지 진행됐는지 나타낸다. */
public enum NpcCompanyListingStage {
    SCHEDULED,
    ANNOUNCED,
    INFORMATION_PUBLISHED,
    REVIEWED,
    OFFER_CONFIRMED,
    LISTED
}
