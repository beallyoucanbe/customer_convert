package com.smart.sso.server.service;

import com.smart.sso.client.rpc.SsoUser;
import com.smart.sso.server.session.Role;

/**
 * 用户服务接口
 */
public interface UserService {
	
	/**
	 * 登录
	 */
	SsoUser login(String username, String password);

	/**
	 * 注册
	 * @param username
	 * @param password
	 */
	String signup(String username, String password, String role);

	SsoUser getUserById(String userId);

	Role signRole(String role, String roleName, String permission);


}
