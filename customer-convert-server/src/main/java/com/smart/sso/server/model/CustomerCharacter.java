package com.smart.sso.server.model;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 客户特征信息，给BI用;
 */
@Data
@TableName(autoResultMap = true)
public class CustomerCharacter implements Serializable {
    /**
     * 主键
     */
    @TableId
    private String id;
    private String customerId;
    private String customerName;
    private String ownerId;
    private String ownerName;
    private String activityName;
    private String activityId;
    private String conversionRate;

    private Boolean matchingJudgmentStage;
    private Boolean transactionStyleStage;
    private Boolean functionIntroductionStage;
    private Boolean confirmValueStage;
    private Boolean confirmPurchaseStage;
    private Boolean completePurchaseStage;

    private String fundsVolume;
    private String hasTime;
    private String completeIntro;
    private String completeStockInfo;
    private Double remindCommunityFreq;
    private Double remindLiveFreq;
    private Double visitLiveFreq;
    private Double visitCommunityFreq;
    private Double functionFreq;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private LocalDateTime updateTimeTelephone;

    private String customerRefundStatus;
    private LocalDateTime refundTime;

    private LocalDateTime latestTimeVisitLive;
    private LocalDateTime latestTimeRemindLive;
    private LocalDateTime latestTimeVisitCommunity;
    private LocalDateTime latestTimeRemindCommunity;
    private LocalDateTime latestTimeUseFunction;
    private LocalDateTime timeAddCustomer;

    private Double deliveryRemindLiveFreq;
    private Double deliveryRemindCallbackFreq;

    private Integer course1;
    private Integer course2;
    private Integer course3;
    private Integer course4;
    private Integer course5;
    private Integer course6;
    private Integer course7;
    private Integer course8;
    private Integer course9;
    private Integer course10;
    private Integer course11;
    private Integer course12;
    private Integer course13;
    private Integer course14;
    private Integer course15;
}
