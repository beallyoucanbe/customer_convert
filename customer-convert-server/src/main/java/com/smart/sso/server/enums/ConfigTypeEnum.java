package com.smart.sso.server.enums;

import lombok.Getter;

@Getter
public enum ConfigTypeEnum {
    COMMON("COMMON", "通用配置"),

    LEADER_MEMBERS("leader_members", "组长组员"),
    CURRENT_ACTIVITY_ID("current_activity_id", "当前正在进行的活动"),
    ACTIVITY_ID_NAMES("activity_id_names", "活动id和name的对应关系"),

    QIWEI_APPLICATION_CONFIG("qiwei_application_config", "企微应用的配置");

    private final String value;
    private final String text;

    ConfigTypeEnum(String value, String text) {
        this.value = value;
        this.text = text;
    }
}
