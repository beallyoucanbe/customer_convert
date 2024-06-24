package com.smart.sso.server.common;

/**
 * 有效期限管理器
 * 
 * @author Joe
 */
public interface ExpirationPolicy {
	
	/**
	 * 每5分钟执行一次
	 */
	public static final String SCHEDULED_CRON = "0 */5 * * * ?";

	/**
	 * 每15分钟执行一次
	 */
	public static final String SCHEDULED_CRON_ = "0 */15 * * * ?";
	
    /**
     * 定时清理
     */
    void verifyExpired();
}
