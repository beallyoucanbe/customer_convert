package com.smart.sso.server.model;

import lombok.*;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FeatureContent {
    private String callId;
    private String question;
    private String answer;
}


