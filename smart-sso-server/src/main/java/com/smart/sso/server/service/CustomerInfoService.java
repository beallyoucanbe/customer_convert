package com.smart.sso.server.service;

import com.smart.sso.server.model.VO.CustomerListVO;
import com.smart.sso.server.model.VO.CustomerProfile;
import com.smart.sso.server.model.dto.CustomerInfoListRequest;
import com.smart.sso.server.model.dto.PageListResponse;

public interface CustomerInfoService {

    PageListResponse<CustomerListVO> queryCustomerInfoList(CustomerInfoListRequest params);

    CustomerProfile queryCustomerById(String id);

    void insetCustomerInfoList();
}
