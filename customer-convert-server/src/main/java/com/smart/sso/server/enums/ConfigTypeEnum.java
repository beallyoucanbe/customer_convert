package com.smart.sso.server.enums;

import lombok.Getter;

@Getter
public enum ConfigTypeEnum {
    NOTIFY_URL("NOTIFY_URL", "企微通知地址"),
    COMMON("COMMON", "通用配置"),



    CURRENT_CAMPAIGN("current_campaign", "当前阶段的活动"),
    LEADER("leader", "组长"),
    STAFF_ID_NEED_PROCESS("staff_id_need_process", "参加活动的销售名称");

    private final String value;
    private final String text;

    ConfigTypeEnum(String value, String text) {
        this.value = value;
        this.text = text;
    }
}
