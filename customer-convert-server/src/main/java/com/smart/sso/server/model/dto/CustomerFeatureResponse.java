package com.smart.sso.server.model.dto;

import lombok.*;

import java.util.List;
import java.util.Map;

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
        private List<Question> questions;
    }

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Basic {
        // 客户听课次数
        private Integer classAttendTimes;
        // 客户听课时长
        private Integer classAttendDuration;
        // 客户认可数
        private Integer approveCount;
        // 客户学习请教频次
        private FrequencyContent customerLearningFreq = new FrequencyContent();
        // 业务员互动频次
        private FrequencyContent ownerInteractionFreq = new FrequencyContent();
        // 客户是否愿意继续沟通
        private BaseFeature customerContinueCommunicate;
        // 业务员正确包装系列课
        private BaseFeature ownerPackagingCourse;
        // 业务员正确包装功能
        private BaseFeature ownerPackagingFunction;
        // 考察客户
        private BaseFeature examineCustomer;
        private BaseFeature fundsVolume;
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

    @Getter
    @Setter
    @NoArgsConstructor
    public static class FrequencyContent{
        private Object value;
        private RecordContent records;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class RecordContent{
        private List<RecordTitle> columns;
        private List<Map<String, Object>> data;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecordTitle{
        private String key;
        private String label;
    }

}
