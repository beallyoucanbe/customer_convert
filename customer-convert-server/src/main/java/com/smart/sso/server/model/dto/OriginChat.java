package com.smart.sso.server.model.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class OriginChat {
    private String id;
    private List<Message> contents;

    @Getter
    @Setter
    public static class Message {
        private String role;
        private String time;
        private String content;
    }
}


