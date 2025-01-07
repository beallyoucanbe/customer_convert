package com.smart.sso.server.service;

import com.smart.sso.server.model.CustomerInfo;

import java.time.LocalDateTime;
import java.util.List;

public interface CustomerRelationService {

    CustomerInfo getByActivityAndCustomer(String customerId, String ownerId, String activityId);

    List<CustomerInfo> getByActivity(String activityId);

    List<CustomerInfo> getByActivityAndSigned(String activityId);
}
