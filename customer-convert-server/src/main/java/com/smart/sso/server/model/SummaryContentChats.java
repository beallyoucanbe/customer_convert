package com.smart.sso.server.model;

import lombok.*;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SummaryContentChats {
    private List<Chat> chats;
    private String time;

    @Getter
    @Setter
    public static class Chat {
        private List<Message> messages;
        private String recognition;
    }

    @Getter
    @Setter
    public static class Message {
        private String role;
        private String content;
    }
}




