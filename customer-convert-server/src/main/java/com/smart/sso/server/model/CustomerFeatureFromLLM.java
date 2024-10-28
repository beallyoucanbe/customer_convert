package com.smart.sso.server.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;

@Data
@NoArgsConstructor
public class CustomerFeatureFromLLM {
    private String id;
    private String customerId;
    private String customerName;
    private String ownerId;
    private String ownerName;
    private String currentCampaign;
    private Integer communicationDuration;
    private Timestamp communicationTime;

    private CommunicationContent fundsVolume;
    private CommunicationContent earningDesire;
    private CommunicationContent softwareFunctionClarity;
    private CommunicationContent stockSelectionMethod;
    private CommunicationContent selfIssueRecognition;
    private CommunicationContent softwareValueApproval;
    private CommunicationContent softwarePurchaseAttitude;
    private CommunicationContent currentStocks;
    private CommunicationContent stockPurchaseReason;
    private CommunicationContent tradeTimingDecision;
    private CommunicationContent tradingStyle;
    private CommunicationContent stockMarketAge;
    private CommunicationContent learningAbility;
    private CommunicationContent illustrateBasedStock;
    private CommunicationContent tradeStyleIntroduce;
    private CommunicationContent stockPickMethodReview;
    private CommunicationContent stockPickTimingReview;
    private CommunicationContent customerIssuesQuantified;
    private CommunicationContent softwareValueQuantified;
    
}
