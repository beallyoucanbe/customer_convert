package com.smart.sso.server.model.VO;

import lombok.Data;

import java.util.Date;

@Data
public class CustomerProfile {
    private String name ;
    private String owner ;
    private String currentCampaign ;
    private Integer communicationRounds ;
    private Date lastCommunicationDate ;
    private Long totalDuration ;
    private Integer customerStage ;
    private String conversionRate ;
}
