package com.game.buildingstory.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class StockLiquidityStateTests {
    @Test
    void capacityRuleChangeKeepsOrdersAlreadyMadeInCurrentPeriod() {
        Player player = mock(Player.class);
        when(player.getElapsedDays()).thenReturn(10);
        StockLiquidityState state = new StockLiquidityState(player, "stock", 1_500, 100_000);
        state.applyNetBuyChange(1_000);

        state.updateCapacity(900);

        assertThat(state.getCapacity()).isEqualTo(1_000);
        assertThat(state.getNetPlayerBuyQuantity()).isEqualTo(1_000);
        assertThat(state.getReferencePrice()).isEqualTo(100_000);
        assertThat(state.getRefreshedElapsedDays()).isEqualTo(10);
    }
}
