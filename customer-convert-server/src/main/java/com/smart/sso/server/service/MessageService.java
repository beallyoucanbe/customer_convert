package com.smart.sso.server.service;

import com.smart.sso.server.model.TextMessage;
import com.smart.sso.server.model.dto.LeadMemberRequest;

import java.time.LocalDateTime;

public interface MessageService {

    String sendMessageToChat(String url, TextMessage message);

    void sendNoticeForLeader(LeadMemberRequest leadMember, String currentCampaign, LocalDateTime dateTime);

    void updateCustomerCharacter(String id, boolean checkPurchaseAttitude);

}
