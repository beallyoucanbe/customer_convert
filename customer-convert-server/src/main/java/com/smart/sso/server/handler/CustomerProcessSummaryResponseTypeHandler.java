package com.smart.sso.server.handler;

import com.smart.sso.server.model.dto.CustomerProcessSummaryResponse;

public class CustomerProcessSummaryResponseTypeHandler extends JsonTypeHandler<CustomerProcessSummaryResponse> {
    public CustomerProcessSummaryResponseTypeHandler() {
        super(CustomerProcessSummaryResponse.class);
    }
}