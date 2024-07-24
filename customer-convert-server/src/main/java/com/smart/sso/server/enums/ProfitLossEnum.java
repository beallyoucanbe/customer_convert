package com.smart.sso.server.enums;

import lombok.Getter;

@Getter
public enum ProfitLossEnum {
    PROFIT("profit", "盈利"),
    LOSS("loss", "亏损");

    private final String value;
    private final String text;

    ProfitLossEnum(String value, String text) {
        this.value = value;
        this.text = text;
    }
}
