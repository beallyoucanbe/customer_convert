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
    private List<FeatureContent> fundsVolumeModel;
    @TableField(typeHandler = FeatureContentTypeHandler.class)
    private List<FeatureContent> fundsVolumeSales;
    @TableField(typeHandler = FeatureContentTypeHandler.class)
    private List<FeatureContent> profitLossSituationModel;
    @TableField(typeHandler = FeatureContentTypeHandler.class)
    private List<FeatureContent> profitLossSituationSales;
    @TableField(typeHandler = FeatureContentTypeHandler.class)
    private List<FeatureContent> earningDesireModel;
    @TableField(typeHandler = FeatureContentTypeHandler.class)
    private List<FeatureContent> earningDesireSales;
    @TableField(typeHandler = FeatureContentTypeHandler.class)
    private List<FeatureContent> currentStocksModel;
    @TableField(typeHandler = FeatureContentTypeHandler.class)
    private List<FeatureContent> currentStocksSales;
    @TableField(typeHandler = FeatureContentTypeHandler.class)
    private List<FeatureContent> stockPurchaseReasonModel;
    @TableField(typeHandler = FeatureContentTypeHandler.class)
    private List<FeatureContent> stockPurchaseReasonSales;
    @TableField(typeHandler = FeatureContentTypeHandler.class)
    private List<FeatureContent> tradeTimingDecisionModel;
    @TableField(typeHandler = FeatureContentTypeHandler.class)
    private List<FeatureContent> tradeTimingDecisionSales;
    @TableField(typeHandler = FeatureContentTypeHandler.class)
    private List<FeatureContent> tradingStyleModel;
    @TableField(typeHandler = FeatureContentTypeHandler.class)
    private List<FeatureContent> tradingStyleSales;
    @TableField(typeHandler = FeatureContentTypeHandler.class)
    private List<FeatureContent> stockMarketAgeModel;
    @TableField(typeHandler = FeatureContentTypeHandler.class)
    private List<FeatureContent> stockMarketAgeSales;
    @TableField(typeHandler = FeatureContentTypeHandler.class)
    private List<FeatureContent> learningAbilityModel;
    @TableField(typeHandler = FeatureContentTypeHandler.class)
    private List<FeatureContent> learningAbilitySales;
    @TableField(typeHandler = FeatureContentTypeHandler.class)
    private List<FeatureContent> courseTeacherApprovalModel;
    @TableField(typeHandler = FeatureContentTypeHandler.class)
    private List<FeatureContent> courseTeacherApprovalSales;
    @TableField(typeHandler = FeatureContentTypeHandler.class)
    private List<FeatureContent> softwareFunctionClarityModel;
    @TableField(typeHandler = FeatureContentTypeHandler.class)
    private List<FeatureContent> softwareFunctionClaritySales;
    @TableField(typeHandler = FeatureContentTypeHandler.class)
    private List<FeatureContent> stockSelectionMethodModel;
    @TableField(typeHandler = FeatureContentTypeHandler.class)
    private List<FeatureContent> stockSelectionMethodSales;
    @TableField(typeHandler = FeatureContentTypeHandler.class)
    private List<FeatureContent> selfIssueRecognitionModel;
    @TableField(typeHandler = FeatureContentTypeHandler.class)
    private List<FeatureContent> selfIssueRecognitionSales;
    @TableField(typeHandler = FeatureContentTypeHandler.class)
    private List<FeatureContent> softwareValueApprovalModel;
    @TableField(typeHandler = FeatureContentTypeHandler.class)
    private List<FeatureContent> softwareValueApprovalSales;
    @TableField(typeHandler = FeatureContentTypeHandler.class)
    private List<FeatureContent> softwarePurchaseAttitudeModel;
    @TableField(typeHandler = FeatureContentTypeHandler.class)
    private List<FeatureContent> softwarePurchaseAttitudeSales;
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
