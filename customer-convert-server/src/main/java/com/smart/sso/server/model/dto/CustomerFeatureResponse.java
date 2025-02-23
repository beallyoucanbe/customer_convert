package com.smart.sso.server.model.dto;

import lombok.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Data
public class CustomerFeatureResponse {

    private ProcessSummary summary;
    private Basic basic = new Basic();
    private CustomerProcessSummary.TradingMethod tradingMethod;
    private Warmth warmth = new Warmth();

    @Getter
    @Setter
    public static class ProcessSummary {
        // 优势列表
        private List<String> advantage = new ArrayList<>();
        // 问题列表
        private List<Question> questions = new ArrayList<>();
    }

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Basic {
        private BaseFeature memberStocksBuy;
        private BaseFeature memberStocksPrice;
        private BaseFeature welfareStocksBuy;
        private BaseFeature welfareStocksPrice;
        private BaseFeature consultingPracticalClass;
        private FrequencyContent customerLearningFreq;
        private CourseTeacherFeature teacherApproval;
        private BaseFeature continueFollowingStock;
        private BaseFeature softwareValueApproval;
        private BaseFeature softwarePurchaseAttitude;
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
    @Getter
    @Setter
    @NoArgsConstructor
    public static class Warmth{
        // 2.0听课次数
        private Integer classAttendTimes_2;
        // 2.0听课时长
        private Integer classAttendDuration_2;
        // 3.0听课次数
        private Integer classAttendTimes_3;
        // 3.0听课时长
        private Integer classAttendDuration_3;
        // 客户资金体量
        private Feature.CustomerConclusion fundsVolume = new Feature.CustomerConclusion();
        // 客户仓位
        private Feature.CustomerConclusion stockPosition = new Feature.CustomerConclusion();
        // 交易风格
        private Feature.CustomerConclusion tradingStyle = new Feature.CustomerConclusion();
        // 客户的回复频次
        private Integer customerResponse;
        // 是否购买类似的产品
        private Feature.CustomerConclusion purchaseSimilarProduct = new Feature.CustomerConclusion();
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class CourseContent{
        private Integer total;
        private Integer process;
        private RecordContent records;
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
    public static class ChatContent{
        private String value;
        private OriginChat originChat;
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
