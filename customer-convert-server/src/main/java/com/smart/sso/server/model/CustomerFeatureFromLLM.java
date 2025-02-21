package com.smart.sso.server.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
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
    private LocalDateTime communicationTime;
    private String callId;

    private CommunicationContent fundsVolume;
    private CommunicationContent stockPosition;
    private CommunicationContent tradingStyle;
    private List<CommunicationContent> customerResponse = new ArrayList<>();
    private CommunicationContent purchaseSimilarProduct;
    private CommunicationContent memberStocksBuy;
    private CommunicationContent memberStocksPrice;
    private CommunicationContent welfareStocksBuy;
    private CommunicationContent welfareStocksPrice;
    private CommunicationContent consultingPracticalClass;
    private CommunicationFreqContent customerLearning = new CommunicationFreqContent();
    private CommunicationContent teacherApproval;
    private CommunicationContent continueFollowingStock;
    private CommunicationContent softwareValueApproval;
    private CommunicationContent softwarePurchaseAttitude;

}
