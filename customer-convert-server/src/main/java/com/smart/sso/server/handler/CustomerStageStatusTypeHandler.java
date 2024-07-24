package com.smart.sso.server.handler;

import com.smart.sso.server.model.CustomerStageStatus;

public class CustomerStageStatusTypeHandler extends JsonTypeHandler<CustomerStageStatus> {
    public CustomerStageStatusTypeHandler() {
        super(CustomerStageStatus.class);
    }
}