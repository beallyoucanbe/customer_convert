package com.smart.sso.server.enums;

import lombok.Getter;

@Getter
public enum ConfigTypeEnum {
    COMMON("COMMON", "通用配置"),

    CURRENT_CAMPAIGN("current_campaign", "当前阶段的活动"),
    LEADER("leader", "组长"),
    STAFF_ID_NEED_PROCESS("staff_id_need_process", "参加活动的销售名称"),
    CURRENT_ACTIVITY_ID("current_activity_id", "当前正在进行的活动"),
    QIWEI_APPLICATION_CONFIG("qiwei_application_config", "企微应用的配置");

    private final String value;
    private final String text;

    ConfigTypeEnum(String value, String text) {
        this.value = value;
        this.text = text;
    }
}
