package com.smart.sso.server.handler;

import com.smart.sso.server.model.CommunicationContent;

public class CommunicationContentTypeHandler extends JsonTypeHandler<CommunicationContent> {
    public CommunicationContentTypeHandler() {
        super(CommunicationContent.class);
    }
}