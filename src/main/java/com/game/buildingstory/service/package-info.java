/**
 * 게임 규칙과 하루 진행 로직을 처리하는 서비스 패키지다.
 *
 * <p>서비스는 컨트롤러와 엔티티 사이의 조정자다. 컨트롤러는 HTTP 요청을 받아
 * 어떤 행동이 필요한지만 서비스에 전달하고, 서비스는 Repository로 필요한 상태를
 * 읽은 뒤 엔티티 메서드를 호출해 게임 규칙을 적용한다.</p>
 *
 * <p>대표 흐름은 {@link com.game.buildingstory.service.GameService#tick(long, boolean)}이다.
 * 하루가 지나면 날짜 증가, 월세 정산, 매물 갱신, 주식 가격 갱신, 이벤트 활성화가
 * 순서대로 실행된다. 이 순서가 게임 결과를 결정하므로, 새 기능을 넣을 때는
 * "하루 시작 전인지, 날짜 증가 후인지, 이벤트 표시 전인지"를 먼저 정해야 한다.</p>
 *
 * <p>원리: 큰 기능은 전용 서비스로 분리한다. 부동산 거래는
 * {@link com.game.buildingstory.service.BuildingTradeService}, 주식은
 * {@link com.game.buildingstory.service.StockService}, 비서 운영은
 * {@link com.game.buildingstory.service.SecretaryOperationsService}가 맡는다.
 * 이렇게 하면 GameService는 전체 순서를 조립하고, 세부 규칙은 각 서비스가 관리한다.</p>
 */
package com.game.buildingstory.service;
