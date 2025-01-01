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
    private String activityId;
    private String period;
    private Integer communicationDuration;
    private Timestamp communicationTime;
    private String communicationType;
    private String callId;

    @TableField(typeHandler = CommunicationContentTypeHandler.class)
    private List<CommunicationContent> fundsVolume;
    @TableField(typeHandler = CommunicationContentTypeHandler.class)
    private List<CommunicationContent> hasTime;

    @TableField(typeHandler = CommunicationContentTypeHandler.class)
    private List<CommunicationContent> introduceService_1;
    @TableField(typeHandler = CommunicationContentTypeHandler.class)
    private List<CommunicationContent> introduceService_2;
    @TableField(typeHandler = CommunicationContentTypeHandler.class)
    private List<CommunicationContent> introduceService_3;
    @TableField(typeHandler = CommunicationContentTypeHandler.class)
    private List<CommunicationContent> introduceService_4;
    @TableField(typeHandler = CommunicationContentTypeHandler.class)
    private List<CommunicationContent> introduceService_5;

    @TableField(typeHandler = CommunicationContentTypeHandler.class)
    private List<CommunicationContent> remindService_1;
    @TableField(typeHandler = CommunicationContentTypeHandler.class)
    private List<CommunicationContent> remindService_2;
    @TableField(typeHandler = CommunicationContentTypeHandler.class)
    private List<CommunicationContent> remindService_3;
    @TableField(typeHandler = CommunicationContentTypeHandler.class)
    private List<CommunicationContent> remindService_4;
    @TableField(typeHandler = CommunicationContentTypeHandler.class)
    private List<CommunicationContent> remindService_5;
    @TableField(typeHandler = CommunicationContentTypeHandler.class)
    private List<CommunicationContent> currentStocks;
    @TableField(typeHandler = CommunicationContentTypeHandler.class)
    private List<CommunicationContent> tradingStyle;
    @TableField(typeHandler = CommunicationContentTypeHandler.class)
    private List<CommunicationContent> stockMarketAge;

    @TableField(exist = false, typeHandler = CommunicationContentTypeHandler.class)
    private List<CommunicationContent> earningDesire;
    @TableField(exist = false, typeHandler = CommunicationContentTypeHandler.class)
    private List<CommunicationContent> softwareFunctionClarity;
    @TableField(exist = false, typeHandler = CommunicationContentTypeHandler.class)
    private List<CommunicationContent> stockSelectionMethod;
    @TableField(exist = false, typeHandler = CommunicationContentTypeHandler.class)
    private List<CommunicationContent> selfIssueRecognition;
    @TableField(exist = false, typeHandler = CommunicationContentTypeHandler.class)
    private List<CommunicationContent> softwareValueApproval;
    @TableField(exist = false, typeHandler = CommunicationContentTypeHandler.class)
    private List<CommunicationContent> softwarePurchaseAttitude;
    @TableField(exist = false, typeHandler = CommunicationContentTypeHandler.class)
    private List<CommunicationContent> stockPurchaseReason;
    @TableField(exist = false, typeHandler = CommunicationContentTypeHandler.class)
    private List<CommunicationContent> tradeTimingDecision;
    @TableField(exist = false, typeHandler = CommunicationContentTypeHandler.class)
    private List<CommunicationContent> learningAbility;
    @TableField(exist = false, typeHandler = CommunicationContentTypeHandler.class)
    private List<CommunicationContent> illustrateBasedStock;
    @TableField(exist = false, typeHandler = CommunicationContentTypeHandler.class)
    private List<CommunicationContent> tradeStyleIntroduce;
    @TableField(exist = false, typeHandler = CommunicationContentTypeHandler.class)
    private List<CommunicationContent> stockPickMethodReview;
    @TableField(exist = false, typeHandler = CommunicationContentTypeHandler.class)
    private List<CommunicationContent> stockPickTimingReview;
    @TableField(exist = false, typeHandler = CommunicationContentTypeHandler.class)
    private List<CommunicationContent> customerIssuesQuantified;
    @TableField(exist = false, typeHandler = CommunicationContentTypeHandler.class)
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