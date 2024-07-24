package com.smart.sso.server.handler;

import com.smart.sso.server.model.dto.CustomerProcessSummaryResponse;

public class ProcessInfoExplanationTypeHandler extends JsonTypeHandler<CustomerProcessSummaryResponse.ProcessInfoExplanation> {
    public ProcessInfoExplanationTypeHandler() {
        super(CustomerProcessSummaryResponse.ProcessInfoExplanation.class);
    }
}
