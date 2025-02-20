package com.smart.sso.server.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter

public enum MessageContentType {
    TEST("TEST", "发送测试信息"),
    LONG_CALL_SUMMARY_REAL_TIME("LONG_CALL_SUMMARY_REAL_TIME", "长通话总结（实时）"),
    SCRIPT_RECOMMENDATION_PUSH("SCRIPT_RECOMMENDATION_PUSH", "话术推荐推送"),
    POTENTIAL_CUSTOMER_LIST("POTENTIAL_CUSTOMER_LIST", "潜力客户列表"),
    DISTRIBUTION_OF_CALL_DURATION("DISTRIBUTION_OF_CALL_DURATION", "通话时长分布");

    @EnumValue
    private final String value;
    @JsonValue
    private final String text;

    MessageContentType(String text, String value) {
        this.text = text;
        this.value = value;
    }
}
