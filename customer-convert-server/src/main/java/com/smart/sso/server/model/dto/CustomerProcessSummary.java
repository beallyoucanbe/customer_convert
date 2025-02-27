package com.smart.sso.server.model.dto;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Data
public class CustomerProcessSummary {

    // 信息收集和功能讲解
    private ProcessInfoExplanation infoExplanation;
    // 客户自己的交易方法
    private TradingMethod tradingMethod;

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

    @Getter
    @Setter
    public static class TradingMethod {
        private TradeMethodFeature currentStocks;
        private TradeMethodFeature stockPurchaseReason;
        private TradeMethodFeature tradeTimingDecision;
        private TradeMethodFeature tradingStyle;
        private TradeMethodFeature stockMarketAge;
    }
}
