package com.smart.sso.server.model.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class CallBackRequest {
    private String bid;
    private String sourceId;
    private String updateTime;
    private Boolean status;
}
