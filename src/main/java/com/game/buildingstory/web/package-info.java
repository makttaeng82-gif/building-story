/**
 * HTTP 요청, 세션, 화면 모델을 다루는 웹 계층 패키지다.
 *
 * <p>컨트롤러는 브라우저 요청을 받는 입구다. 세션에서 현재 플레이어 ID를 찾고,
 * 서비스 메서드를 호출한 뒤, HTML 템플릿 이름이나 JSON 응답을 돌려준다.</p>
 *
 * <p>원리: 화면에 필요한 데이터는 Model에 담긴다. 예를 들어 메인 화면은
 * {@link com.game.buildingstory.web.MainPageModelAssembler}가 건물, 주식,
 * 비서, 이벤트 상태를 한 번에 모아 템플릿으로 전달한다. 이렇게 하면 컨트롤러가
 * 화면 데이터 조립 코드로 비대해지는 것을 막을 수 있다.</p>
 */
package com.game.buildingstory.web;
