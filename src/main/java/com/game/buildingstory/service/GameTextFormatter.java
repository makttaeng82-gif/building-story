package com.game.buildingstory.service;

import java.util.Locale;

final class GameTextFormatter {
    /*
     * 게임 화면에서 반복되는 표시 문자열을 만드는 내부 유틸리티다.
     *
     * 금액, 평판, 퍼센트처럼 여러 서비스에서 같은 형식으로 보여줘야 하는 텍스트를 이곳에서 만든다.
     */
    private GameTextFormatter() {
    }

    static String reputationText(long amount) {
        if (amount == 0) {
            return "0";
        }
        String sign = amount < 0 ? "-" : "";
        long value = Math.abs(amount);
        if (value < 10_000L) {
            long thousand = value / 1_000L;
            long remainder = value % 1_000L;
            if (thousand > 0) {
                return sign + thousand + "천" + (remainder > 0 ? remainder : "");
            }
            return sign + value;
        }

        long jo = value / 1_000_000_000_000L;
        value %= 1_000_000_000_000L;
        long eok = value / 100_000_000L;
        value %= 100_000_000L;
        long man = value / 10_000L;
        long remainder = value % 10_000L;

        StringBuilder builder = new StringBuilder(sign);
        if (jo > 0) {
            builder.append(jo).append("조");
        }
        if (eok > 0) {
            builder.append(eok).append("억");
        }
        if (man > 0) {
            builder.append(man).append("만");
        }
        if (remainder > 0 && jo == 0 && eok == 0) {
            builder.append(remainder);
        }
        return builder.toString();
    }

    static String percent(double value) {
        if (Math.abs(value - Math.rint(value)) < 0.0001) {
            return String.format(Locale.ROOT, "%.0f%%", value);
        }
        return String.format(Locale.ROOT, "%.2f", value).replaceAll("0+$", "").replaceAll("\\.$", "") + "%";
    }
}
