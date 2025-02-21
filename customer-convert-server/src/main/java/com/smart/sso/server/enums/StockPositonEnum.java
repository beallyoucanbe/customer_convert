package com.smart.sso.server.enums;

import lombok.Getter;
import org.springframework.util.StringUtils;

@Getter
public enum StockPositonEnum {

    LIGHT ("light ", "轻仓"),
    HEAVY (" heavy ", "重仓"),
    FULL ("full  ", "满仓"),
    EMPTY ("empty ", "空仓");

    private final String value;
    private final String text;

    StockPositonEnum(String value, String text) {
        this.value = value;
        this.text = text;
    }

    public static String getTextByValue(String value) {
        if (StringUtils.isEmpty(value)) {
            return null;
        }
        for (StockPositonEnum function : values()) {
            if (value.equals(function.getValue())) {
                return function.getText();
            }
        }
        return null;
    }
}
