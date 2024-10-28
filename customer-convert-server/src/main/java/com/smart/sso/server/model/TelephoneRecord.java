package com.smart.sso.server.model;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.smart.sso.server.handler.CommunicationContentTypeHandler;
import lombok.Data;

import java.sql.Timestamp;
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
    private String currentCampaign;
    private Integer communicationDuration;
    private Timestamp communicationTime;
    private String callId;

    @TableField(typeHandler = CommunicationContentTypeHandler.class)
    private List<CommunicationContent> fundsVolume;
    @TableField(typeHandler = CommunicationContentTypeHandler.class)
    private List<CommunicationContent> earningDesire;
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
    private List<CommunicationContent> learningAbility;
    @TableField(typeHandler = CommunicationContentTypeHandler.class)
    private List<CommunicationContent> illustrateBasedStock;
    @TableField(typeHandler = CommunicationContentTypeHandler.class)
    private List<CommunicationContent> tradeStyleIntroduce;
    @TableField(typeHandler = CommunicationContentTypeHandler.class)
    private List<CommunicationContent> stockPickMethodReview;
    @TableField(typeHandler = CommunicationContentTypeHandler.class)
    private List<CommunicationContent> stockPickTimingReview;

    @TableField(typeHandler = CommunicationContentTypeHandler.class)
    private List<CommunicationContent> customerIssuesQuantified;
    @TableField(typeHandler = CommunicationContentTypeHandler.class)
    private List<CommunicationContent> softwareValueQuantified;

    private Integer parsed;
    private String tenantId;
    // 预留属性
    private String reservedInfo;
    private String reservedProperty;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime  updateTime;
}