package com.smart.sso.server.model.dto;

import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
public class TradeMethodFeature extends Feature {
    private CustomerProcessSummary.ProcessInfoExplanationContent standardAction;

    public TradeMethodFeature(Feature featureVo) {
        // 复制父类属性
        this.setInquired(featureVo.getInquired());
        this.setInquiredOriginChat(featureVo.getInquiredOriginChat());
        this.setCustomerConclusion(featureVo.getCustomerConclusion());
    }
}
