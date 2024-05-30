package com.smart.sso.server.model.dto;

import com.smart.sso.server.model.FeatureContent;
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
        private FeatureContent fundsVolume;
        private FeatureContent profitLossSituation;
        private FeatureContent earningDesire;
    }

    @Getter
    @Setter
    public static class TradingMethod {
        private FeatureContent currentStocks;
        private FeatureContent stockPurchaseReason;
        private FeatureContent tradeTimingDecision;
        private FeatureContent tradingStyle;
        private FeatureContent stockMarketAge;
        private FeatureContent learningAbility;
    }

    @Getter
    @Setter
    public static class Recognition {
        private FeatureContent courseTeacherApproval;
        private FeatureContent softwareFunctionClarity;
        private FeatureContent stockSelectionMethod;
        private FeatureContent selfIssueRecognition;
        private FeatureContent softwareValueApproval;
        private FeatureContent softwarePurchaseAttitude;
    }
}
