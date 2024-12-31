package com.smart.sso.server.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.smart.sso.server.common.BaseResponse;
import com.smart.sso.server.common.ResultUtils;
import com.smart.sso.server.constant.AppConstant;
import com.smart.sso.server.model.ActivityInfoWithVersion;
import com.smart.sso.server.model.TelephoneRecordStatics;
import com.smart.sso.server.model.VO.ChatDetail;
import com.smart.sso.server.model.VO.ChatHistoryVO;
import com.smart.sso.server.model.VO.CustomerProfile;
import com.smart.sso.server.model.dto.CallBackRequest;
import com.smart.sso.server.model.dto.CustomerFeatureResponse;
import com.smart.sso.server.model.dto.CustomerInfoListRequest;
import com.smart.sso.server.model.dto.CustomerBaseListResponse;
import com.smart.sso.server.model.dto.CustomerProcessSummary;
import com.smart.sso.server.service.CustomerInfoService;
import com.smart.sso.server.service.EventService;
import com.smart.sso.server.service.MessageService;
import com.smart.sso.server.service.TelephoneRecordService;
import com.smart.sso.server.util.CommonUtils;
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
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

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
    private EventService eventService;

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
        CustomerProfile customerProfile = customerInfoService.queryCustomerById(customerId, activityId);
        return ResultUtils.success(customerProfile);
    }

    @ApiOperation(value = "获取客户特征信息")
    @GetMapping("/customer/{customer_id}/activity/{activity_id}/features")
    public BaseResponse<CustomerFeatureResponse> getCustomerFeatures(@PathVariable(value = "customer_id") String customerId,
                                                                     @PathVariable(value = "activity_id") String activityId) {
        String tttt = "{\"warmth\":{\"delivery_course\":{\"total\":12,\"process\":5,\"records\":{\"columns\":[{\"key\":\"client\",\"label\":\"客户端\"},{\"key\":\"event_type\",\"label\":\"行为类型\"},{\"key\":\"event_time\",\"label\":\"访问时间\"},{\"key\":\"action_content\",\"label\":\"访问内容\"},{\"key\":\"action_section\",\"label\":\"访问栏目\"},{\"key\":\"event_duration\",\"label\":\"访问时长\"}],\"data\":[{\"client\":\"app\",\"event_type\":\"浏览\",\"event_time\":\"2014-12-25 21:45:23\",\"action_content\":\"股票\",\"action_section\":\"量化模式\",\"event_duration\":\"1min54s\"}]}},\"funds_volume\":{\"value\":\"less_five_w\",\"origin_chat\":{\"id\":\"2024-11-29/xiaoweixing_潘海燕_9cf25810-6e8c-4195-ba4a-5758d33109e0.txt\",\"contents\":[{\"role\":\"潘海燕\",\"time\":\"2024-11-29 14:23:26\",\"content\":\"又想挣点生活费，你看还还把钱买了一个票还套住了，到现在唉呦天哪的我买了1万1万6000块钱本钱，一推推好几千，剩了1万块钱了\"}]}},\"marketing_course\":{\"total\":12,\"process\":5,\"records\":{\"columns\":[{\"key\":\"client\",\"label\":\"客户端\"},{\"key\":\"event_type\",\"label\":\"行为类型\"},{\"key\":\"event_time\",\"label\":\"访问时间\"},{\"key\":\"action_content\",\"label\":\"访问内容\"},{\"key\":\"action_section\",\"label\":\"访问栏目\"},{\"key\":\"event_duration\",\"label\":\"访问时长\"}],\"data\":[{\"client\":\"app\",\"event_type\":\"浏览\",\"event_time\":\"2014-12-25 21:45:23\",\"action_content\":\"股票\",\"action_section\":\"量化模式\",\"event_duration\":\"1min54s\"}]}},\"visit_freq\":{\"value\":\"low\",\"records\":{\"columns\":[{\"key\":\"client\",\"label\":\"客户端\"},{\"key\":\"event_type\",\"label\":\"行为类型\"},{\"key\":\"event_time\",\"label\":\"访问时间\"},{\"key\":\"action_content\",\"label\":\"访问内容\"},{\"key\":\"action_section\",\"label\":\"访问栏目\"},{\"key\":\"event_duration\",\"label\":\"访问时长\"}],\"data\":[{\"client\":\"app\",\"event_type\":\"浏览\",\"event_time\":\"2014-12-25 21:45:23\",\"action_content\":\"股票\",\"action_section\":\"量化模式\",\"event_duration\":\"1min54s\"}]}},\"function_freq\":{\"value\":\"low\",\"records\":{\"columns\":[{\"key\":\"client\",\"label\":\"客户端\"},{\"key\":\"event_type\",\"label\":\"行为类型\"},{\"key\":\"event_time\",\"label\":\"访问时间\"},{\"key\":\"action_content\",\"label\":\"访问内容\"},{\"key\":\"action_section\",\"label\":\"访问栏目\"},{\"key\":\"event_duration\",\"label\":\"访问时长\"}],\"data\":[{\"client\":\"app\",\"event_type\":\"浏览\",\"event_time\":\"2014-12-25 21:45:23\",\"action_content\":\"股票\",\"action_section\":\"量化模式\",\"event_duration\":\"1min54s\"}]}},\"customer_course\":{\"value\":\"abundant\",\"origin_chat\":{\"id\":\"2024-11-29/xiaoweixing_潘海燕_9cf25810-6e8c-4195-ba4a-5758d33109e0.txt\",\"contents\":[{\"role\":\"潘海燕\",\"time\":\"2024-11-29 14:23:26\",\"content\":\"又想挣点生活费，你看还还把钱买了一个票还套住了，到现在唉呦天哪的我买了1万1万6000块钱本钱，一推推好几千，剩了1万块钱了\"}]}}},\"handover_period\":{\"basic\":{\"complete_intro\":{\"value\":true,\"records\":{\"columns\":[{\"key\":\"client\",\"label\":\"客户端\"},{\"key\":\"event_type\",\"label\":\"行为类型\"},{\"key\":\"event_time\",\"label\":\"访问时间\"},{\"key\":\"action_content\",\"label\":\"访问内容\"},{\"key\":\"action_section\",\"label\":\"访问栏目\"},{\"key\":\"event_duration\",\"label\":\"访问时长\"}],\"data\":[{\"client\":\"app\",\"event_type\":\"浏览\",\"event_time\":\"2014-12-25 21:45:23\",\"action_content\":\"股票\",\"action_section\":\"量化模式\",\"event_duration\":\"1min54s\"}]}},\"remind_freq\":{\"value\":\"high\",\"records\":{\"columns\":[{\"key\":\"client\",\"label\":\"客户端\"},{\"key\":\"event_type\",\"label\":\"行为类型\"},{\"key\":\"event_time\",\"label\":\"访问时间\"},{\"key\":\"action_content\",\"label\":\"访问内容\"},{\"key\":\"action_section\",\"label\":\"访问栏目\"},{\"key\":\"event_duration\",\"label\":\"访问时长\"}],\"data\":[{\"client\":\"app\",\"event_type\":\"浏览\",\"event_time\":\"2014-12-25 21:45:23\",\"action_content\":\"股票\",\"action_section\":\"量化模式\",\"event_duration\":\"1min54s\"}]}},\"trans_freq\":{\"value\":\"high\",\"records\":{\"columns\":[{\"key\":\"client\",\"label\":\"客户端\"},{\"key\":\"event_type\",\"label\":\"行为类型\"},{\"key\":\"event_time\",\"label\":\"访问时间\"},{\"key\":\"action_content\",\"label\":\"访问内容\"},{\"key\":\"action_section\",\"label\":\"访问栏目\"},{\"key\":\"event_duration\",\"label\":\"访问时长\"}],\"data\":[{\"client\":\"app\",\"event_type\":\"浏览\",\"event_time\":\"2014-12-25 21:45:23\",\"action_content\":\"股票\",\"action_section\":\"量化模式\",\"event_duration\":\"1min54s\"}]}}},\"current_stocks\":{\"inquired\":\"yes\",\"inquired_origin_chat\":{\"id\":\"2024-12-20/xiaoweixing_潘海燕_01575db7-99e7-4337-be2e-18ffd26c72c5.txt\",\"contents\":[{\"role\":\"肖惟兴\",\"time\":\"2024-12-20 10:54:11\",\"content\":\"好，打开了是吧？\"},{\"role\":\"潘海燕\",\"time\":\"2024-12-20 10:54:14\",\"content\":\"打开了。\"},{\"role\":\"潘海燕\",\"time\":\"2024-12-20 10:54:19\",\"content\":\"嗯\"},{\"role\":\"肖惟兴\",\"time\":\"2024-12-20 10:54:21\",\"content\":\"好行可以行，打开了可以的，打开了之后，然后的话呢我们这个现在怎么样呢非常简单，啊简单你这个手机外打开了之后打开了之后，然后的话姐那你这个你现在做什么股票，你说一下我帮你结合人要我们看你的手里的股票，你做什么股票姐。\"}]},\"customer_conclusion\":{\"model_record\":\"潘海燕 2024-12-20 10:55:29\\n你一个是TCL科技，\\n肖惟兴 2024-12-20 10:55:30\\nT4l科技是吧？\\n潘海燕 2024-12-20 10:55:34\\n0啊00嗯300100。\",\"sales_manual_tag\":null,\"sales_record\":null,\"update_time\":null,\"compare_value\":\"潘海燕 2024-12-20 10:55:29\\n你一个是TCL科技，\\n肖惟兴 2024-12-20 10:55:30\\nT4l科技是吧？\\n潘海燕 2024-12-20 10:55:34\\n0啊00嗯300100。\",\"origin_chat\":{\"id\":\"2024-12-20/xiaoweixing_潘海燕_01575db7-99e7-4337-be2e-18ffd26c72c5.txt\",\"contents\":[{\"role\":\"潘海燕\",\"time\":\"2024-12-20 10:55:29\",\"content\":\"你一个是TCL科技，\"},{\"role\":\"肖惟兴\",\"time\":\"2024-12-20 10:55:30\",\"content\":\"T4l科技是吧？\"},{\"role\":\"潘海燕\",\"time\":\"2024-12-20 10:55:34\",\"content\":\"0啊00嗯300100。\"}]}},\"standard_action\":{\"result\":true,\"origin_chat\":{\"id\":\"2024-12-20/xiaoweixing_潘海燕_01575db7-99e7-4337-be2e-18ffd26c72c5.txt\",\"contents\":[{\"role\":\"肖惟兴\",\"time\":\"2024-12-20 10:55:46\",\"content\":\"噢今天这个跌了一点是吧？\"},{\"role\":\"潘海燕\",\"time\":\"2024-12-20 10:55:48\",\"content\":\"嗯\"},{\"role\":\"肖惟兴\",\"time\":\"2024-12-20 10:55:49\",\"content\":\"啊你这个股票的话买了多少万啊姐？\"}]}}},\"trading_style\":{\"inquired\":\"no\",\"inquired_origin_chat\":null,\"customer_conclusion\":{\"model_record\":null,\"sales_manual_tag\":null,\"sales_record\":null,\"update_time\":null,\"compare_value\":null,\"origin_chat\":null},\"standard_action\":{\"result\":false,\"origin_chat\":null}},\"stock_market_age\":{\"inquired\":\"yes\",\"inquired_origin_chat\":{\"id\":\"2024-12-06/xiaoweixing_潘海燕_faa191c9-7fd0-42b1-a69b-4026a0f8ac48.txt\",\"contents\":[{\"role\":\"肖惟兴\",\"time\":\"2024-12-06 10:57:00\",\"content\":\"好，那那那那那姐，那我问你一个最简单，你用起留的方法买了什么股票啊姐？\"}]},\"customer_conclusion\":{\"model_record\":\"潘海燕 2024-12-06 10:57:10\\n启名的课还没学完，呢就教一些什么？\",\"sales_manual_tag\":null,\"sales_record\":null,\"update_time\":null,\"compare_value\":\"潘海燕 2024-12-06 10:57:10\\n启名的课还没学完，呢就教一些什么？\",\"origin_chat\":{\"id\":\"2024-12-06/xiaoweixing_潘海燕_faa191c9-7fd0-42b1-a69b-4026a0f8ac48.txt\",\"contents\":[{\"role\":\"潘海燕\",\"time\":\"2024-12-06 10:57:10\",\"content\":\"启名的课还没学完，呢就教一些什么？\"}]}},\"standard_action\":null}}}";
        CustomerFeatureResponse test = JsonUtil.readValue(tttt, new TypeReference<CustomerFeatureResponse>() {
        });
        return ResultUtils.success(test);
    }

    @ApiOperation(value = "获取客户参加的活动列表")
    @GetMapping("/customer/{customer_id}/activities")
    public BaseResponse<List<ActivityInfoWithVersion>> getCustomerActivityInfo(@PathVariable(value = "customer_id") String customerId) {
        List<ActivityInfoWithVersion> activityInfoList = customerInfoService.getActivityInfoByCustomerId(customerId);
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
                                       String from, String manager,
                                       HttpServletResponse response) throws IOException {
        String targetUrl = customerInfoService.getRedirectUrl(customerId, activityId, from, manager);
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
        messageService.sendMessageForPerLeader(userId);
        return ResultUtils.success(null);
    }

    @ApiOperation(value = "企微的回调接口")
    @PostMapping("/customer/communication_sync/wecom")
    public BaseResponse<Void> communicationSyncWecom(@RequestBody Map<String, Object> message) {
        String filePath = "/data/customer-convert/callback/wecom/message.txt";
        CommonUtils.appendTextToFile(filePath, JsonUtil.serialize(message));
        return ResultUtils.success(null);
    }

    @ApiOperation(value = "电话的回调接口")
    @PostMapping("/customer/communication_sync/telephone")
    public BaseResponse<Void> communicationSyncTelephone(@RequestBody Map<String, Object> message) {
        String filePath = "/data/customer-convert/callback/telephone/message.txt";
        CommonUtils.appendTextToFile(filePath, JsonUtil.serialize(message));
        return ResultUtils.success(null);
    }

}
