package com.smart.sso.server.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CommunicationContent {
    private String ts;
    private String question;
    private String answerText;
    private String answerTag;
    private String doubtText;
    private String doubtTag;
    private String callId;
    private String questionCallId;
    private String answerCallId;

}


