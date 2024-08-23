package com.smart.sso.server.service;

import com.smart.sso.server.model.CustomerFeature;
import com.smart.sso.server.model.CustomerInfo;
import com.smart.sso.server.model.CustomerStageStatus;
import com.smart.sso.server.model.CustomerSummary;
import com.smart.sso.server.model.VO.CustomerProfile;
import com.smart.sso.server.model.dto.CallBackRequest;
import com.smart.sso.server.model.dto.CustomerFeatureResponse;
import com.smart.sso.server.model.dto.CustomerInfoListRequest;
import com.smart.sso.server.model.dto.CustomerInfoListResponse;
import com.smart.sso.server.model.dto.CustomerProcessSummaryResponse;
import com.smart.sso.server.model.dto.LeadMemberRequest;

import java.util.List;

public interface CustomerInfoService {

    CustomerInfoListResponse queryCustomerInfoList(CustomerInfoListRequest params);

    CustomerProfile queryCustomerById(String id);

    CustomerFeatureResponse queryCustomerFeatureById(String id);

    CustomerProcessSummaryResponse queryCustomerProcessSummaryById(String id);

    String getConversionRate(CustomerFeature customerFeature);

    CustomerStageStatus getCustomerStageStatus(CustomerInfo customerInfo, CustomerFeature customerFeature, CustomerSummary customerSummary);

    void modifyCustomerFeatureById(String id, CustomerFeatureResponse customerFeature);

    void callback(String sourceId);

    String getRedirectUrl(String customerId, String activeId, String from, String manager);

    List<LeadMemberRequest> addLeaderMember(List<LeadMemberRequest> members, boolean overwrite);

    String getChatContent(String path);

}
