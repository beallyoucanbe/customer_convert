package com.smart.sso.server.model.VO;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.Date;

@Data
public class CustomerProfile {
    @JsonProperty("name")
    private String customerName ;
    @JsonProperty("owner")
    private String ownerName ;
    private String currentCampaign ;
    private Integer communicationRounds ;
    private Date lastCommunicationDate ;
    private Long totalDuration ;
    private Integer customerStage ;
    private String conversionRate ;
}
