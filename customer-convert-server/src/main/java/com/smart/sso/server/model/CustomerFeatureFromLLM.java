package com.smart.sso.server.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

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
    private LocalDateTime communicationTime;
    private String callId;

    private CommunicationContent ownerPrologue;
    private CommunicationContent ownerExplainCaseOrder;
    private CommunicationContent ownerResponseRefusePurchase;
    private CommunicationFreqContent customerLearning = new CommunicationFreqContent();
    private CommunicationFreqContent ownerInteraction = new CommunicationFreqContent();
    private CommunicationContent customerRefuseCommunication;
    private CommunicationContent ownerResponseRefuseCommunication;
    private CommunicationContent ownerPackagingCourse;
    private CommunicationContent appointmentContact;
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
    private CommunicationContent customerIssuesQuantified;
    private CommunicationContent softwareValueQuantified;
}
