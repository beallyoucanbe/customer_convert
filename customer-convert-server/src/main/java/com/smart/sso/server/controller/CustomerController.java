package com.smart.sso.server.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.smart.sso.server.common.BaseResponse;
import com.smart.sso.server.common.ResultUtils;
import com.smart.sso.server.config.RedisConfig;
import com.smart.sso.server.mapper.CustomerInfoMapper;
import com.smart.sso.server.model.ActivityInfo;
import com.smart.sso.server.model.CustomerInfo;
import com.smart.sso.server.model.VO.ChatDetail;
import com.smart.sso.server.model.VO.ChatHistoryVO;
import com.smart.sso.server.model.VO.CustomerProfile;
import com.smart.sso.server.model.dto.CallBackRequest;
import com.smart.sso.server.model.dto.CustomerFeatureResponse;
import com.smart.sso.server.model.dto.CustomerInfoListRequest;
import com.smart.sso.server.model.dto.CustomerInfoListResponse;
import com.smart.sso.server.model.dto.CustomerProcessSummary;
import com.smart.sso.server.model.dto.LeadMemberRequest;
import com.smart.sso.server.service.ConfigService;
import com.smart.sso.server.service.CustomerInfoService;
import com.smart.sso.server.service.MessageService;
import com.smart.sso.server.service.TelephoneRecordService;
import com.smart.sso.server.util.CommonUtils;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.URISyntaxException;
import java.time.LocalDateTime;
import java.util.List;

import static com.smart.sso.server.constant.AppConstant.SOURCEID_KEY_PREFIX;

@RestController
@Slf4j
public class CustomerController {

    @Autowired
    private CustomerInfoService customerInfoService;
    @Autowired
    private MessageService messageService;
    @Autowired
    private ConfigService configService;
    @Autowired
    private TelephoneRecordService recordService;
    @Autowired
    private CustomerInfoMapper customerInfoMapper;
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @ApiOperation(value = "获取客户列表")
    @GetMapping("/customers")
    public BaseResponse<CustomerInfoListResponse> getCustomerList(@RequestParam(value = "offset", defaultValue = "1") Integer page,
                                                                  @RequestParam(value = "limit", defaultValue = "10") Integer limit,
                                                                  @RequestParam(value = "sort_by", defaultValue = "update_time") String sortBy,
                                                                  @RequestParam(value = "order", defaultValue = "desc") String order,
                                                                  @RequestParam(value = "name", required = false) String customerName,
                                                                  @RequestParam(value = "owner", required = false) String ownerName,
                                                                  @RequestParam(value = "conversion_rate", required = false) String conversionRate,
                                                                  @RequestParam(value = "activity_name", required = false) String  activityName) {
        CustomerInfoListRequest params = new CustomerInfoListRequest(page, limit, sortBy, order, customerName, ownerName, conversionRate, activityName);
        CustomerInfoListResponse commonPageList = customerInfoService.queryCustomerInfoList(params);
        return ResultUtils.success(commonPageList);
    }

    @ApiOperation(value = "获取客户基本信息")
    @GetMapping("/customer/{customer_id}/activity/{activity_id}/profile")
    public BaseResponse<CustomerProfile> getCustomerProfile(@PathVariable(value = "customer_id") String customerId,
                                                            @PathVariable(value = "activity_id") String activityId) {
        CustomerProfile customerProfile = customerInfoService.queryCustomerById(customerId, activityId);
        return ResultUtils.success(customerProfile);
    }

    @ApiOperation(value = "获取客户特征信息")
    @GetMapping("/customer/{customer_id}/activity/{activity_id}/features")
    public BaseResponse<CustomerFeatureResponse> getCustomerFeatures(@PathVariable(value = "customer_id") String customerId,
                                                                     @PathVariable(value = "activity_id") String activityId) {
        CustomerFeatureResponse FeatureProfile = customerInfoService.queryCustomerFeatureById(customerId, activityId);
        return ResultUtils.success(FeatureProfile);
    }

    @ApiOperation(value = "获取客户过程总结")
    @GetMapping("/customer/{id}/summary")
    public BaseResponse<CustomerProcessSummary> getCustomerSummary(@PathVariable(value = "id") String id) {
        CustomerProcessSummary customerSummary = customerInfoService.queryCustomerProcessSummaryById(id);
        return ResultUtils.success(customerSummary);
    }

    @ApiOperation(value = "获取客户参加的活动列表")
    @GetMapping("/customer/{customer_id}/activities")
    public BaseResponse<List<ActivityInfo>> getCustomerActivityInfo(@PathVariable(value = "customer_id") String customerId) {
        List<ActivityInfo> activityInfoList = customerInfoService.getActivityInfoByCustomerId(customerId);
        return ResultUtils.success(activityInfoList);
    }

    @ApiOperation(value = "修改客户特征信息")
    @PostMapping("/customer/{customer_id}/activity/{activity_id}/features")
    public BaseResponse<CustomerProcessSummary> modifyCustomerFeatures(@PathVariable(value = "customer_id") String customerId,
                                                                       @PathVariable(value = "activity_id") String activityId,
                                                                       @RequestBody CustomerFeatureResponse customerFeatureRequest) {
        customerInfoService.modifyCustomerFeatureById(customerId, activityId, customerFeatureRequest);
        return ResultUtils.success(null);
    }

    @ApiOperation(value = "客户识别回调")
    @PostMapping("/customer/callback")
    public BaseResponse<Void> callBack(@RequestBody CallBackRequest callBackRequest) {
        String sourceId = callBackRequest.getSourceId();
        String staffId = callBackRequest.getData().getCall().getStaffId();
        if (CollectionUtils.isEmpty(RedisConfig.staffIdList)){
            RedisConfig.staffIdList.addAll(configService.getStaffIds());
        }
        if (!RedisConfig.staffIdList.contains(staffId)) {
            log.error("staff id 不参加活动， 跳过不处理: " + staffId);
        }
        String redisKey = SOURCEID_KEY_PREFIX + sourceId;
        // 检查key是否存在于Redis中
        Boolean hasKey = redisTemplate.hasKey(redisKey);
        if (Boolean.TRUE.equals(hasKey)) {
            // key已经存在，说明已经处理过
            log.error("source id 已存在， 跳过不处理: " + sourceId);
        } else {
            customerInfoService.callback(sourceId);
        }
        return ResultUtils.success(null);
    }

    @ApiOperation(value = "redis 缓存预热，初始化")
    @GetMapping("/customer/redis_init")
    public BaseResponse<Void> redisInit() {
        String filePath = "/opt/customer-convert/callback/sourceid.txt";
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.contains("2024-08-23")){
                    continue;
                }
                String redisKey = SOURCEID_KEY_PREFIX + line.trim();
                redisTemplate.opsForValue().set(redisKey, "processed");
            }
        } catch (IOException e) {
            log.error("初始化redis 失败");
        }
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
                                       @RequestParam(value = "active_id") String activityId,
                                       String from, String manager,
                                       HttpServletResponse response) throws IOException, URISyntaxException {
        String targetUrl = customerInfoService.getRedirectUrl(customerId, activityId, from, manager);
        // 使用HttpServletResponse进行重定向
        response.sendRedirect(CommonUtils.encodeParameters(targetUrl));
        response.setStatus(302);
        return ResultUtils.success(null);
    }

    @ApiOperation(value = "触发给用户发送通知")
    @GetMapping("/customer/send_message")
    public BaseResponse<Void> sendMessage(@RequestParam(value = "id") String id) {
        log.error("触发客户的特征更新，id: " + id);
        messageService.updateCustomerCharacter(id, true);
        customerInfoService.updateCharacterCostTime(id);

        return ResultUtils.success(null);
    }

    @ApiOperation(value = "获取通话记录")
    @GetMapping("/customer/{customer_id}/activity/{activity_id}/chat_history")
    public BaseResponse<List<ChatHistoryVO>> getChatContent1(@PathVariable(value = "customer_id") String customerId,
                                                             @PathVariable(value = "activity_id") String activityId) {
        return ResultUtils.success(recordService.getChatHistory(customerId, activityId));
    }

    @ApiOperation(value = "获取单次通话")
    @GetMapping("/customer/{customer_id}/activity/{activity_id}/chat_detail")
    public BaseResponse<ChatDetail> getChatContent2(@PathVariable(value = "customer_id") String customerId,
                                                    @PathVariable(value = "activity_id") String activityId,
                                                    @RequestParam(value = "id") String callId) {
        return ResultUtils.success(recordService.getChatDetail(customerId, activityId, callId));
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
                messageService.updateCustomerCharacter(item.getId(), false);
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


    @ApiOperation(value = "统计数据")
    @PostMapping("/customer/statistics")
    public BaseResponse<List<LeadMemberRequest>> statistics() {
        customerInfoService.statistics();
        return ResultUtils.success(null);
    }

}
