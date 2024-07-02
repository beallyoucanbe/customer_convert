package com.smart.sso.server.model;

import lombok.Data;

@Data
public class CustomerStageStatus {
    private Integer infoCollection;
    private Integer matchingJudgment;
    private Integer transactionStyle;
    private Integer functionIntroduction;
    private Integer confirmValue;
    private Integer confirmPurchase;
}
