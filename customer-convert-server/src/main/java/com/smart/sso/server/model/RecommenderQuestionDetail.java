package com.smart.sso.server.model;

import com.smart.sso.server.model.dto.CustomerFeatureResponse;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RecommenderQuestionDetail {
    private String aiConclusion;
    private CustomerFeatureResponse.RecordContent records;
}
