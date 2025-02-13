package com.smart.sso.server.model;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName(autoResultMap = true)
public class CustomerDoubt {

    @TableId
    private Integer id;
    private String originDoubt;
    private String normDoubt;
    private String saleId;
    private String saleName;
    private String saleCategory;
    private String talkText;
    private String callId;
    private String sourceId;
    private String activityId;
    private LocalDateTime communicationTime;
    private Integer score;
    private String ext1;
    private String ext2;
}
