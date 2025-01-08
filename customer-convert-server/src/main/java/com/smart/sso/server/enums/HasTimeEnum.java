package com.smart.sso.server.enums;

import lombok.Getter;
import org.springframework.util.StringUtils;

@Getter
public enum HasTimeEnum {
    HIGH("abundant", "时间充裕"),
    MEDIUM("normal", "时间不太充裕"),
    LOW("unknown", "时间不确定");

    private final String value;
    private final String text;

    HasTimeEnum(String value, String text) {
        this.value = value;
        this.text = text;
    }

    public static String getTextByValue(String value) {
        if (StringUtils.isEmpty(value)) {
            return null;
        }
        for (HasTimeEnum function : values()) {
            if (value.equals(function.getValue())) {
                return function.getText();
            }
        }
        return null;
    }
}
