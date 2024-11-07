package com.smart.sso.server.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class QiweiApplicationConfig {
    public String agentId;
    public String corpSecret;
    public String corpId;
}
