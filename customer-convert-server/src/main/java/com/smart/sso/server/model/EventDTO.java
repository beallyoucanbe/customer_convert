package com.smart.sso.server.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class EventDTO {
    private String client;
    private String eventType;
    private String eventTime;
    private String actionContent;
    private String actionSection;
    private String eventDuration;
}
