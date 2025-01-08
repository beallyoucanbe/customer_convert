package com.smart.sso.server.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

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
    private String callId;

    private CommunicationContent fundsVolume;
    private CommunicationContent hasTime;

    private CommunicationContent introduceService_1;
    private CommunicationContent introduceService_2;
    private CommunicationContent introduceService_3;
    private CommunicationContent introduceService_4;
    private CommunicationContent introduceService_5;

    private List<CommunicationContent> remindService_1 = new ArrayList<>();
    private List<CommunicationContent> remindService_2 = new ArrayList<>();
    private List<CommunicationContent> remindService_3 = new ArrayList<>();
    private List<CommunicationContent> remindService_4 = new ArrayList<>();
    private List<CommunicationContent> remindService_5 = new ArrayList<>();

    private CommunicationContent currentStocks;
    private CommunicationContent tradingStyle;
    private CommunicationContent stockMarketAge;

    private CommunicationContent earningDesire;
    private CommunicationContent softwareFunctionClarity;
    private CommunicationContent stockSelectionMethod;
    private CommunicationContent selfIssueRecognition;
    private CommunicationContent softwareValueApproval;
    private CommunicationContent softwarePurchaseAttitude;
    private CommunicationContent stockPurchaseReason;
    private CommunicationContent tradeTimingDecision;
    private CommunicationContent learningAbility;
    private CommunicationContent illustrateBasedStock;
    private CommunicationContent tradeStyleIntroduce;
    private CommunicationContent stockPickMethodReview;
    private CommunicationContent stockPickTimingReview;
    private CommunicationContent customerIssuesQuantified;
    private CommunicationContent softwareValueQuantified;
    
}
