package com.smart.sso.server.model.dto;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Data
public class CustomerProcessSummary {

    // 总结
    private ProcessSummary summary;
    // 信息收集和功能讲解
    private ProcessInfoExplanation infoExplanation;

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
        private Boolean result;
        private OriginChat originChat;
    }
}
