package com.smart.sso.server.service;

import com.smart.sso.server.model.*;
import com.smart.sso.server.model.VO.CustomerProfile;
import com.smart.sso.server.model.dto.CustomerFeatureResponse;
import com.smart.sso.server.model.dto.CustomerInfoListRequest;
import com.smart.sso.server.model.dto.CustomerBaseListResponse;

import java.util.List;

public interface CustomerInfoService {

    CustomerBaseListResponse queryCustomerInfoList(CustomerInfoListRequest params);

    CustomerProfile queryCustomerById(String customerId, String campaignId);

    CustomerFeatureResponse queryCustomerFeatureById(String customerId, String activityId);

    String getConversionRate(CustomerFeatureResponse customerFeature);

    CustomerStageStatus getCustomerStageStatus(CustomerBase customerBase, CustomerFeature featureFromSale, CustomerFeatureFromLLM featureFromLLM);

    List<ActivityInfoWithVersion> getActivityInfoByCustomerId(String customerId);

    void modifyCustomerFeatureById(String customerId, String campaignId, CustomerFeatureResponse customerFeature);

    void callback(String sourceId);

    String getRedirectUrl(String customerId, String activeId, String ownerId, String owner, String from, String manager);

    void updateCharacterCostTime(String id);

    void statistics();

    void syncCustomerInfoFromRelation();

}
