package com.smart.sso.server.handler;

import com.smart.sso.server.model.VO.CustomerProfile;

public class CustomerProfileTypeHandler extends JsonTypeHandler<CustomerProfile> {
    public CustomerProfileTypeHandler() {
        super(CustomerProfile.class);
    }
}