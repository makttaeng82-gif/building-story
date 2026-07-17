package com.game.buildingstory.service.time;

import com.game.buildingstory.domain.Player;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

/**
 * 등록된 하루 처리 단계를 정해진 순서대로 실행한다.
 *
 * <p>새 콘텐츠가 같은 게임 시간을 사용한다면 {@link DailyGameProcessor} 구현을 추가하면 된다.
 * 날짜 자체는 이 클래스가 변경하지 않으므로 중복 날짜 증가가 발생하지 않는다.</p>
 */
@Service
public class DailyGameOrchestrator {
    private final List<DailyGameProcessor> processors;

    public DailyGameOrchestrator(List<DailyGameProcessor> processors) {
        // Spring이 수집한 구현체 목록을 복사한 뒤 실행 순서를 고정한다.
        this.processors = processors.stream()
                .sorted(Comparator.comparingInt(DailyGameProcessor::order))
                .toList();
    }

    public String process(Player player, boolean deferCityEvents) {
        DailyProcessContext context = new DailyProcessContext(player, deferCityEvents);
        String notice = "";

        for (DailyGameProcessor processor : processors) {
            DailyProcessResult result = processor.process(context);
            if (result.shouldStop()) {
                // 기존 tick 규칙과 같이 화면 전환 신호가 일반 정산 알림보다 우선한다.
                return result.terminalSignal();
            }
            notice = appendNotice(notice, result.notice());
        }
        return notice;
    }

    private String appendNotice(String base, String addition) {
        if (addition == null || addition.isBlank()) {
            return base;
        }
        if (base == null || base.isBlank()) {
            return addition;
        }
        return base + " \u00b7 " + addition;
    }
}
