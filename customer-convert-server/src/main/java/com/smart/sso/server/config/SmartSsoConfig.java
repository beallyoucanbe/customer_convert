package com.smart.sso.server.config;

import javax.servlet.http.HttpSessionListener;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.web.servlet.ServletListenerRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.smart.sso.client.SmartContainer;
import com.smart.sso.client.filter.LoginFilter;
import com.smart.sso.client.filter.LogoutFilter;
import com.smart.sso.client.listener.LogoutListener;

@Configuration
public class SmartSsoConfig {

    @Value("${sso.server.url}")
    private String serverUrl;
    @Value("${sso.app.id}")
    private String appId;
    @Value("${sso.app.secret}")
    private String appSecret;
    
	/**
	 * 单实例方式注册单点登出Listener
	 * 
	 * @return
	 */
	@Bean
	public ServletListenerRegistrationBean<HttpSessionListener> LogoutListener() {
		ServletListenerRegistrationBean<HttpSessionListener> listenerRegBean = new ServletListenerRegistrationBean<>();
		LogoutListener logoutListener = new LogoutListener();
		listenerRegBean.setListener(logoutListener);
		return listenerRegBean;
	}

	/**
	 * 单点登录Filter容器
	 * 
	 * @return
	 */
	@Bean
	public FilterRegistrationBean<SmartContainer> smartContainer() {
		SmartContainer smartContainer = new SmartContainer();
		smartContainer.setServerUrl(serverUrl);
		smartContainer.setAppId(appId);
		smartContainer.setAppSecret(appSecret);
		
		// 忽略拦截URL,多个逗号分隔
        smartContainer.setExcludeUrls("/login,/logout,/oauth2/*,/customers/*,/customer/*,/assets/*,/user/*");

		smartContainer.setFilters(new LogoutFilter(), new LoginFilter());

		FilterRegistrationBean<SmartContainer> registration = new FilterRegistrationBean<>();
		registration.setFilter(smartContainer);
		registration.addUrlPatterns("/*");
		registration.setOrder(1);
		registration.setName("smartContainer");
		return registration;
	}
}
