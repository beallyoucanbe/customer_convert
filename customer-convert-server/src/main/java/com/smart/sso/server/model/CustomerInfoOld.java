package com.smart.sso.server.model;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Date;

/**
 * 公司信息;
 */
@Data
@TableName("customer_info")
public class CustomerInfoOld implements Serializable {
    /**
     * 主键
     */
    @TableId
    private String id;
    // 客户名称
    private String customerName;
    // 客户id
    private String customerId;
    // 责任人名称
    private String ownerName;
    // 责任人Id
    private String ownerId;
    // 当前归属活动
    private String currentCampaign;
    // 转化概率，枚举值
    private String conversionRate;

    // 沟通轮次
    private Integer communicationRounds;
    // 最近沟通日期
    private Date lastCommunicationDate;
    // 沟通总时长，单位s
    private Long totalDuration;
    // 租户Id
    private String tenantId;
    // 预留信息
    private String reservedInfo;
    // 预留属性
    private String reservedProperty;
    private LocalDateTime createTime;
    private LocalDateTime  updateTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime  updateTimeTelephone;
}
