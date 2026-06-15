package com.game.buildingstory.web;

import org.springframework.stereotype.Component;

@Component("moneyText")
public class MoneyText {
    public String format(long amount) {
        if (amount == 0) {
            return "0원";
        }

        long jo = amount / 1_000_000_000_000L;
        amount %= 1_000_000_000_000L;
        long eok = amount / 100_000_000L;
        amount %= 100_000_000L;
        long man = amount / 10_000L;

        StringBuilder builder = new StringBuilder();
        if (jo > 0) {
            builder.append(jo).append("조");
        }
        if (eok > 0) {
            builder.append(eok).append("억");
        }
        if (man > 0 && jo == 0) {
            builder.append(man).append("만");
        }
        if (builder.isEmpty()) {
            builder.append(amount);
        }
        return builder.append("원").toString();
    }
}
