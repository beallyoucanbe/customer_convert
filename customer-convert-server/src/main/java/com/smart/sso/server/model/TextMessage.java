package com.smart.sso.server.model;

import lombok.Data;

@Data
public class TextMessage implements Cloneable {

    private String touser;
    private String msgtype = "markdown";
    private String agentid;
    private int enable_duplicate_check = 1;
    private int duplicate_check_interval = 3600;
    private TextContent markdown;

    @Override
    public TextMessage clone() {
        try {
            TextMessage cloned = (TextMessage) super.clone();
            // 对于内部的 TextContent 需要深拷贝，避免共享引用
            cloned.setMarkdown(cloned.getMarkdown() != null ? cloned.getMarkdown().clone() : null);
            return cloned;
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Data
    public static class TextContent implements Cloneable {
        private String content;

        @Override
        public TextContent clone() {
            try {
                return (TextContent) super.clone();
            } catch (CloneNotSupportedException e) {
                e.printStackTrace();
                return null;
            }
        }
    }
}

