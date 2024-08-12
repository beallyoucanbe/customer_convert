package com.smart.sso.server.model.dto;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

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
    }

    @Getter
    @Setter
    public static class TradingMethod {
        private FeatureSpecial currentStocks;
        private FeatureSpecial stockPurchaseReason;
        private FeatureSpecial tradeTimingDecision;
        private Feature tradingStyle;
        private Feature stockMarketAge;
        private Feature learningAbility;
    }

    @Getter
    @Setter
    public static class Recognition {
        private Feature courseTeacherApproval;
        private Feature softwareFunctionClarity;
        private Feature stockSelectionMethod;
        private Feature selfIssueRecognition;
        private Feature learnNewMethodApproval;
        private Feature continuousLearnApproval;
        private Feature softwareValueApproval;
        private Feature softwarePurchaseAttitude;
    }

    @Getter
    @Setter
    public static class Feature {
        private String inquired = "no"; // no-need, no, yes
        private Object modelRecord;
        private String salesRecord;
        private Object salesManualTag;
        private Object compareValue;
        private OriginChat originChat;
        private OriginChat inquiredOriginChat;
    }

    @Getter
    @Setter
    public static class FeatureSpecial {
        private String inquired = "no"; // no-need, no, yes
        private Object modelRecord;
        private String salesRecord;
        private Object salesManualTag;
        private Object compareValue;
        private List<OriginChat> originChats;
        private OriginChat inquiredOriginChat;
    }

    @Getter
    @Setter
    public static class OriginChat {
        private String id;
        private String content;
    }
}
