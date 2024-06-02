package com.smart.sso.server.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

/**
 * 对话双方角色
 */
@Getter
public enum ChatRole {
    SALES("sales", 0),
    CUSTOMER("customer", 1);

    @EnumValue
    private final int value;
    @JsonValue
    private final String text;

    ChatRole(String text, int value) {
        this.text = text;
        this.value = value;
    }

}
