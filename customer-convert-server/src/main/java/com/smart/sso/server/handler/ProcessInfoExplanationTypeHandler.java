package com.smart.sso.server.handler;

import com.smart.sso.server.model.dto.CustomerProcessSummary;

public class ProcessInfoExplanationTypeHandler extends JsonTypeHandler<CustomerProcessSummary.ProcessInfoExplanation> {
    public ProcessInfoExplanationTypeHandler() {
        super(CustomerProcessSummary.ProcessInfoExplanation.class);
    }
}
