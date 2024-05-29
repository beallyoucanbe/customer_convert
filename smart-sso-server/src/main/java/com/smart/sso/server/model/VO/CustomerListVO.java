package com.smart.sso.server.model.VO;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.Date;

@Data
public class CustomerListVO {
    private String id ;
    private String name ;
    private String owner ;
    private String currentCampaign ;
    private String conversionRate ;
    @JsonProperty("last_update")
    private Date updateTime ;
}
