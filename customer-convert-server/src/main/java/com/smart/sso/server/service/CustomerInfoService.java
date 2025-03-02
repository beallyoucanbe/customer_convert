package com.smart.sso.server.service;

import com.smart.sso.server.model.*;
import com.smart.sso.server.model.VO.CustomerProfile;
import com.smart.sso.server.model.dto.CustomerFeatureResponse;
import com.smart.sso.server.model.dto.CustomerInfoListRequest;
import com.smart.sso.server.model.dto.CustomerInfoListResponse;
import com.smart.sso.server.model.dto.CustomerProcessSummary;

import java.util.List;

public interface CustomerInfoService {

    CustomerInfoListResponse queryCustomerInfoList(CustomerInfoListRequest params);

    CustomerProfile queryCustomerById(String customerId, String campaignId);

    CustomerFeatureResponse queryCustomerFeatureById(String customerId, String activityId);

    CustomerProcessSummary queryCustomerProcessSummaryById(String id);

    String getConversionRate(CustomerFeatureResponse customerFeature);

    CustomerStageStatus getCustomerStageStatus(CustomerInfo customerInfo, CustomerFeature featureFromSale, CustomerFeatureFromLLM featureFromLLM);

    List<ActivityInfoWithVersion> getActivityInfoByCustomerId(String customerId);

    List<ActivityInfo> getAllActivityInfo();

    void modifyCustomerFeatureById(String customerId, String campaignId, CustomerFeatureResponse customerFeature);

    void callback(String ownerId, String sourceId);

    String getRedirectUrl(String customerId, String activeId, String ownerId, String owner, String from, String manager);

    String getRedirectUrlOld(String customerId, String activeId);

    void updateCharacterCostTime(String id);

    void statistics();

    void syncCustomerInfoFromRelation();

    /**
     * 获取超过三天为联系的客户人员
     * @param activeId
     * @return
     */
    List<CustomerInfo> getCustomerInfoLongTimeNoSee(String activeId);

}
