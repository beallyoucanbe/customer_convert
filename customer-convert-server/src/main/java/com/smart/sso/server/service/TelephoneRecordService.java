package com.smart.sso.server.service;

import com.smart.sso.server.model.CustomerFeatureFromLLM;

public interface TelephoneRecordService {
    /**
     * 获取一个用户模型处理之后的所有特征
     */
    CustomerFeatureFromLLM getCustomerFeatureFromLLM(String customerId, String currentCampaign);
}
