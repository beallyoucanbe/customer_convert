package com.smart.sso.server.controller;

import com.smart.sso.server.common.BaseResponse;
import com.smart.sso.server.common.ResultUtils;
import com.smart.sso.server.constant.AppConstant;
import com.smart.sso.server.model.ActivityInfoWithVersion;
import com.smart.sso.server.model.Config;
import com.smart.sso.server.model.CustomerInfo;
import com.smart.sso.server.model.TelephoneRecordStatics;
import com.smart.sso.server.model.VO.ChatDetail;
import com.smart.sso.server.model.VO.ChatHistoryVO;
import com.smart.sso.server.model.VO.CustomerProfile;
import com.smart.sso.server.model.dto.CallBackRequest;
import com.smart.sso.server.model.dto.CustomerFeatureResponse;
import com.smart.sso.server.model.dto.CustomerInfoListRequest;
import com.smart.sso.server.model.dto.CustomerBaseListResponse;
import com.smart.sso.server.service.*;
import com.smart.sso.server.util.CommonUtils;
import com.smart.sso.server.util.DateUtil;
import com.smart.sso.server.util.JsonUtil;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;

import static com.smart.sso.server.constant.AppConstant.SOURCEID_KEY_PREFIX;

@RestController
@Slf4j
public class CustomerController {

    @Autowired
    private CustomerInfoService customerInfoService;
    @Autowired
    private MessageService messageService;
    @Autowired
    private TelephoneRecordService recordService;
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    @Autowired
    private CustomerRelationService customerRelationService;
    @Autowired
    private ConfigService configService;

    @ApiOperation(value = "获取客户列表")
    @GetMapping("/customers")
    public BaseResponse<CustomerBaseListResponse> getCustomerList(@RequestParam(value = "offset", defaultValue = "1") Integer page,
                                                                  @RequestParam(value = "limit", defaultValue = "10") Integer limit,
                                                                  @RequestParam(value = "sort_by", defaultValue = "update_time") String sortBy,
                                                                  @RequestParam(value = "order", defaultValue = "desc") String order,
                                                                  @RequestParam(value = "name", required = false) String customerName,
                                                                  @RequestParam(value = "owner", required = false) String ownerName,
                                                                  @RequestParam(value = "conversion_rate", required = false) String conversionRate,
                                                                  @RequestParam(value = "activity_name", required = false) String  activityName) {
        CustomerInfoListRequest params = new CustomerInfoListRequest(page, limit, sortBy, order, customerName, ownerName, conversionRate, activityName);
        CustomerBaseListResponse commonPageList = customerInfoService.queryCustomerInfoList(params);
        return ResultUtils.success(commonPageList);
    }

    @ApiOperation(value = "获取客户基本信息")
    @GetMapping("/customer/{customer_id}/activity/{activity_id}/profile")
    public BaseResponse<CustomerProfile> getCustomerProfile(@PathVariable(value = "customer_id") String customerId,
                                                            @PathVariable(value = "activity_id") String activityId) {

        customerId = CommonUtils.decrypt(customerId);
        CustomerProfile customerProfile = customerInfoService.queryCustomerById(customerId, activityId);
        CustomerInfo customerInfo = customerRelationService.getByCustomer(customerId, customerProfile.getOwnerId());
        customerProfile.setAccessTime(customerInfo.getAccessTime());
        customerProfile.setTransactionCycle_2(customerInfo.getOrderCycle_2_0());
        return ResultUtils.success(customerProfile);
    }

    @ApiOperation(value = "获取客户特征信息")
    @GetMapping("/customer/{customer_id}/activity/{activity_id}/features")
    public BaseResponse<CustomerFeatureResponse> getCustomerFeatures(@PathVariable(value = "customer_id") String customerId,
                                                                     @PathVariable(value = "activity_id") String activityId) {
        customerId = CommonUtils.decrypt(customerId);
        CustomerFeatureResponse FeatureProfile = customerInfoService.queryCustomerFeatureById(customerId, activityId);
        return ResultUtils.success(FeatureProfile);
    }

    @ApiOperation(value = "获取客户参加的活动列表")
    @GetMapping("/customer/{customer_id}/activities")
    public BaseResponse<List<ActivityInfoWithVersion>> getCustomerActivityInfo(@PathVariable(value = "customer_id") String customerId) {
        customerId = CommonUtils.decrypt(customerId);
        List<ActivityInfoWithVersion> activityInfoList = customerInfoService.getActivityInfoByCustomerId(customerId);
        return ResultUtils.success(activityInfoList);
    }

    @ApiOperation(value = "修改客户特征信息")
    @PostMapping("/customer/{customer_id}/activity/{activity_id}/features")
    public BaseResponse<Void> modifyCustomerFeatures(@PathVariable(value = "customer_id") String customerId,
                                                                       @PathVariable(value = "activity_id") String activityId,
                                                                       @RequestBody CustomerFeatureResponse customerFeatureRequest) {
        customerId = CommonUtils.decrypt(customerId);
        customerInfoService.modifyCustomerFeatureById(customerId, activityId, customerFeatureRequest);
        return ResultUtils.success(null);
    }

    @ApiOperation(value = "客户识别回调")
    @PostMapping("/customer/callback")
    public BaseResponse<Void> callBack(@RequestBody CallBackRequest callBackRequest) {
        String sourceId = callBackRequest.getSourceId();
        String staffId = "";
        String customerId = "";
        if (Objects.nonNull(callBackRequest.getData()) && Objects.nonNull(callBackRequest.getData().getCall()) &&
                !StringUtils.isEmpty(callBackRequest.getData().getCall().getStaffId())) {
            customerId = callBackRequest.getData().getCall().getCustomerId();
            staffId = callBackRequest.getData().getCall().getStaffId();
            boolean needProcess = false;
            for (Set<String> item : AppConstant.staffIdMap.values()){
                if (item.contains(staffId)){
                    needProcess = true;
                    break;
                }
            }
            if (!needProcess) {
                log.error("staff id: {} 不参加活动， 跳过不处理, customer id: {}, source id: {}", staffId, customerId, sourceId);
                return ResultUtils.success(null);
            }
        }
        String redisKey = SOURCEID_KEY_PREFIX + sourceId;
        // 检查key是否存在于Redis中
        Boolean hasKey = redisTemplate.hasKey(redisKey);
        if (Boolean.TRUE.equals(hasKey)) {
            // key已经存在，说明已经处理过
            log.error("source id:{} 已存在， 跳过不处理, staff id: {}, customer id: {}", sourceId, staffId, customerId);
        } else {
            log.error("开始调用python脚本：source id:{}, staff id: {}, customer id: {}", sourceId, staffId, customerId);
            customerInfoService.callback(sourceId);/**/
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
                if (line.contains("2024-08-23")) {
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
                                       @RequestParam(value = "owner_id", required = false) String ownerId,
                                       @RequestParam(value = "owner", required = false) String owner,
                                       String from, String manager,
                                       HttpServletResponse response) throws IOException {
        customerId = CommonUtils.decrypt(customerId);
        String targetUrl = customerInfoService.getRedirectUrl(customerId, activityId, ownerId, owner, from, manager);
        // 使用HttpServletResponse进行重定向
        response.sendRedirect(CommonUtils.encodeParameters(targetUrl));
        response.setStatus(302);
        return ResultUtils.success(null);
    }

    @ApiOperation(value = "触发给用户发送通知")
    @GetMapping("/customer/send_message")
    public BaseResponse<Void> sendMessage(@RequestParam(value = "customer_id") String customerId, @RequestParam(value = "activity_id") String activityId) {
        log.error("触发客户的特征更新，id: " + customerId);
        try {
            messageService.updateCustomerCharacter(customerId, activityId, true);
        } catch (Exception e) {
            log.error("触发客户的特征更新失败",e);
            return ResultUtils.success(null);
        }
//        customerInfoService.updateCharacterCostTime(customerId);

        return ResultUtils.success(null);
    }

    @ApiOperation(value = "获取通话记录")
    @GetMapping("/customer/{customer_id}/activity/{activity_id}/chat_history")
    public BaseResponse<List<ChatHistoryVO>> getChatContent1(@PathVariable(value = "customer_id") String customerId,
                                                             @PathVariable(value = "activity_id") String activityId) {
        customerId = CommonUtils.decrypt(customerId);
        return ResultUtils.success(recordService.getChatHistory(customerId, activityId));
    }

    @ApiOperation(value = "获取单次通话")
    @GetMapping("/customer/{customer_id}/activity/{activity_id}/chat_detail")
    public BaseResponse<ChatDetail> getChatContent2(@PathVariable(value = "customer_id") String customerId,
                                                    @PathVariable(value = "activity_id") String activityId,
                                                    @RequestParam(value = "id") String callId) {
        customerId = CommonUtils.decrypt(customerId);
        return ResultUtils.success(recordService.getChatDetail(customerId, activityId, callId));
    }

    @ApiOperation(value = "全量初始化")
    @GetMapping("/customer/init_character")
    public BaseResponse<Void> initCharacter() {
        LocalDateTime dateTime = LocalDateTime.of(2024, 1, 1, 12, 0, 0);

        List<TelephoneRecordStatics> customerRecordList = recordService.getCustomerIdUpdate(dateTime);
        if (CollectionUtils.isEmpty(customerRecordList)) {
            return ResultUtils.success(null);
        }
        for (TelephoneRecordStatics item : customerRecordList) {
            try {
                messageService.updateCustomerCharacter(item.getCustomerId(), item.getActivityId(), false);
            } catch (Exception e) {
                log.error("更新CustomerCharacter失败：ID=" + item.getCustomerId(), e);
            }
        }
        return ResultUtils.success(null);
    }

    @ApiOperation(value = "统计数据")
    @PostMapping("/customer/statistics")
    public BaseResponse<Void> statistics() {
        customerInfoService.statistics();
        return ResultUtils.success(null);
    }

    @ApiOperation(value = "同步customer_info")
    @PostMapping("/customer/sync_customer_info")
    public BaseResponse<Void> syncCustomerInfo() {
        recordService.syncCustomerInfo();
        return ResultUtils.success(null);
    }

    @ApiOperation(value = "给业务员发送测试的信息（企微）")
    @PostMapping("/customer/test_send_message")
    public BaseResponse<Void> testSendMessage(@RequestBody Map<String, String> message) {
        messageService.sendTestMessageToSales(message);
        return ResultUtils.success(null);
    }

    @ApiOperation(value = "同步customer_info from 客户关系表")
    @PostMapping("/customer/sync_customer_info_from_relation")
    public BaseResponse<Void> syncCustomerInfoFromRelation() {
        customerInfoService.syncCustomerInfoFromRelation();
        return ResultUtils.success(null);
    }

    @ApiOperation(value = "给单个领导发送统计信息")
    @PostMapping("/customer/send_message_for_per_leader")
    public BaseResponse<Void> sendMessageForPerLeader(@RequestParam(value = "user_id") String userId) {
        return ResultUtils.success(null);
    }

    @ApiOperation(value = "企微的回调接口")
    @PostMapping("/customer/communication_sync/wecom")
    public BaseResponse<Void> communicationSyncWecom(@RequestBody Map<String, Object> message) {
        String userId = message.get("user_id").toString();
        String customerId = message.get("customer_id").toString();
        log.error("收到企微回调，user id: {}, customer id: {}", userId, customerId);
        String dateTimeStr = DateUtil.getCurrentDateTime();
        String dateStr = dateTimeStr.split(" ")[0];
        String timeStr = dateTimeStr.split(" ")[1].replace(":", "_");
        // 生成一个5位的随机数
        Random random = new Random();
        int randomComponent = 100 + random.nextInt(900);
        String filePath = "/data/customer-convert/callback/wecom/" + dateStr + "/" + userId + "_" + customerId + "_" + timeStr + "-" + randomComponent;
        log.error("存储路径:{}", filePath);
        CommonUtils.appendTextToFile(filePath, JsonUtil.serialize(message));
        return ResultUtils.success(null);
    }

    @ApiOperation(value = "电话的回调接口")
    @PostMapping("/customer/communication_sync/telephone")
    public BaseResponse<Void> communicationSyncTelephone(@RequestBody Map<String, Object> message) {
        log.error("收到语音回调，message: {}", JsonUtil.serialize(message));
        String dateTimeStr = DateUtil.getCurrentDateTime();
        String dateStr = dateTimeStr.split(" ")[0];
        String filePath = "/data/customer-convert/callback/telephone/" + dateStr + "_message.txt";
        CommonUtils.appendTextToFile(filePath, JsonUtil.serialize(message));
        return ResultUtils.success(null);
    }

    @ApiOperation(value = "新增配置项")
    @PostMapping("/customer/add_config")
    public BaseResponse<Void> addConfig(@RequestBody Config config) {
        configService.addConfig(config);
        return ResultUtils.success(null);
    }

    @ApiOperation(value = "修改配置项")
    @PostMapping("/customer/modify_config")
    public BaseResponse<Void> modifyConfig(@RequestBody Config config) {
        configService.modifyConfig(config);
        return ResultUtils.success(null);
    }



}
