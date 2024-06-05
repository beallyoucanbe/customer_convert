package com.smart.sso.server.model;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.smart.sso.server.handler.InquiredSerializer;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FeatureContent {
    @JsonSerialize(using = InquiredSerializer.class)
    private Boolean inquired;
    private Object modelRecord;
    private String salesRecord;
}
