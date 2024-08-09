package com.smart.sso.server.enums;

import lombok.Getter;
import org.springframework.util.StringUtils;

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

    public static String getTextByValue(String value) {
        if (StringUtils.isEmpty(value)) {
            return null;
        }
        for (ProfitLossEnum function : values()) {
            if (value.equals(function.getValue())) {
                return function.getText();
            }
        }
        return null;
    }
}
