package com.smart.sso.server.service;

import com.smart.sso.server.model.TextMessage;

import java.util.Map;

public interface MessageService {

    String sendMessageToChat(TextMessage message);

    String sendMessageToChat(String url, TextMessage message);

    /**
     * 发送购买态度总结任务
     */
    void sendPurchaseAttitudeSummary(String activityId);

    void updateCustomerCharacter(String customerId, String activityId, boolean checkPurchaseAttitude);

    String getAccessToken(String userId);

    String getAgentId(String userId);

    void sendTestMessageToSales(Map<String, String> message);

    void sendMessageForPerLeader(String userId);

    void sendCommunicationSummary(String ownerId);

}
