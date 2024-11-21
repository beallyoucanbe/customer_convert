package com.smart.sso.server.service;

import com.smart.sso.server.model.*;
import com.smart.sso.server.model.VO.CustomerProfile;
import com.smart.sso.server.model.dto.CustomerFeatureResponse;
import com.smart.sso.server.model.dto.CustomerInfoListRequest;
import com.smart.sso.server.model.dto.CustomerInfoListResponse;
import com.smart.sso.server.model.dto.CustomerProcessSummary;
import com.smart.sso.server.model.dto.LeadMemberRequest;

import java.util.List;

public interface CustomerInfoService {

    CustomerInfoListResponse queryCustomerInfoList(CustomerInfoListRequest params);

    CustomerProfile queryCustomerById(String customerId, String campaignId);

    CustomerFeatureResponse queryCustomerFeatureById(String customerId, String activityId);

    CustomerProcessSummary queryCustomerProcessSummaryById(String id);

    String getConversionRate(CustomerFeatureResponse customerFeature);

    CustomerStageStatus getCustomerStageStatus(CustomerInfo customerInfo, CustomerFeature featureFromSale, CustomerFeatureFromLLM featureFromLLM);

    List<ActivityInfo> getActivityInfoByCustomerId(String customerId);

    void modifyCustomerFeatureById(String customerId, String campaignId, CustomerFeatureResponse customerFeature);

    void callback(String sourceId);

    String getRedirectUrl(String customerId, String activeId, String from, String manager);

    String getRedirectUrlOld(String customerId, String activeId);

    void updateCharacterCostTime(String id);

    void statistics();

}
