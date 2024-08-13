package com.smart.sso.server.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter

public enum CustomerRecognition {
    APPROVED("approved", 0),
    NOT_APPROVED("not_approved", 1),
    UNKNOWN("unknown", 2);

    @EnumValue
    private final int value;
    @JsonValue
    private final String text;

    CustomerRecognition(String text, int value) {
        this.text = text;
        this.value = value;
    }
}
