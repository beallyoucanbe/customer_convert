package com.smart.sso.server.model.dto;

import com.smart.sso.server.common.PageRequest;
import lombok.Data;

import java.io.Serializable;

@Data
public class CustomerInfoListRequest extends PageRequest implements Serializable {
    private String name;
    private String owner;
    private String conversionRate;
    private String lastUpdated;
    private String currentCampaign;
}
