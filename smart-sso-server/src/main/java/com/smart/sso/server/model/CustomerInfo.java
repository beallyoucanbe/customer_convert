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
    private String name;
    // 责任人
    private String owner;
    // 当前归属活动
    private String currentCampaign;
    // 转化概率，枚举值
    private String conversionRate;
    // 客户阶段，0-4
    private Integer customerStage;
    // 沟通轮次
    private Integer communicationRounds;
    // 最近沟通日期
    private Date lastCommunicationDate;
    // 沟通总时长，单位s
    private Long totalDuration;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime  updateTime;
}
