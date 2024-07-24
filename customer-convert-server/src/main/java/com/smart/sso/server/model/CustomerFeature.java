package com.smart.sso.server.model;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.smart.sso.server.handler.FeatureContentSalesTypeHandler;
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
    @TableField(typeHandler = FeatureContentSalesTypeHandler.class)
    private FeatureContentSales fundsVolumeSales;
    @TableField(typeHandler = FeatureContentTypeHandler.class)
    private List<FeatureContent> profitLossSituationModel;
    @TableField(typeHandler = FeatureContentSalesTypeHandler.class)
    private FeatureContentSales profitLossSituationSales;
    @TableField(typeHandler = FeatureContentTypeHandler.class)
    private List<FeatureContent> earningDesireModel;
    @TableField(typeHandler = FeatureContentSalesTypeHandler.class)
    private FeatureContentSales earningDesireSales;
    @TableField(typeHandler = FeatureContentTypeHandler.class)
    private List<FeatureContent> currentStocksModel;
    @TableField(typeHandler = FeatureContentSalesTypeHandler.class)
    private FeatureContentSales currentStocksSales;
    @TableField(typeHandler = FeatureContentTypeHandler.class)
    private List<FeatureContent> stockPurchaseReasonModel;
    @TableField(typeHandler = FeatureContentSalesTypeHandler.class)
    private FeatureContentSales stockPurchaseReasonSales;
    @TableField(typeHandler = FeatureContentTypeHandler.class)
    private List<FeatureContent> tradeTimingDecisionModel;
    @TableField(typeHandler = FeatureContentSalesTypeHandler.class)
    private FeatureContentSales tradeTimingDecisionSales;
    @TableField(typeHandler = FeatureContentTypeHandler.class)
    private List<FeatureContent> tradingStyleModel;
    @TableField(typeHandler = FeatureContentSalesTypeHandler.class)
    private FeatureContentSales tradingStyleSales;
    @TableField(typeHandler = FeatureContentTypeHandler.class)
    private List<FeatureContent> stockMarketAgeModel;
    @TableField(typeHandler = FeatureContentSalesTypeHandler.class)
    private FeatureContentSales stockMarketAgeSales;
    @TableField(typeHandler = FeatureContentTypeHandler.class)
    private List<FeatureContent> learningAbilityModel;
    @TableField(typeHandler = FeatureContentSalesTypeHandler.class)
    private FeatureContentSales learningAbilitySales;
    @TableField(typeHandler = FeatureContentTypeHandler.class)
    private List<FeatureContent> courseTeacherApprovalModel;
    @TableField(typeHandler = FeatureContentSalesTypeHandler.class)
    private FeatureContentSales courseTeacherApprovalSales;
    @TableField(typeHandler = FeatureContentTypeHandler.class)
    private List<FeatureContent> softwareFunctionClarityModel;
    @TableField(typeHandler = FeatureContentSalesTypeHandler.class)
    private FeatureContentSales softwareFunctionClaritySales;
    @TableField(typeHandler = FeatureContentTypeHandler.class)
    private List<FeatureContent> stockSelectionMethodModel;
    @TableField(typeHandler = FeatureContentSalesTypeHandler.class)
    private FeatureContentSales stockSelectionMethodSales;
    @TableField(typeHandler = FeatureContentTypeHandler.class)
    private List<FeatureContent> selfIssueRecognitionModel;
    @TableField(typeHandler = FeatureContentSalesTypeHandler.class)
    private FeatureContentSales selfIssueRecognitionSales;
    @TableField(typeHandler = FeatureContentTypeHandler.class)
    private List<FeatureContent> softwareValueApprovalModel;
    @TableField(typeHandler = FeatureContentSalesTypeHandler.class)
    private FeatureContentSales softwareValueApprovalSales;
    @TableField(typeHandler = FeatureContentTypeHandler.class)
    private List<FeatureContent> softwarePurchaseAttitudeModel;
    @TableField(typeHandler = FeatureContentSalesTypeHandler.class)
    private FeatureContentSales softwarePurchaseAttitudeSales;
    private String note;
    // 租户Id
    private String tenantId;
    // 预留信息
    private String reservedFeature;
    // 预留属性
    private String reservedProperty;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime  updateTime;
}
