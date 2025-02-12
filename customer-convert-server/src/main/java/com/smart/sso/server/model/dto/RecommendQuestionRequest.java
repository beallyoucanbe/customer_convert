package com.smart.sso.server.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RecommendQuestionRequest {

    private String activityName;
    private String questionType;
}
