package com.smart.sso.server.model;

import lombok.Data;

@Data
public class TextMessage {

    private String touser;
    private String msgtype = "markdown";
    private String agentid;
    private int enable_duplicate_check = 1;
    private int duplicate_check_interval = 3600;
    private TextContent markdown;

    @Data
    public static class TextContent {
        private String content;
    }
}
