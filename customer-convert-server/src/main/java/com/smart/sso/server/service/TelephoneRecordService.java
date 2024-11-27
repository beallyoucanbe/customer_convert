package com.smart.sso.server.service;

import com.smart.sso.server.model.CustomerFeatureFromLLM;
import com.smart.sso.server.model.CustomerInfo;
import com.smart.sso.server.model.TelephoneRecordStatics;
import com.smart.sso.server.model.VO.ChatDetail;
import com.smart.sso.server.model.VO.ChatHistoryVO;

import java.time.LocalDateTime;
import java.util.List;

public interface TelephoneRecordService {
    /**
     * 获取一个用户模型处理之后的所有特征
     */
    CustomerFeatureFromLLM getCustomerFeatureFromLLM(String customerId, String currentCampaign);

    ChatDetail getChatDetail(String customerId, String activityId, String callId);

    List<ChatHistoryVO> getChatHistory(String customerId, String activityId);

    /**
     * 将record 的数据同步到customer info 中（测试用）
     * @return
     */
    Boolean syncCustomerInfo();

    /**
     * 全量更新当前活动的通话次数
     * @return
     */
    void refreshCommunicationRounds();

    List<TelephoneRecordStatics> getCustomerIdUpdate(LocalDateTime dateTime);

    TelephoneRecordStatics getCommunicationRound(String customerId, String activityId);

    CustomerInfo syncCustomerInfoFromRecord(String customerId, String activityId);
}
