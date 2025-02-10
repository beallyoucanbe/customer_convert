package com.smart.sso.server.model;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName(autoResultMap = true)
public class Message implements Serializable {
    /**
     * 主键
     */
    @TableId
    private String id;
    // 客户id
    private String customerId;
    // 消息目标类型：个人，群组
    private String targetType;
    // 消息类型
    private String messageType;
    // 消息类型
    private String content;
    // 活动名称
    private String activityId;
    private LocalDateTime createTime;
    private LocalDateTime  updateTime;
}
