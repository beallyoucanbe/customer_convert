package com.smart.sso.server.model;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.smart.sso.server.handler.FeatureContentTypeHandler;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

@Data
@TableName(autoResultMap = true)
public class CustomerFeature implements Serializable {
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
    private Boolean understandSaleExplain;

    @TableField(typeHandler = FeatureContentTypeHandler.class)
    private FeatureContent fundsVolumeModel;
    @TableField(typeHandler = FeatureContentTypeHandler.class)
    private FeatureContent fundsVolumeSales;
    @TableField(typeHandler = FeatureContentTypeHandler.class)
    private FeatureContent profitLossSituationModel;
    @TableField(typeHandler = FeatureContentTypeHandler.class)
    private FeatureContent profitLossSituationSales;
    @TableField(typeHandler = FeatureContentTypeHandler.class)
    private FeatureContent earningDesireModel;
    @TableField(typeHandler = FeatureContentTypeHandler.class)
    private FeatureContent earningDesireSales;
    @TableField(typeHandler = FeatureContentTypeHandler.class)
    private FeatureContent currentStocksModel;
    @TableField(typeHandler = FeatureContentTypeHandler.class)
    private FeatureContent currentStocksSales;
    @TableField(typeHandler = FeatureContentTypeHandler.class)
    private FeatureContent stockPurchaseReasonModel;
    @TableField(typeHandler = FeatureContentTypeHandler.class)
    private FeatureContent stockPurchaseReasonSales;
    @TableField(typeHandler = FeatureContentTypeHandler.class)
    private FeatureContent tradeTimingDecisionModel;
    @TableField(typeHandler = FeatureContentTypeHandler.class)
    private FeatureContent tradeTimingDecisionSales;
    @TableField(typeHandler = FeatureContentTypeHandler.class)
    private FeatureContent tradingStyleModel;
    @TableField(typeHandler = FeatureContentTypeHandler.class)
    private FeatureContent tradingStyleSales;
    @TableField(typeHandler = FeatureContentTypeHandler.class)
    private FeatureContent stockMarketAgeModel;
    @TableField(typeHandler = FeatureContentTypeHandler.class)
    private FeatureContent stockMarketAgeSales;
    @TableField(typeHandler = FeatureContentTypeHandler.class)
    private FeatureContent learningAbilityModel;
    @TableField(typeHandler = FeatureContentTypeHandler.class)
    private FeatureContent learningAbilitySales;
    @TableField(typeHandler = FeatureContentTypeHandler.class)
    private FeatureContent courseTeacherApprovalModel;
    @TableField(typeHandler = FeatureContentTypeHandler.class)
    private FeatureContent courseTeacherApprovalSales;
    @TableField(typeHandler = FeatureContentTypeHandler.class)
    private FeatureContent softwareFunctionClarityModel;
    @TableField(typeHandler = FeatureContentTypeHandler.class)
    private FeatureContent softwareFunctionClaritySales;
    @TableField(typeHandler = FeatureContentTypeHandler.class)
    private FeatureContent stockSelectionMethodModel;
    @TableField(typeHandler = FeatureContentTypeHandler.class)
    private FeatureContent stockSelectionMethodSales;
    @TableField(typeHandler = FeatureContentTypeHandler.class)
    private FeatureContent selfIssueRecognitionModel;
    @TableField(typeHandler = FeatureContentTypeHandler.class)
    private FeatureContent selfIssueRecognitionSales;
    @TableField(typeHandler = FeatureContentTypeHandler.class)
    private FeatureContent softwareValueApprovalModel;
    @TableField(typeHandler = FeatureContentTypeHandler.class)
    private FeatureContent softwareValueApprovalSales;
    @TableField(typeHandler = FeatureContentTypeHandler.class)
    private FeatureContent softwarePurchaseAttitudeModel;
    @TableField(typeHandler = FeatureContentTypeHandler.class)
    private FeatureContent softwarePurchaseAttitudeSales;
    private String note;
    // 租户Id
    private String tenantId;
    // 预留信息
    private String reservedFeature;
    // 预留属性
    private String reservedProperty;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime  updateTime;
}
