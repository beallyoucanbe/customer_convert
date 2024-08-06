package com.smart.sso.server.handler;

import com.smart.sso.server.model.dto.CustomerFeatureResponse;

public class CustomerFeatureResponseTypeHandler extends JsonTypeHandler<CustomerFeatureResponse> {
    public CustomerFeatureResponseTypeHandler() {
        super(CustomerFeatureResponse.class);
    }
}