package com.smart.sso.server.enums;

import lombok.Getter;

@Getter
public enum FundsVolumeEnum {
    GREAT_EQUAL_TEN_MILLION("great_equal_ten_w", "大于等于10万"),
    LESS_TEN_MILLION("less_ten_w", "小于10万"),
    ABUNDANT("abundant", "充裕"),
    DEFICIENT("deficient", "匮乏");

    private final String value;
    private final String text;

    FundsVolumeEnum(String value, String text) {
        this.value = value;
        this.text = text;
    }
}
