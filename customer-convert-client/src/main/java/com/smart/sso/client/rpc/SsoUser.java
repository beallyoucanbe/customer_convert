package com.smart.sso.client.rpc;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SsoUser {
    private Integer id;
    private String username;
    private String role;
}
