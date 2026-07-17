package com.game.buildingstory.service;

/**
 * 서버 렌더링 SVG 캔들 하나의 좌표 정보다.
 *
 * <p>가격 이력 엔티티의 open/high/low/close 값을 차트 픽셀 좌표로 변환한 결과이며,
 * 템플릿은 이 값을 line/rect 속성에 그대로 넣어 캔들 차트를 그린다.</p>
 */
public record StockCandleView(
        int x,
        int openY,
        int highY,
        int lowY,
        int closeY,
        int bodyY,
        int bodyHeight,
        boolean rising,
        String dateText
) {
}
