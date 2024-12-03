package com.smart.sso.server.service;

import com.smart.sso.server.model.CustomerRelation;

import java.time.LocalDateTime;
import java.util.List;

public interface CustomerRelationService {

    CustomerRelation getByActivityAndCustomer(String customerId, String ownerId, String activityId);

    List<CustomerRelation> getByActivityAndUpdateTime(String activityId, LocalDateTime dateTime);

    List<CustomerRelation> getByActivityAndSigned(String activityId);
}
