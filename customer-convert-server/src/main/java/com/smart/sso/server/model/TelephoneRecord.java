package com.smart.sso.server.model;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.smart.sso.server.handler.CommunicationContentTypeHandler;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@TableName(autoResultMap = true)
public class TelephoneRecord {
    @TableId
    private String id;
    private String customerId;
    private String customerName;
    private String ownerId;
    private String ownerName;
    private String activityId;
    private Integer communicationDuration;
    private LocalDateTime communicationTime;
    private String callId;
    private String communicationType;
    @TableField(typeHandler = CommunicationContentTypeHandler.class)
    private List<CommunicationContent> ownerPrologue;
    @TableField(typeHandler = CommunicationContentTypeHandler.class)
    private List<CommunicationContent> ownerExplainCaseOrder;
    @TableField(typeHandler = CommunicationContentTypeHandler.class)
    private List<CommunicationContent> ownerResponseRefusePurchase;
    @TableField(typeHandler = CommunicationContentTypeHandler.class)
    private List<CommunicationContent> customerLearning;
    @TableField(typeHandler = CommunicationContentTypeHandler.class)
    private List<CommunicationContent> ownerInteraction;
    @TableField(typeHandler = CommunicationContentTypeHandler.class)
    private List<CommunicationContent> customerRefuseCommunication;
    @TableField(typeHandler = CommunicationContentTypeHandler.class)
    private List<CommunicationContent> ownerResponseRefuseCommunication;
    @TableField(typeHandler = CommunicationContentTypeHandler.class)
    private List<CommunicationContent> ownerPackagingCourse;
    @TableField(typeHandler = CommunicationContentTypeHandler.class)
    private List<CommunicationContent> appointmentContact;
    @TableField(typeHandler = CommunicationContentTypeHandler.class)
    private List<CommunicationContent> softwareFunctionClarity;
    @TableField(typeHandler = CommunicationContentTypeHandler.class)
    private List<CommunicationContent> stockSelectionMethod;
    @TableField(typeHandler = CommunicationContentTypeHandler.class)
    private List<CommunicationContent> selfIssueRecognition;
    @TableField(typeHandler = CommunicationContentTypeHandler.class)
    private List<CommunicationContent> softwareValueApproval;
    @TableField(typeHandler = CommunicationContentTypeHandler.class)
    private List<CommunicationContent> softwarePurchaseAttitude;
    @TableField(typeHandler = CommunicationContentTypeHandler.class)
    private List<CommunicationContent> currentStocks;
    @TableField(typeHandler = CommunicationContentTypeHandler.class)
    private List<CommunicationContent> stockPurchaseReason;
    @TableField(typeHandler = CommunicationContentTypeHandler.class)
    private List<CommunicationContent> tradeTimingDecision;
    @TableField(typeHandler = CommunicationContentTypeHandler.class)
    private List<CommunicationContent> tradingStyle;
    @TableField(typeHandler = CommunicationContentTypeHandler.class)
    private List<CommunicationContent> stockMarketAge;
    @TableField(typeHandler = CommunicationContentTypeHandler.class)
    private List<CommunicationContent> customerIssuesQuantified;
    @TableField(typeHandler = CommunicationContentTypeHandler.class)
    private List<CommunicationContent> softwareValueQuantified;
    private Integer parsed;
    private LocalDateTime createTime;
    private LocalDateTime  updateTime;
}