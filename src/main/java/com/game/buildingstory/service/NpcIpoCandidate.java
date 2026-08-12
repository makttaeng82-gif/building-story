package com.game.buildingstory.service;

/** 고정 신규상장 후보의 일정과 공모 할인 기준이다. */
public record NpcIpoCandidate(String stockKey, int targetMonthAfterUnlock, int offerDiscountBasisPoints) {
}
