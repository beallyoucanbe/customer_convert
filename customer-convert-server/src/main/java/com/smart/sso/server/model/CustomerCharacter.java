package com.smart.sso.server.model;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
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
    private String earningDesire;
    private String softwareFunctionClarity;
    private String stockSelectionMethod;
    private String selfIssueRecognition;
    private String softwareValueApproval;
    private String softwarePurchaseAttitude;

    private String summaryMatchJudgment;
    private String summaryTransactionStyle;
    private String summaryFollowCustomer;
    private String summaryFunctionIntroduction;
    private String summaryConfirmValue;
    private String summaryExecuteOrder;
    private String issuesValueQuantified;

    private int standardExplanationCompletion;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private LocalDateTime updateTimeTelephone;
}
