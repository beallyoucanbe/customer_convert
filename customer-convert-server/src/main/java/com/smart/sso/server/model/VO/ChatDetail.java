package com.smart.sso.server.model.VO;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class ChatDetail {
    private String id;
    @JsonProperty("duration")
    private Integer communicationDuration;
    @JsonProperty("time")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime communicationTime;
    List<Message> messages;
    private String type;

    @Getter
    @Setter
    public static class Message {
        private String role;
        private String time;
        private String content;
    }
}
