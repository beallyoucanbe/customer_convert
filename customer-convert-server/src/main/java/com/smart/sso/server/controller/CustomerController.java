package com.smart.sso.server.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.smart.sso.server.common.BaseResponse;
import com.smart.sso.server.common.ResultUtils;
import com.smart.sso.server.mapper.CustomerInfoMapper;
import com.smart.sso.server.model.CustomerInfo;
import com.smart.sso.server.model.VO.CustomerProfile;
import com.smart.sso.server.model.dto.CallBackRequest;
import com.smart.sso.server.model.dto.CustomerFeatureResponse;
import com.smart.sso.server.model.dto.CustomerInfoListRequest;
import com.smart.sso.server.model.dto.CustomerInfoListResponse;
import com.smart.sso.server.model.dto.CustomerProcessSummaryResponse;
import com.smart.sso.server.model.dto.LeadMemberRequest;
import com.smart.sso.server.service.CustomerInfoService;
import com.smart.sso.server.service.MessageService;
import com.smart.sso.server.util.CommonUtils;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URISyntaxException;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@Slf4j
public class CustomerController {

    @Autowired
    private CustomerInfoService customerInfoService;
    @Autowired
    private MessageService messageService;
    @Autowired
    private CustomerInfoMapper customerInfoMapper;

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

    @ApiOperation(value = "修改客户过程总结")
    @PostMapping("/customer/{id}/features")
    public BaseResponse<CustomerProcessSummaryResponse> modifyCustomerFeatures(@PathVariable(value = "id") String id,
                                                                               @RequestBody CustomerFeatureResponse customerFeatureRequest) {
        customerInfoService.modifyCustomerFeatureById(id, customerFeatureRequest);
        return ResultUtils.success(null);
    }

    @ApiOperation(value = "客户识别回调")
    @PostMapping("/customer/callback")
    public BaseResponse<Void> callBack(@RequestBody CallBackRequest callBackRequest) {
        customerInfoService.callback(callBackRequest);
        return ResultUtils.success(null);
    }

    @ApiOperation(value = "存活检查接口")
    @GetMapping("/customer/check")
    public BaseResponse<Void> checkAlive() {
        return ResultUtils.success(null);
    }

    @ApiOperation(value = "跳转接口")
    @GetMapping("/customer/redirect")
    public BaseResponse<Void> redirect(@RequestParam(value = "customer_id") String customerId,
                                       @RequestParam(value = "active_id") String activeId,
                                       String from, String manager,
                                       HttpServletResponse response) throws IOException, URISyntaxException {
        String targetUrl = customerInfoService.getRedirectUrl(customerId, activeId, from, manager);
        // 使用HttpServletResponse进行重定向
        response.sendRedirect(CommonUtils.encodeParameters(targetUrl));
        response.setStatus(302);
        return ResultUtils.success(null);
    }

    @ApiOperation(value = "触发给用户发送通知")
    @GetMapping("/customer/send_message")
    public BaseResponse<Void> sendMessage(@RequestParam(value = "id") String id) {
        messageService.sendNoticeForSingle(id);
        return ResultUtils.success(null);
    }

    @ApiOperation(value = "返回对话的原文")
    @GetMapping("/customer/content")
    public BaseResponse<String> getChatContent(@RequestParam(value = "path") String path) {
        return ResultUtils.success(customerInfoService.getChatContent(path));
    }

    @ApiOperation(value = "全量初始化")
    @GetMapping("/customer/init_character")
    public BaseResponse<Void> initCharacter() {
        LocalDateTime dateTime = LocalDateTime.of(2024, 1, 1, 12, 0, 0);
        QueryWrapper<CustomerInfo> queryWrapper = new QueryWrapper<>();
        // 筛选时间
        queryWrapper.gt("update_time", dateTime);
        List<CustomerInfo> customerFeatureList = customerInfoMapper.selectList(queryWrapper);
        for (CustomerInfo item : customerFeatureList) {
            try {
                messageService.updateCustomerCharacter(item.getId());
            } catch (Exception e) {
                log.error("更新CustomerCharacter失败：ID=" + item.getId());
            }
        }
        return ResultUtils.success(null);
    }

    @ApiOperation(value = "更新组长和组员的关系（增量）")
    @PostMapping("/customer/leader_members")
    public BaseResponse<List<LeadMemberRequest>> leaderMembers(@RequestBody List<LeadMemberRequest> members,
                                                                 @RequestParam(value = "overwrite", defaultValue = "true") boolean overwrite) {
        List<LeadMemberRequest> newMembers = customerInfoService.addLeaderMember(members, overwrite);
        return ResultUtils.success(newMembers);
    }

}
