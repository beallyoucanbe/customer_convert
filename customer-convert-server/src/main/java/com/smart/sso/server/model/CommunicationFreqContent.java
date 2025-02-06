package com.smart.sso.server.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CommunicationFreqContent {
    private int remindCount = 0;
    private int communicationCount = 0;
    private int communicationTime = 0;
}


