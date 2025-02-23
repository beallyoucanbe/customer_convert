package com.smart.sso.server.enums;

import lombok.Getter;
import org.springframework.util.StringUtils;

@Getter
public enum FundsVolumeEnum {

    GREAT_THIRTY_W("great_thirty_w", "大于30万"),
    TWENTY_TO_THIRTY_W(" twenty_to_thirty_w", "20万到30万之间"),
    FIFTEEN_TO_TWENTY_W("fifteen_to_twenty_w", "15万到20万之间"),
    LESS_FIFTEEN_W("less_fifteen_w", "小于15万");

    private final String value;
    private final String text;

    FundsVolumeEnum(String value, String text) {
        this.value = value;
        this.text = text;
    }

    public static String getTextByValue(String value) {
        if (StringUtils.isEmpty(value)) {
            return null;
        }
        for (FundsVolumeEnum function : values()) {
            if (value.equals(function.getValue())) {
                return function.getText();
            }
        }
        return null;
    }
}
