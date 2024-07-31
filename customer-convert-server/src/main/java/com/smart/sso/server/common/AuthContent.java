package com.smart.sso.server.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 授权存储信息
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class AuthContent {
    private String tgt;
    private boolean sendLogoutRequest;
    private String redirectUri;
}