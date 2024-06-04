package com.smart.sso.server.service;

import com.smart.sso.server.model.CustomerFeature;
import com.smart.sso.server.model.VO.CustomerProfile;
import com.smart.sso.server.model.dto.CustomerFeatureResponse;
import com.smart.sso.server.model.dto.CustomerInfoListRequest;
import com.smart.sso.server.model.dto.CustomerInfoListResponse;
import com.smart.sso.server.model.dto.CustomerProcessSummaryResponse;

public interface CustomerInfoService {

    CustomerInfoListResponse queryCustomerInfoList(CustomerInfoListRequest params);

    CustomerProfile queryCustomerById(String id);

    CustomerFeatureResponse queryCustomerFeatureById(String id);

    CustomerProcessSummaryResponse queryCustomerProcessSummaryById(String id);

    void insetCustomerInfoList();

    void insetCustomerFeature(String id);
}
