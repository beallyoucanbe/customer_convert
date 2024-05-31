package com.smart.sso.server.model.dto;

import com.smart.sso.server.common.PageRequest;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
public class CustomerInfoListRequest extends PageRequest implements Serializable {
    private String name;
    private String owner;
    private String conversionRate;
    private String currentCampaign;

    public CustomerInfoListRequest(int page, int limit, String sortBy, String order,
                                   String name, String owner, String conversionRate, String currentCampaign) {
        super(page, limit, sortBy, order);
        this.name = name;
        this.owner = owner;
        this.conversionRate = conversionRate;
        this.currentCampaign = currentCampaign;
    }
}
