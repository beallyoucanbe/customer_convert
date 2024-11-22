package com.smart.sso.server.service;

import com.smart.sso.server.model.TextMessage;

public interface MessageService {

    String sendMessageToChat(TextMessage message);

    /**
     * 发送购买态度总结任务
     */
    void sendPurchaseAttitudeSummary(String activityId);

    void updateCustomerCharacter(String customerId, String activityId, boolean checkPurchaseAttitude);

    String getAccessToken();

}
