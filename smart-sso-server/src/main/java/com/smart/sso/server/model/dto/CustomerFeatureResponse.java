package com.smart.sso.server.model.dto;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
public class CustomerFeatureResponse {

    private Profile profile;
    private Basic basic;
    private TradingMethod tradingMethod;
    private Recognition recognition;
    private String note;

    @Getter
    @Setter
    public static class Profile {
        private String customerLifecycle;
        private Boolean hasComputerVersion;
        private Integer classCount;
        private String passwordEarnest;
        private String usageFrequency;
        private Long classLength;
    }

    @Getter
    @Setter
    public static class Basic {
        private Feature fundsVolume;
        private Feature profitLossSituation;
        private Feature earningDesire;
        private Feature courseTeacherApproval;
    }

    @Getter
    @Setter
    public static class TradingMethod {
        private Feature currentStocks;
        private Feature stockPurchaseReason;
        private Feature tradeTimingDecision;
        private Feature tradingStyle;
        private Feature stockMarketAge;
        private Feature learningAbility;
    }

    @Getter
    @Setter
    public static class Recognition {
        private Feature softwareFunctionClarity;
        private Feature stockSelectionMethod;
        private Feature selfIssueRecognition;
        private Feature softwareValueApproval;
        private Feature softwarePurchaseAttitude;
    }

    @Getter
    @Setter
    public static class Feature {
        private String inquired = "no-need";
        private Object modelRecord;
        private String salesRecord;
    }
}
