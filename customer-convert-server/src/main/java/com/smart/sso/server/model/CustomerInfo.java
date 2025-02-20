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

    private Integer id;
    private String userId;
    private String userName;
    private String salesId;
    private String salesName;
    private LocalDateTime accessTime;
    private Integer totalCourses_2_0;
    private Integer totalCourses_3_0;
    private Integer totalDuration_2_0;
    private Integer totalDuration_3_0;
    private Integer isPurchased_2_0;
    private Integer isPurchased_3_0;
    private LocalDateTime purchase_2_0Time;
    private LocalDateTime purchase_3_0Time;
    private LocalDateTime createTime;
    private LocalDateTime modifyTime;
    private Integer orderCycle_2_0;
    private Integer orderCycle_3_0;
}
