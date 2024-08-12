package com.smart.sso.server.enums;

import lombok.Getter;
import org.springframework.util.StringUtils;

@Getter
public enum EarningDesireEnum {
    HIGH("high", "强"),
    LOW("low", "弱");

    private final String value;
    private final String text;

    EarningDesireEnum(String value, String text) {
        this.value = value;
        this.text = text;
    }

    public static String getTextByValue(String value) {
        if (StringUtils.isEmpty(value)) {
            return null;
        }
        for (EarningDesireEnum function : values()) {
            if (value.equals(function.getValue())) {
                return function.getText();
            }
        }
        return null;
    }
}
