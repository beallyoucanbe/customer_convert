package com.smart.sso.server.enums;

import lombok.Getter;
import org.springframework.util.StringUtils;

@Getter
public enum TradingStyleEnum {

    SHORT ("short", "短线"),
    LONG ("long", "长线"),
    BAND ("band", "波段"),
    UNSTEADY ("unsteady", "不固定风格");

    private final String value;
    private final String text;

    TradingStyleEnum(String value, String text) {
        this.value = value;
        this.text = text;
    }

    public static String getTextByValue(String value) {
        if (StringUtils.isEmpty(value)) {
            return null;
        }
        for (TradingStyleEnum function : values()) {
            if (value.equals(function.getValue())) {
                return function.getText();
            }
        }
        return null;
    }
}
