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
@TableName(autoResultMap = true)
public class CustomerInfo implements Serializable {
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
    // 活动名称
    private String activityName;
    private String activityId;
    // 转化概率，枚举值
    private String conversionRate;
    // 沟通轮次
    private Integer communicationRounds;
    // 客户阶段，0-5
    @TableField(exist = false)
    private CustomerStageStatus customerStage;
    // 最近沟通日期
    @TableField(exist = false)
    private Date lastCommunicationDate;
    // 是否 188
    @TableField(exist = false)
    private Integer isSend188;
    private LocalDateTime createTime;
    private LocalDateTime  updateTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime  updateTimeTelephone;
}
