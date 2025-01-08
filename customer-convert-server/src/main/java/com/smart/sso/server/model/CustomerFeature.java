package com.smart.sso.server.model;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.smart.sso.server.handler.FeatureContentSalesTypeHandler;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName(autoResultMap = true)
public class CustomerFeature implements Serializable {
    /**
     * 主键
     */
    @TableId
    private String id;
    @TableField(typeHandler = FeatureContentSalesTypeHandler.class)
    private FeatureContentSales fundsVolumeSales;
    @TableField(typeHandler = FeatureContentSalesTypeHandler.class)
    private FeatureContentSales earningDesireSales;
    @TableField(exist = false, typeHandler = FeatureContentSalesTypeHandler.class)
    private FeatureContentSales hasTimeSales;
    @TableField(typeHandler = FeatureContentSalesTypeHandler.class)
    private FeatureContentSales currentStocksSales;
    @TableField(typeHandler = FeatureContentSalesTypeHandler.class)
    private FeatureContentSales stockPurchaseReasonSales;
    @TableField(typeHandler = FeatureContentSalesTypeHandler.class)
    private FeatureContentSales tradeTimingDecisionSales;
    @TableField(typeHandler = FeatureContentSalesTypeHandler.class)
    private FeatureContentSales tradingStyleSales;
    @TableField(typeHandler = FeatureContentSalesTypeHandler.class)
    private FeatureContentSales stockMarketAgeSales;
    @TableField(typeHandler = FeatureContentSalesTypeHandler.class)
    private FeatureContentSales learningAbilitySales;
    @TableField(typeHandler = FeatureContentSalesTypeHandler.class)
    private FeatureContentSales softwareFunctionClaritySales;
    @TableField(typeHandler = FeatureContentSalesTypeHandler.class)
    private FeatureContentSales stockSelectionMethodSales;
    @TableField(typeHandler = FeatureContentSalesTypeHandler.class)
    private FeatureContentSales selfIssueRecognitionSales;
    @TableField(typeHandler = FeatureContentSalesTypeHandler.class)
    private FeatureContentSales softwareValueApprovalSales;
    @TableField(typeHandler = FeatureContentSalesTypeHandler.class)
    private FeatureContentSales softwarePurchaseAttitudeSales;
    private LocalDateTime createTime;
    private LocalDateTime  updateTime;
}
