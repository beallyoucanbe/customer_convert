package com.smart.sso.server.model.VO;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.smart.sso.server.model.CustomerStageStatus;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.Date;

@Data
public class CustomerProfile {
    @JsonProperty("name")
    private String customerName ;
    private String customerId ;
    @JsonProperty("owner")
    private String ownerName ;
    private String activityName;
    private String activityId;
    private Integer isSend188;
    private Integer communicationRounds ;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private Date lastCommunicationDate ;
    private Long totalDuration ;
    private CustomerStageStatus customerStage ;
    private String conversionRate ;
    private Integer classeAttendTimes;
    private Integer classeAttendDuration;
}
