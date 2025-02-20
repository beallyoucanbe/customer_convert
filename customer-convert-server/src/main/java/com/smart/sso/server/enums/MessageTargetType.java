package com.smart.sso.server.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter

public enum MessageTargetType {
    USER("user", 0),
    GROUP("group", 1);

    @EnumValue
    private final int value;
    @JsonValue
    private final String text;

    MessageTargetType(String text, int value) {
        this.text = text;
        this.value = value;
    }
}
