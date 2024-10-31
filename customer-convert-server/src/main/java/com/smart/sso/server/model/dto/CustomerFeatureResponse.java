package com.smart.sso.server.model.dto;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Data
public class CustomerFeatureResponse {

    private ProcessSummary summary;
    private Basic basic;
    private Quantified quantified;
    private CustomerProcessSummary.TradingMethod tradingMethod;
    private BaseFeature softwareFunctionClarity;
    private BaseFeature stockSelectionMethod;
    private BaseFeature selfIssueRecognition;
    private BaseFeature softwareValueApproval;
    private BaseFeature softwarePurchaseAttitude;

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
    public static class Basic {
        private BaseFeature fundsVolume;
        private BaseFeature earningDesire;
    }

    @Getter
    @Setter
    public static class Quantified {
        private CustomerProcessSummary.ProcessInfoExplanationContent customerIssuesQuantified;
        private CustomerProcessSummary.ProcessInfoExplanationContent softwareValueQuantified;
    }
}
