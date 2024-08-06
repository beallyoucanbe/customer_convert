package com.smart.sso.server.model;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.smart.sso.server.handler.CustomerFeatureResponseTypeHandler;
import com.smart.sso.server.handler.CustomerProcessSummaryResponseTypeHandler;
import com.smart.sso.server.handler.CustomerProfileTypeHandler;
import com.smart.sso.server.model.VO.CustomerProfile;
import com.smart.sso.server.model.dto.CustomerFeatureResponse;
import com.smart.sso.server.model.dto.CustomerProcessSummaryResponse;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;


@Data
@TableName(autoResultMap = true)
public class CustomerCompleteDescribe implements Serializable {
    /**
     * 主键
     */
    @TableId
    private String id;
    // 客户阶段，0-5
    @TableField(typeHandler = CustomerProfileTypeHandler.class)
    private CustomerProfile profile;
    @TableField(typeHandler = CustomerFeatureResponseTypeHandler.class)
    private CustomerFeatureResponse feature;
    @TableField(typeHandler = CustomerProcessSummaryResponseTypeHandler.class)
    private CustomerProcessSummaryResponse summary;
    // 沟通轮次
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime  updateTime;
}
