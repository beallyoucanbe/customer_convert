package com.smart.sso.server.model;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
@Data
@TableName(autoResultMap = true)
public class CustomerMessages implements Serializable {

    private Long id;
    private String messageId;
    private String userId;
    private String userName;
    private String salesId;
    private String salesName;
    private Integer isSalesSent;
    private Integer messageSource;
    private Integer messageType;
    private String messageContent;
    private Integer duration;
    private LocalDateTime messageTime;
    private LocalDateTime createdTime;

}
