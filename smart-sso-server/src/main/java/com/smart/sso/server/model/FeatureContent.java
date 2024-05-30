package com.smart.sso.server.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FeatureContent {
    private Boolean inquired;
    private String modelRecord;
    private String salesRecord;
}
