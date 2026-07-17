package com.game.buildingstory.service.time;

import com.game.buildingstory.domain.Player;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DailyGameOrchestratorTests {

    @Test
    void processorsRunByOrderAndCombineNotices() {
        List<Integer> executedOrders = new ArrayList<>();
        DailyGameOrchestrator orchestrator = new DailyGameOrchestrator(List.of(
                processor(300, executedOrders, DailyProcessResult.continueWith("주식 갱신")),
                processor(100, executedOrders, DailyProcessResult.continueWith("월세 정산")),
                processor(200, executedOrders, DailyProcessResult.continueWithoutNotice())
        ));

        String result = orchestrator.process(new Player("tester", "password"), false);

        assertThat(executedOrders).containsExactly(100, 200, 300);
        assertThat(result).isEqualTo("월세 정산 · 주식 갱신");
    }

    @Test
    void terminalSignalStopsLaterProcessorsAndOverridesNotice() {
        List<Integer> executedOrders = new ArrayList<>();
        DailyGameOrchestrator orchestrator = new DailyGameOrchestrator(List.of(
                processor(100, executedOrders, DailyProcessResult.continueWith("정산 알림")),
                processor(200, executedOrders, DailyProcessResult.stop("EVENT:15")),
                processor(300, executedOrders, DailyProcessResult.continueWith("실행되면 안 됨"))
        ));

        String result = orchestrator.process(new Player("tester", "password"), false);

        assertThat(executedOrders).containsExactly(100, 200);
        assertThat(result).isEqualTo("EVENT:15");
    }

    private DailyGameProcessor processor(
            int order,
            List<Integer> executedOrders,
            DailyProcessResult result
    ) {
        return new DailyGameProcessor() {
            @Override
            public int order() {
                return order;
            }

            @Override
            public DailyProcessResult process(DailyProcessContext context) {
                executedOrders.add(order);
                return result;
            }
        };
    }
}
