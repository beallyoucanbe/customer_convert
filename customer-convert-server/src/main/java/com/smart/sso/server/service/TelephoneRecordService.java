package com.smart.sso.server.service;

import com.smart.sso.server.model.CustomerFeatureFromLLM;
import com.smart.sso.server.model.CustomerBase;
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

    int getCommunicationTimeCurrentDay(String customerId, LocalDateTime communicationTime);

    List<TelephoneRecordStatics> getCustomerIdUpdate(LocalDateTime dateTime);

    int getCommunicationCountFromTime(String customerId, LocalDateTime dateTime);

    TelephoneRecordStatics getCommunicationRound(String customerId, String activityId);

    CustomerBase syncCustomerInfoFromRecord(String customerId, String activityId);

    // 检查主键id是否存在
    boolean existId(String id);
}
