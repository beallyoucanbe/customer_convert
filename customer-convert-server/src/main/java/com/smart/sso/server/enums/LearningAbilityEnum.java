package com.smart.sso.server.enums;

import lombok.Getter;
import org.springframework.util.StringUtils;

@Getter
public enum LearningAbilityEnum {
    STRONG("strong", "强"),
    WEAK("weak", "弱"),
    UNDECIDABLE("undecidable", "无法判断");

    private final String value;
    private final String text;

    LearningAbilityEnum(String value, String text) {
        this.value = value;
        this.text = text;
    }

    public static String getTextByValue(String value) {
        if (StringUtils.isEmpty(value)) {
            return null;
        }
        for (LearningAbilityEnum function : values()) {
            if (value.equals(function.getValue())) {
                return function.getText();
            }
        }
        return null;
    }
}
