package com.smart.sso.server.service;

import com.smart.sso.server.model.CustomerFeature;
import com.smart.sso.server.model.CustomerStageStatus;
import com.smart.sso.server.model.CustomerSummary;
import com.smart.sso.server.model.VO.CustomerProfile;
import com.smart.sso.server.model.dto.CallBackRequest;
import com.smart.sso.server.model.dto.CustomerFeatureResponse;
import com.smart.sso.server.model.dto.CustomerInfoListRequest;
import com.smart.sso.server.model.dto.CustomerInfoListResponse;
import com.smart.sso.server.model.dto.CustomerProcessSummaryResponse;

public interface CustomerInfoService {

    CustomerInfoListResponse queryCustomerInfoList(CustomerInfoListRequest params);

    CustomerProfile queryCustomerById(String id);

    CustomerFeatureResponse queryCustomerFeatureById(String id);

    CustomerProcessSummaryResponse queryCustomerProcessSummaryById(String id);

    String getConversionRate(CustomerFeature customerFeature);

    CustomerStageStatus getCustomerStageStatus(CustomerFeature customerFeature, CustomerSummary customerSummary);

    void modifyCustomerFeatureById(String id, CustomerFeatureResponse customerFeature);

    void callback(CallBackRequest callBackRequest);

}
