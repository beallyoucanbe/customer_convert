package com.smart.sso.server.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

/**
 * 客户转化率
 */
@Getter
public enum ConversionRateEnum {
    LOW("low", 0),
    MEDIUM("medium", 1),
    HIGH("high", 2);

    @EnumValue
    private final int value;
    @JsonValue
    private final String text;

    ConversionRateEnum(String text, int value) {
        this.text = text;
        this.value = value;
    }
}
