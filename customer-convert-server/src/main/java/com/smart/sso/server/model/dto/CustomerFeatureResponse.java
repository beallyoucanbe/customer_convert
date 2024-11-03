package com.smart.sso.server.model.dto;

import lombok.*;

import java.util.List;

@Data
public class CustomerFeatureResponse {

    private ProcessSummary summary;
    private Basic basic;
    private CustomerProcessSummary.TradingMethod tradingMethod;


    @Getter
    @Setter
    public static class ProcessSummary {
        // 优势列表
        private List<String> advantage;
        // 问题列表
        private List<String> questions;
    }

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Basic {
        private BaseFeature fundsVolume;
        private BaseFeature earningDesire;
        private BaseFeature softwareFunctionClarity;
        private BaseFeature stockSelectionMethod;
        private BaseFeature selfIssueRecognition;
        private BaseFeature softwareValueApproval;
        private BaseFeature softwarePurchaseAttitude;
        private Quantified quantified;
    }

    @Getter
    @Setter
    public static class Quantified {
        private CustomerProcessSummary.ProcessInfoExplanationContent customerIssuesQuantified;
        private CustomerProcessSummary.ProcessInfoExplanationContent softwareValueQuantified;
    }
}
