package com.smart.sso.server.model.VO;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CustomerListVO {
    private String id ;
    @JsonProperty("name")
    private String customerName ;
    private String customerId ;
    @JsonProperty("owner")
    private String ownerName ;
    private String activityName;
    private String activityId;
    private String conversionRate ;
    @JsonProperty("last_update")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime ;
}
