package com.smart.sso.server.model;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.smart.sso.server.handler.FeatureContentTypeHandler;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName(autoResultMap = true)
public class CustomerFeature implements Serializable, Cloneable {
    /**
     * 主键
     */
    @TableId
    private String id;
    private String customerLifecycle;
    private Boolean hasComputerVersion;
    private Integer classCount;
    private String passwordEarnest;
    private String usageFrequency;
    private Long classLength;

    @TableField(typeHandler = FeatureContentTypeHandler.class)
    private FeatureContent fundsVolume;
    @TableField(typeHandler = FeatureContentTypeHandler.class)
    private FeatureContent profitLossSituation;
    @TableField(typeHandler = FeatureContentTypeHandler.class)
    private FeatureContent earningDesire;
    @TableField(typeHandler = FeatureContentTypeHandler.class)
    private FeatureContent currentStocks;
    @TableField(typeHandler = FeatureContentTypeHandler.class)
    private FeatureContent stockPurchaseReason;
    @TableField(typeHandler = FeatureContentTypeHandler.class)
    private FeatureContent tradeTimingDecision;
    @TableField(typeHandler = FeatureContentTypeHandler.class)
    private FeatureContent tradingStyle;
    @TableField(typeHandler = FeatureContentTypeHandler.class)
    private FeatureContent stockMarketAge;
    @TableField(typeHandler = FeatureContentTypeHandler.class)
    private FeatureContent learningAbility;
    @TableField(typeHandler = FeatureContentTypeHandler.class)
    private FeatureContent courseTeacherApproval;
    @TableField(typeHandler = FeatureContentTypeHandler.class)
    private FeatureContent softwareFunctionClarity;
    @TableField(typeHandler = FeatureContentTypeHandler.class)
    private FeatureContent stockSelectionMethod;
    @TableField(typeHandler = FeatureContentTypeHandler.class)
    private FeatureContent selfIssueRecognition;
    @TableField(typeHandler = FeatureContentTypeHandler.class)
    private FeatureContent softwareValueApproval;
    @TableField(typeHandler = FeatureContentTypeHandler.class)
    private FeatureContent softwarePurchaseAttitude;

    private String note;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime  updateTime;
}
