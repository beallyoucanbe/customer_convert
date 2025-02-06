package com.smart.sso.server.model;

import lombok.*;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CommunicationFreqContent {
    private int remindCount = 0;
    private int communicationCount = 0;
    private int communicationTime = 0;
    private List<FrequencyItem> frequencyItemList = new ArrayList<>();

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FrequencyItem{
        private String callId;
        private Timestamp communicationTime;
        private Integer count;
        private String content;
    }
}


