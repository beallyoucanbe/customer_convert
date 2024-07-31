package com.smart.sso.server.common;

import com.smart.sso.client.rpc.SsoUser;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 服务端回传Token对象
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class RpcAccessToken {
    private String accessToken;
    private int expiresIn;
    private String refreshToken;
    private SsoUser user;
}