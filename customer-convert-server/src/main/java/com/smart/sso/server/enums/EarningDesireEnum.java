package com.smart.sso.server.enums;

import lombok.Getter;

@Getter
public enum EarningDesireEnum {
    HIGH("high", "高"),
    LOW("low", "低");

    private final String value;
    private final String text;

    EarningDesireEnum(String value, String text) {
        this.value = value;
        this.text = text;
    }
}
