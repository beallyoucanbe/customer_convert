package com.smart.sso.server.model;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 配置项;
 */
@Data
@TableName(autoResultMap = true)
public class Config implements Serializable {
    /**
     * 主键
     */
    @TableId
    private Integer id;
    private String type;
    private String name;
    private String value;
    private String tenantId;

    private LocalDateTime createTime;
    private LocalDateTime  updateTime;
}
