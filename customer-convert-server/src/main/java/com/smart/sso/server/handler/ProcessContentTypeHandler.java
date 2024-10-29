package com.smart.sso.server.handler;

import com.smart.sso.server.model.dto.CustomerProcessSummary;

public class ProcessContentTypeHandler extends JsonTypeHandler<CustomerProcessSummary.ProcessContent> {
    public ProcessContentTypeHandler() {
        super(CustomerProcessSummary.ProcessContent.class);
    }
}