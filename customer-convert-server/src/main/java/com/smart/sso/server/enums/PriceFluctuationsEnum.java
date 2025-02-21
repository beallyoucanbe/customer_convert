package com.smart.sso.server.enums;

import lombok.Getter;
import org.springframework.util.StringUtils;

@Getter
public enum PriceFluctuationsEnum {
    RISE("rise", "涨"),
    FALL("fall", "跌"),
    UNCHANGED("unchanged", "持平");

    private final String value;
    private final String text;

    PriceFluctuationsEnum(String value, String text) {
        this.value = value;
        this.text = text;
    }

    public static String getTextByValue(String value) {
        if (StringUtils.isEmpty(value)) {
            return null;
        }
        for (PriceFluctuationsEnum function : values()) {
            if (value.equals(function.getValue())) {
                return function.getText();
            }
        }
        return null;
    }
}
