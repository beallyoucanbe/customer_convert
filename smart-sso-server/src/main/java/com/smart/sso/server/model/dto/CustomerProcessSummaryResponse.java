package com.smart.sso.server.model.dto;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;
import java.util.List;

@Data
public class CustomerProcessSummaryResponse {

    // 总结
    private ProcessSummary summary;
    // 信息收集和功能讲解
    private ProcessInfoExplanation infoExplanation;
    //疑问
    private ProcessApprovalAnalysis approvalAnalysis;


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
    public static class ProcessInfoExplanation {
        // 客户信息收集及判断完毕
        private String infoCollection;
        // 销售有结合客户的股票举例
        private Boolean stock;
        // 销售有基于客户交易风格做针对性的功能介绍
        private Boolean tradeBasedIntro;
        // 客户交易风格了解完毕
        private String tradeStyleUnderstanding;
        // 销售有点评客户的选股方法
        private Boolean stockPickReview;
        // 销售有点评客户的选股时机
        private Boolean stockTimingReview;
    }

    @Getter
    @Setter
    public static class ProcessApprovalAnalysis {
        // 方法认可
        private ProcessContent method;
        // 问题认可
        private ProcessContent issue;
        // 价值认可
        private ProcessContent value;
        // 价格认可
        private ProcessContent price;
        // 购买认可
        private ProcessContent purchase;
    }

    @Getter
    @Setter
    public static class ProcessContent {
        private String recognition;
        private List<Chat> chats;
    }

    @Getter
    @Setter
    public static class Chat {
        private String recognition;
        private List<Message> messages;
    }

    @Getter
    @Setter
    public static class Message {
        private String role;
        private String content;
        private Date time;
    }


}
