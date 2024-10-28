package com.smart.sso.server.service;

public interface TelephoneRecordService {
    /**
     * 获取一个用户模型处理之后的所有特征
     */
    void getCustomerFeatureFromLLM(String customerId, String currentCampaign);
}
