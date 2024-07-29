package com.smart.sso.server.model;

import lombok.Data;

@Data
public class CustomerStageStatus {
    private Integer matchingJudgment = 0;
    private Integer transactionStyle = 0;
    private Integer functionIntroduction = 0;
    private Integer confirmValue = 0;
    private Integer confirmPurchase = 0;
    private Integer completePurchase = 0;
}
