package com.game.buildingstory.web;

public final class SessionKeys {
    /*
     * 세션 attribute 이름을 한 곳에 모아둔다.
     *
     * 문자열을 컨트롤러마다 직접 쓰면 오타가 나도 컴파일러가 잡지 못한다.
     * 상수로 관리하면 이름 변경과 검색이 쉬워진다.
     */
    public static final String PLAYER_ID = "PLAYER_ID";

    private SessionKeys() {
    }
}
