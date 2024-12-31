package com.smart.sso.server.model.dto;

import com.smart.sso.server.model.EventDTO;
import lombok.*;

import java.util.List;

@Data
public class CustomerFeatureResponse {

    private ProcessSummary summary;
    private Basic basic;
    private CustomerProcessSummary.TradingMethod tradingMethod;
    private Warmth warmth;
    private HandoverPeriod handoverPeriod;


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
    @Getter
    @Setter
    @NoArgsConstructor
    public static class Warmth{
        // 交付课听课情况
        private CourseContent deliveryCourse;
        // 营销课听课情况
        private CourseContent marketingCourse;
        // 直播/圈子访问频次
        private FrequencyContent visitFreq;
        // 功能指标使用频次
        private FrequencyContent functionFreq;
        // 客户资金体量
        private ChatContent fundsVolume;
        // 是否有时间听课
        private ChatContent customerCourse;
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
    public static class HandoverPeriod{
        private HandoverPeriodBasic basic;
        private TradeMethodFeature currentStocks;
        private TradeMethodFeature tradingStyle;
        private TradeMethodFeature stockMarketAge;

    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class HandoverPeriodBasic{
        private FrequencyContent completeIntro;
        private FrequencyContent remindFreq;
        private FrequencyContent transFreq;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class RecordContent{
        private List<RecordTitle> columns;
        private List<EventDTO> data;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class RecordTitle{
        private String key;
        private String label;
    }

}
