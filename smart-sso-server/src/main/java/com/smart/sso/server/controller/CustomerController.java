package com.smart.sso.server.controller;

import com.smart.sso.server.common.BaseResponse;
import com.smart.sso.server.common.ResultUtils;
import com.smart.sso.server.model.VO.CustomerProfile;
import com.smart.sso.server.model.dto.CustomerFeatureResponse;
import com.smart.sso.server.model.dto.CustomerInfoListRequest;
import com.smart.sso.server.model.dto.CustomerInfoListResponse;
import com.smart.sso.server.model.dto.CustomerProcessSummaryResponse;
import com.smart.sso.server.service.CustomerInfoService;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CustomerController {

    @Autowired
    private CustomerInfoService customerInfoService;

    @ApiOperation(value = "获取客户列表")
    @GetMapping("/customers")
    public BaseResponse<CustomerInfoListResponse> getCustomerList(@RequestParam(value = "offset", defaultValue = "1") Integer page,
                                                                  @RequestParam(value = "limit", defaultValue = "10") Integer limit,
                                                                  @RequestParam(value = "sort_by", defaultValue = "update_time") String sortBy,
                                                                  @RequestParam(value = "order", defaultValue = "desc") String order,
                                                                  @RequestParam(value = "name", required = false) String customerName,
                                                                  @RequestParam(value = "owner", required = false) String ownerName,
                                                                  @RequestParam(value = "conversion_rate", required = false) String conversionRate,
                                                                  @RequestParam(value = "current_campaign", required = false) String currentCampaign) {
        CustomerInfoListRequest params = new CustomerInfoListRequest(page, limit, sortBy, order, customerName, ownerName, conversionRate, currentCampaign);
        CustomerInfoListResponse commonPageList = customerInfoService.queryCustomerInfoList(params);
        return ResultUtils.success(commonPageList);
    }

    @ApiOperation(value = "获取客户基本信息")
    @GetMapping("/customer/{id}/profile")
    public BaseResponse<CustomerProfile> getCustomerProfile(@PathVariable(value = "id") String id) {
        CustomerProfile customerProfile = customerInfoService.queryCustomerById(id);
        return ResultUtils.success(customerProfile);
    }

    @ApiOperation(value = "获取客户特征信息")
    @GetMapping("/customer/{id}/features")
    public BaseResponse<CustomerFeatureResponse> c(@PathVariable(value = "id") String id) {
        CustomerFeatureResponse FeatureProfile = customerInfoService.queryCustomerFeatureById(id);
        return ResultUtils.success(FeatureProfile);
    }

    @ApiOperation(value = "获取客户过程总结")
    @GetMapping("/customer/{id}/summary")
    public BaseResponse<CustomerProcessSummaryResponse> getCustomerSummary(@PathVariable(value = "id") String id) {
        CustomerProcessSummaryResponse customerSummary = customerInfoService.queryCustomerProcessSummaryById(id);
        return ResultUtils.success(customerSummary);
    }

    @ApiOperation(value = "获取客户过程总结")
    @PostMapping("/customer/{id}/features")
    public BaseResponse<CustomerProcessSummaryResponse> modifyCustomerFeatures(@PathVariable(value = "id") String id,
                                                                               @RequestBody CustomerFeatureResponse customerFeatureRequest) {
        customerInfoService.modifyCustomerFeatureById(id, customerFeatureRequest);
        return ResultUtils.success(null);
    }

}
