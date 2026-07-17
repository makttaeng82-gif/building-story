/**
 * 게임의 영속 상태를 표현하는 JPA 엔티티 패키지다.
 *
 * <p>이 패키지의 클래스들은 데이터베이스 테이블과 직접 연결된다. 예를 들어
 * {@link com.game.buildingstory.domain.Player}는 플레이어의 날짜, 현금, 평판,
 * 이벤트 예약 상태를 저장하고, {@link com.game.buildingstory.domain.OwnedBuilding}은
 * 플레이어가 보유한 건물의 입주, 수리, 매각 대기 상태를 저장한다.</p>
 *
 * <p>원리: 컨트롤러가 요청을 받으면 서비스 계층이 Repository로 엔티티를 읽고,
 * 엔티티의 메서드로 상태를 변경한다. Spring의 트랜잭션 안에서 변경된 엔티티는
 * JPA dirty checking에 의해 자동으로 UPDATE 된다. 그래서 엔티티 메서드는
 * "무슨 값을 어떻게 바꾸는지"를 가장 작은 단위로 표현해야 한다.</p>
 *
 * <p>주의: 엔티티는 비즈니스 규칙의 마지막 방어선이다. 예를 들어 수량이 부족하면
 * 판매가 실패하고, 월이 넘어가면 날짜가 1일로 돌아가는 식의 불변조건은 이 계층에
 * 남겨두면 서비스가 커져도 규칙이 흩어지지 않는다.</p>
 */
package com.game.buildingstory.domain;
