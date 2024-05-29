package com.smart.sso.server.controller;


import com.smart.sso.server.common.BaseResponse;
import com.smart.sso.server.common.ResultUtils;
import com.smart.sso.server.model.VO.CustomerListVO;
import com.smart.sso.server.model.VO.CustomerProfile;
import com.smart.sso.server.model.dto.CustomerInfoListRequest;
import com.smart.sso.server.model.dto.PageListResponse;
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
    public BaseResponse<PageListResponse<CustomerListVO>> getCustomerList(@RequestParam(value = "page", defaultValue = "1") Integer page,
                                                                          @RequestParam(value = "limit", defaultValue = "10") Integer limit,
                                                                          @RequestParam(value = "sort_by", defaultValue = "update_time") String sortBy,
                                                                          @RequestParam(value = "order", defaultValue = "desc") String order,
                                                                          @RequestParam(value = "name", required = false) String name,
                                                                          @RequestParam(value = "owner", required = false) String owner,
                                                                          @RequestParam(value = "conversion_rate", required = false) String conversionRate,
                                                                          @RequestParam(value = "last_updated", required = false) String lastUpdated,
                                                                          @RequestParam(value = "current_campaign", required = false) String currentCampaign) {
        CustomerInfoListRequest params = new CustomerInfoListRequest(page, limit, sortBy, order,
                name, owner, conversionRate, lastUpdated, currentCampaign);
        PageListResponse<CustomerListVO> pageListResponse = customerInfoService.queryCustomerInfoList(params);
        return ResultUtils.success(pageListResponse);
    }

    @ApiOperation(value = "获取客户基本信息")
    @GetMapping("/customer/{id}/profile")
    public BaseResponse<CustomerProfile> getCustomerList(@PathVariable(value = "id") String id) {
        CustomerProfile customerProfile = customerInfoService.queryCustomerById(id);
        return ResultUtils.success(customerProfile);
    }


    @ApiOperation(value = "插入客户信息")
    @PostMapping("/customer")
    public BaseResponse<String> insertCustomerList(@RequestBody CustomerInfoListRequest params) {
        customerInfoService.insetCustomerInfoList();
        return ResultUtils.success("123");
    }
}
