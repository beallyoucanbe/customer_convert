package com.smart.sso.server.handler;

import com.smart.sso.server.model.dto.CustomerProcessSummaryResponse;

public class ProcessContentTypeHandler extends JsonTypeHandler<CustomerProcessSummaryResponse.ProcessContent> {
    public ProcessContentTypeHandler() {
        super(CustomerProcessSummaryResponse.ProcessContent.class);
    }
}