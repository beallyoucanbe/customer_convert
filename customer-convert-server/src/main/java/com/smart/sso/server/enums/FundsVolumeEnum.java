package com.smart.sso.server.enums;

import lombok.Getter;
import org.springframework.util.StringUtils;

@Getter
public enum FundsVolumeEnum {

    GREAT_TEN_MILLION("great_ten_w", "大于10万"),
    FIVE_TO_TEN_MILLION("five_to_ten_w", "5到10万之间"),
    LESS_FIVE_MILLION("less_five_w", "小于5万"),
    ABUNDANT("abundant", "充裕"),
    DEFICIENT("deficient", "匮乏");

    private final String value;
    private final String text;

    FundsVolumeEnum(String value, String text) {
        this.value = value;
        this.text = text;
    }

    public static String getTextByValue(String value) {
        if (StringUtils.isEmpty(value)) {
            return null;
        }
        for (FundsVolumeEnum function : values()) {
            if (value.equals(function.getValue())) {
                return function.getText();
            }
        }
        return null;
    }
}
