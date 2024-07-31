package com.smart.sso.server.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RefreshTokenContent  {
	private AccessTokenContent accessTokenContent;
	private String accessToken;
}