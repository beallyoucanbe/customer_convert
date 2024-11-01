package com.smart.sso.server.model.dto;

import com.smart.sso.server.common.PageRequest;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
public class CustomerInfoListRequest extends PageRequest implements Serializable {
    private String customerName;
    private String ownerName;
    private String conversionRate;
    private String activityName;

    public CustomerInfoListRequest(int page, int limit, String sortBy, String order,
                                   String customerName, String ownerName, String conversionRate, String activityName) {
        super(page, limit, sortBy, order);
        this.customerName = customerName;
        this.ownerName = ownerName;
        this.conversionRate = conversionRate;
        this.activityName = activityName;
    }
}
