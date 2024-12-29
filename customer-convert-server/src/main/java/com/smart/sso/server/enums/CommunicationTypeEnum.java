package com.smart.sso.server.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

/**
 * 客户转化率
 */
@Getter
public enum CommunicationTypeEnum {
    WECHAT("work_wechat", 0),
    PHONE("phone", 1);

    @EnumValue
    private final int value;
    @JsonValue
    private final String type;

    CommunicationTypeEnum(String type, int value) {
        this.type = type;
        this.value = value;
    }
}
