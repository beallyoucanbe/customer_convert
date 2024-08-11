package com.smart.sso.server.model.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
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
        // 销售有结合客户的股票举例
        private ProcessInfoExplanationContent stock;
        // 销售有基于客户交易风格做针对性的功能介绍
        private ProcessInfoExplanationContent tradeBasedIntro;
        // 销售有点评客户的选股方法
        private ProcessInfoExplanationContent stockPickReview;
        // 销售有点评客户的选股时机
        private ProcessInfoExplanationContent stockTimingReview;
        // 销售有对客户的问题做量化放大
        private ProcessInfoExplanationContent customerIssuesQuantified;
        // 销售有对软件的价值做量化放大
        private ProcessInfoExplanationContent softwareValueQuantified;
    }

    @Getter
    @Setter
    public static class ProcessInfoExplanationContent {
        // 方法认可
        private Boolean result;
        // 问题认可
        private OriginChat originChat;
    }

    @Getter
    @Setter
    public static class OriginChat {
        private String id;
        private String content;
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
        // 客户学不会软件操作
        private ProcessContent softwareOperation;
        // 质疑老师和课程
        private ProcessContent course;
        // 客户没钱购买软件
        private ProcessContent noMoney;
        // 其它
        private ProcessContent others;
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
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
        private Date time;
    }

    @Getter
    @Setter
    public static class Message {
        private String role;
        private String content;
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
        private Date time;
    }


}
