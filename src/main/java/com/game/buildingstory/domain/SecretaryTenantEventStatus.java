package com.game.buildingstory.domain;

/**
 * 비서 임차인 이벤트의 진행 단계다.
 *
 * <p>비서가 임차인으로 등장하고, 요청을 수락하면 고용 가능 상태가 되며, 고용 후 완료된다.</p>
 */
public enum SecretaryTenantEventStatus {
    TENANT,
    REQUEST_AVAILABLE,
    REQUEST_ACCEPTED,
    HIRE_AVAILABLE,
    COMPLETED
}
