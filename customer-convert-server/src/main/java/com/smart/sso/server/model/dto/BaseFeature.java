package com.smart.sso.server.model.dto;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
public class BaseFeature extends Feature {
    private Integer standardProcess;
    private CustomerQuestion customerQuestion;

    @Getter
    @Setter
    public static class CustomerQuestion {
        private Object modelRecord;
        private OriginChat originChat;
    }

    public BaseFeature(Feature featureVo) {
        // 复制父类属性
        this.setInquired(featureVo.getInquired());
        this.setInquiredOriginChat(featureVo.getInquiredOriginChat());
        this.setCustomerConclusion(featureVo.getCustomerConclusion());
    }
}
