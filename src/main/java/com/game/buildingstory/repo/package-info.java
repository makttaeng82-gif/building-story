/**
 * Spring Data JPA Repository 패키지다.
 *
 * <p>Repository는 데이터베이스 접근을 담당한다. 이 프로젝트는 복잡한 SQL을 직접 쓰기보다
 * 메서드 이름 규칙을 사용한다. 예를 들어 {@code findByPlayerAndCityOrderById}처럼 쓰면
 * Spring Data JPA가 메서드 이름을 분석해 SELECT 쿼리를 만든다.</p>
 *
 * <p>원리: 서비스는 Repository를 통해 엔티티를 가져오고, 가져온 엔티티의 메서드로
 * 상태를 변경한다. Repository 안에 게임 규칙을 넣지 않는 이유는 DB 조회 책임과
 * 비즈니스 규칙 책임을 분리하기 위해서다.</p>
 */
package com.game.buildingstory.repo;
