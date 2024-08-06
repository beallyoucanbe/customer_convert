package com.smart.sso.server.model;

import lombok.Data;

@Data
public class TextMessage {

    private String msgType;
    private TextContent markdown;

    @Data
    public static class TextContent {
        private String content;
    }
}
