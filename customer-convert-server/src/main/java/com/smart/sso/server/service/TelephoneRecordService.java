package com.smart.sso.server.service;

import com.smart.sso.server.model.CustomerFeatureFromLLM;
import com.smart.sso.server.model.VO.ChatDetail;
import com.smart.sso.server.model.VO.ChatHistoryVO;

import java.util.List;

public interface TelephoneRecordService {
    /**
     * 获取一个用户模型处理之后的所有特征
     */
    CustomerFeatureFromLLM getCustomerFeatureFromLLM(String customerId, String currentCampaign);

    ChatDetail getChatDetail(String customerId, String activityId, String callId);

    List<ChatHistoryVO> getChatHistory(String customerId, String activityId);

}
