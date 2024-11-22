package com.smart.sso.server.model;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 公司信息;
 */
@Data
@TableName(autoResultMap = true)
public class ScheduledTask implements Serializable {
    /**
     * 主键
     */
    @TableId
    private String id;
    // 任务名称
    private String taskName;
    // 任务状态
    private String status;
    private LocalDateTime createTime;
    private LocalDateTime  updateTime;
}
