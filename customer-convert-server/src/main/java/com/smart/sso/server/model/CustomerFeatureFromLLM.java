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

    private List<CommunicationContent> deliveryRemindLive = new ArrayList<>();
    private List<CommunicationContent> deliveryRemindPlayback = new ArrayList<>();
    private CommunicationContent teacherApproval;

    private CommunicationContent courseMaster_1;
    private CommunicationContent courseMaster_2;
    private CommunicationContent courseMaster_3;
    private CommunicationContent courseMaster_4;
    private CommunicationContent courseMaster_5;
    private CommunicationContent courseMaster_6;
    private CommunicationContent courseMaster_7;

    private CommunicationContent customerRequireRefund;

    private CommunicationContent softwarePurchaseAttitude;


}
