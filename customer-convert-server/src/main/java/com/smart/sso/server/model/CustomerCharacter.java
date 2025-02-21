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
    private Integer classAttendTimes;
    private Integer classAttendDuration;
    private String fundsVolume;
    private String stockPosition;
    private String tradingStyle;
    private Integer customerResponse;
    private String purchaseSimilarProduct;
    private String memberStocksBuy;
    private String memberStocksPrice;
    private String welfareStocksBuy;
    private String welfareStocksPrice;
    private String consultingPracticalClass;
    private String customerLearningFreq;
    private String continueFollowingStock;
    private String teacherProfession;
    private String teacherApprove;
    private String softwareValueApproval;
    private String softwarePurchaseAttitude;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private LocalDateTime updateTimeTelephone;
}
