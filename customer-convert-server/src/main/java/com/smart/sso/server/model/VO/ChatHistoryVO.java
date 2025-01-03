package com.smart.sso.server.model.VO;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.sql.Timestamp;

@Data
public class ChatHistoryVO {
    private String id;
    @JsonProperty("duration")
    private Integer communicationDuration;
    @JsonProperty("time")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private Timestamp communicationTime;
    private ChatHistoryInfo basic;

    @Data
    public static class ChatHistoryInfo{
        private Boolean softwareFunctionClarity;
        private Boolean stockSelectionMethod;
        private Boolean selfIssueRecognition;
        private Boolean softwareValueApproval;
        private Boolean softwarePurchaseAttitude;
    }
}
