package com.smart.sso.server.service;

import com.smart.sso.server.model.dto.CustomerFeatureResponse;

public interface EventService {

    CustomerFeatureResponse.RecordContent getRecordContent(String userId, String eventName);
}
