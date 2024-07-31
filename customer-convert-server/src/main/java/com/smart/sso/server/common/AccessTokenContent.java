package com.smart.sso.server.common;

import com.smart.sso.client.rpc.SsoUser;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AccessTokenContent{
	private CodeContent codeContent;
	private SsoUser user;
	private String appId;
}