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
public class CustomerRelation implements Serializable {

    private String ownerId;
    private Long customerId;
    private String ownerName;
    private String customerName;
    private String activityId;
    private Integer classeAttendTimes;
    private Integer classeAttendDuration;
    private Boolean customerSigned;
    private LocalDateTime createTime;
    private LocalDateTime  updateTime;
}
