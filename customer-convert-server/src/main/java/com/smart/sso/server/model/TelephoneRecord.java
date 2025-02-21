package com.smart.sso.server.model;

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
    private Integer communicationDuration;
    private LocalDateTime communicationTime;
    private String communicationType;
    private String callId;

    @TableField(typeHandler = CommunicationContentTypeHandler.class)
    private List<CommunicationContent> fundsVolume;
    @TableField(typeHandler = CommunicationContentTypeHandler.class)
    private List<CommunicationContent> stockPosition;
    @TableField(typeHandler = CommunicationContentTypeHandler.class)
    private List<CommunicationContent> customerResponse;
    @TableField(typeHandler = CommunicationContentTypeHandler.class)
    private List<CommunicationContent> tradingStyle;
    @TableField(typeHandler = CommunicationContentTypeHandler.class)
    private List<CommunicationContent> purchaseSimilarProduct;
    @TableField(typeHandler = CommunicationContentTypeHandler.class)
    private List<CommunicationContent> memberStocksBuy;
    @TableField(typeHandler = CommunicationContentTypeHandler.class)
    private List<CommunicationContent> memberStocksPrice;
    @TableField(typeHandler = CommunicationContentTypeHandler.class)
    private List<CommunicationContent> welfareStocksBuy;
    @TableField(typeHandler = CommunicationContentTypeHandler.class)
    private List<CommunicationContent> welfareStocksPrice;
    @TableField(typeHandler = CommunicationContentTypeHandler.class)
    private List<CommunicationContent> consultingPracticalClass;
    @TableField(typeHandler = CommunicationContentTypeHandler.class)
    private List<CommunicationContent> customerLearning;
    @TableField(typeHandler = CommunicationContentTypeHandler.class)
    private List<CommunicationContent> teacherApproval;
    @TableField(typeHandler = CommunicationContentTypeHandler.class)
    private List<CommunicationContent> continueFollowingStock;
    @TableField(typeHandler = CommunicationContentTypeHandler.class)
    private List<CommunicationContent> softwareValueApproval;
    @TableField(typeHandler = CommunicationContentTypeHandler.class)
    private List<CommunicationContent> softwarePurchaseAttitude;

    private Integer parsed;
    private String tenantId;
    // 预留属性
    private LocalDateTime createTime;
    private LocalDateTime  updateTime;
}