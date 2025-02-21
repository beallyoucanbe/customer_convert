package com.smart.sso.server.model.dto;

import lombok.*;

import java.util.List;
import java.util.Map;

@Data
public class CustomerFeatureResponse {

    private ProcessSummary summary;
    private Basic basic;
    private CustomerProcessSummary.TradingMethod tradingMethod;
    private Warmth warmth = new Warmth();
    private HandoverPeriod handoverPeriod = new HandoverPeriod();
    private DeliveryPeriod deliveryPeriod = new DeliveryPeriod();


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
        private BaseFeature hasTime;
        private BaseFeature teacherApproval;
        private BaseFeature softwarePurchaseAttitude;
        private BaseFeature courseMaster_1;
        private BaseFeature courseMaster_2;
        private BaseFeature courseMaster_3;
        private BaseFeature courseMaster_4;
        private BaseFeature courseMaster_5;
        private BaseFeature courseMaster_6;
        private BaseFeature courseMaster_7;
        private BaseFeature customerRequireRefund;

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
        // 听课次数
        private Integer classAttendTimes;
        // 听课时长
        private Integer classAttendDuration;
        // 客户资金体量
        private Feature.CustomerConclusion fundsVolume = new Feature.CustomerConclusion();
        // 客户仓位
        private Feature.CustomerConclusion stockPosition = new Feature.CustomerConclusion();
        // 交易风格
        private Feature.CustomerConclusion tradingStyle = new Feature.CustomerConclusion();
        // 客户的回复频次
        private String customerResponse;
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
    public static class HandoverPeriod{
        private HandoverPeriodBasic basic = new HandoverPeriodBasic();
        private TradeMethodFeature currentStocks;
        private TradeMethodFeature tradingStyle;
        private TradeMethodFeature stockMarketAge;

    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class DeliveryPeriod{
        private DeliveryPeriodBasic basic = new DeliveryPeriodBasic();
        private CourseTeacherFeature courseTeacher = new CourseTeacherFeature();
        private MasterCourseFeature masterCourse = new MasterCourseFeature();
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class HandoverPeriodBasic{
        private FrequencyContent completeIntro = new FrequencyContent();
        private FrequencyContent remindLiveFreq  = new FrequencyContent();
        private FrequencyContent remindCommunityFreq = new FrequencyContent();;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class DeliveryPeriodBasic{
        private FrequencyContent communicationFreq = new FrequencyContent();
        private FrequencyContent remindLiveFreq  = new FrequencyContent();
        private FrequencyContent remindPlaybackFreq = new FrequencyContent();;
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
