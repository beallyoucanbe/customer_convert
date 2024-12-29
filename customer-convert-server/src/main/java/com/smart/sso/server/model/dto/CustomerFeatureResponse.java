package com.smart.sso.server.model.dto;

import lombok.*;

import java.util.List;

@Data
public class CustomerFeatureResponse {

    private ProcessSummary summary;
    private Basic basic;
    private CustomerProcessSummary.TradingMethod tradingMethod;
//    private


    @Getter
    @Setter
    public static class ProcessSummary {
        // 优势列表
        private List<String> advantage;
        // 问题列表
        private List<Question> questions;
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

    @Getter
    @Setter
    @NoArgsConstructor
    public static class Question{
        private String message;
        private String complete;
        private String incomplete;
        private String quantify;
        private String inquantify;
        private String question;

        public Question(String message){
            this.message = message;
        }
    }

    /**
     * 客户温度
     */
    public static class Warmth{

        // 交付课听课情况
//        private delivery_course

    }

}
