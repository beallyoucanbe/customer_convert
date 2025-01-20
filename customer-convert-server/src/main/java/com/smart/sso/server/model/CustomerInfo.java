package com.smart.sso.server.model;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 客户-销售关系表，客户同步
 */
@Data
@TableName(autoResultMap = true)
public class CustomerInfo implements Serializable {

    private Long customerId;
    private String customerName;
    private String salesName;
    private Long salesId;
    private Integer activityId;
    private String activityName;
    private Integer customerPurchaseStatus;
    private LocalDateTime purchaseTime;
    private Integer customerRefundStatus;
    private LocalDateTime refundTime;
    private String ext1;
    private String ext2;
}
