package com.smart.sso.server.model;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Date;

/**
 * 配置项;
 */
@Data
@TableName(autoResultMap = true)
public class Events implements Serializable {
    /**
     * 主键
     */
    @TableId
    private Integer userId;
    private String eventName;
    private Date eventTime;
    private Integer eventDuration;
    private String actionType;
    private String actionContent;
    private String classType;
    private Integer teacherId;

    private String ext1;
    private String ext2;
}
