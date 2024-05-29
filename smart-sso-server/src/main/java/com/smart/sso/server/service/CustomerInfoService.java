package com.smart.sso.server.service;

import com.smart.sso.server.model.CustomerInfo;
import com.smart.sso.server.model.dto.CustomerInfoListRequest;
import com.smart.sso.server.model.dto.PageListResponse;

public interface CustomerInfoService {

    PageListResponse<CustomerInfo> queryCustomerInfoList(CustomerInfoListRequest params);

    void insetCustomerInfoList();
}
