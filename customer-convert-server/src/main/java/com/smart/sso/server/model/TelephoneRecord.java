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

    @TableField(typeHandler = CommunicationContentTypeHandler.class)
    private List<CommunicationContent> deliveryRemindLive;
    @TableField(typeHandler = CommunicationContentTypeHandler.class)
    private List<CommunicationContent> deliveryRemindPlayback;
    @TableField(typeHandler = CommunicationContentTypeHandler.class)
    private List<CommunicationContent> teacherApproval;
    @TableField(typeHandler = CommunicationContentTypeHandler.class)
    private List<CommunicationContent> courseMaster_1;
    @TableField(typeHandler = CommunicationContentTypeHandler.class)
    private List<CommunicationContent> courseMaster_2;
    @TableField(typeHandler = CommunicationContentTypeHandler.class)
    private List<CommunicationContent> courseMaster_3;
    @TableField(typeHandler = CommunicationContentTypeHandler.class)
    private List<CommunicationContent> courseMaster_4;
    @TableField(typeHandler = CommunicationContentTypeHandler.class)
    private List<CommunicationContent> courseMaster_5;
    @TableField(typeHandler = CommunicationContentTypeHandler.class)
    private List<CommunicationContent> courseMaster_6;
    @TableField(typeHandler = CommunicationContentTypeHandler.class)
    private List<CommunicationContent> courseMaster_7;
    @TableField(typeHandler = CommunicationContentTypeHandler.class)
    private List<CommunicationContent> customerRequireRefund;
    @TableField(typeHandler = CommunicationContentTypeHandler.class)
    private List<CommunicationContent> homework;

    private Integer parsed;
    private String tenantId;
    // 预留属性
    private String reservedInfo;
    private String reservedProperty;
    private LocalDateTime createTime;
    private LocalDateTime  updateTime;
}