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
    private Feature softwareFunctionClarity;
    private Feature stockSelectionMethod;
    private Feature selfIssueRecognition;
    private Feature softwareValueApproval;
    private Feature softwarePurchaseAttitude;

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
        private Feature fundsVolume;
        private Feature earningDesire;
    }

    @Getter
    @Setter
    public static class Quantified {
        private QuantifiedContent customerIssuesQuantified;
        private QuantifiedContent softwareValueQuantified;
    }

    @Getter
    @Setter
    public static class QuantifiedContent {
        private Object result;
        private OriginChat originChat;
    }

    @Getter
    @Setter
    public static class Feature {
        private Integer standardProcess;
        private String inquired = "no"; // no-need, no, yes
        private OriginChat inquiredOriginChat;
        private CustomerConclusion customerConclusion;
        private CustomerQuestion customerQuestion;
    }
    @Getter
    @Setter
    public static class CustomerConclusion {
        private Object modelRecord;
        private Object salesManualTag;
        private String salesRecord;
        private Object compareValue;
        private OriginChat originChat;
    }

    @Getter
    @Setter
    public static class CustomerQuestion {
        private Object modelRecord;
        private OriginChat originChat;
    }

    @Getter
    @Setter
    public static class OriginChat {
        private String id;
        private List<Message> contents;
    }

    @Getter
    @Setter
    public static class Message {
        private String role;
        private String time;
        private String content;
    }
}
