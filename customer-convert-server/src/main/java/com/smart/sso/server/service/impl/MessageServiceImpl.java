package com.smart.sso.server.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.google.common.collect.ImmutableMap;
import com.smart.sso.server.constant.AppConstant;
import com.smart.sso.server.enums.FundsVolumeEnum;
import com.smart.sso.server.enums.StockPositonEnum;
import com.smart.sso.server.enums.TradingStyleEnum;
import com.smart.sso.server.primary.mapper.CustomerCharacterMapper;
import com.smart.sso.server.primary.mapper.CustomerBaseMapper;
import com.smart.sso.server.model.*;
import com.smart.sso.server.model.VO.CustomerProfile;
import com.smart.sso.server.model.dto.CustomerFeatureResponse;
import com.smart.sso.server.service.ConfigService;
import com.smart.sso.server.service.CustomerInfoService;
import com.smart.sso.server.service.MessageService;
import com.smart.sso.server.service.TelephoneRecordService;
import com.smart.sso.server.util.CommonUtils;
import com.smart.sso.server.util.JsonUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpHeaders;

import java.lang.reflect.Field;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

import static com.smart.sso.server.constant.AppConstant.CUSTOMER_DASHBOARD_URL;


@Service
@Slf4j
public class MessageServiceImpl implements MessageService {

    @Autowired
    private RestTemplate restTemplate;
    @Autowired
    private CustomerInfoService customerInfoService;
    @Autowired
    private ConfigService configService;
    @Autowired
    private CustomerCharacterMapper customerCharacterMapper;
    @Autowired
    private CustomerBaseMapper customerBaseMapper;
    @Autowired
    private TelephoneRecordService recordService;

    ImmutableMap<String, String> conversionRateMap = ImmutableMap.<String, String>builder().put("incomplete", "未完成判断").put("low", "较低").put("medium", "中等").put("high", "较高").build();

    @Override
    public String sendMessageToChat(TextMessage message) {
        // 是否是通过企微机器人发送
        if (AppConstant.robotUrl.containsKey(message.getTouser())){
            return sendMessageToChat(AppConstant.robotUrl.get(message.getTouser()), message);
        }
        // 创建请求头
        HttpHeaders headers = new HttpHeaders();

        headers.set("Content-Type", "application/json");
        log.error("发送消息内容：" + JsonUtil.serialize(message));
        // 创建请求实体
        HttpEntity<TextMessage> requestEntity = new HttpEntity<>(message, headers);
        String url = String.format(AppConstant.SEND_APPLICATION_MESSAGE_URL, getAccessToken(message.getTouser()));
        // 发送 POST 请求
//        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, requestEntity, String.class);
//        // 处理响应
//        if (response.getStatusCode() == HttpStatus.OK) {
//            log.error("发送消息结果：" + response.getBody());
//            Map<String, Object> StringMap = JsonUtil.readValue(response.getBody(), new TypeReference<Map<String, Object>>() {
//            });
//            if (!StringMap.get("errcode").toString().equals("0")) {
//                configService.refreshCustomerConfig();
//                url = String.format(AppConstant.SEND_APPLICATION_MESSAGE_URL, getAccessToken(message.getTouser()));
//                restTemplate.exchange(url, HttpMethod.POST, requestEntity, String.class);
//            }
//            return response.getBody();
//        } else {
//            log.error("Failed to send message: " + response.getStatusCode());
//            throw new RuntimeException("Failed to send message: " + response.getStatusCode());
//        }
        return null;
    }

    @Override
    public String sendMessageToChat(String url, TextMessage message) {
        // 创建请求头
        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/json");
        log.error("发送消息内容：" + JsonUtil.serialize(message) + url);
        // 创建请求实体
        HttpEntity<TextMessage> requestEntity = new HttpEntity<>(message, headers);
        // 发送 POST 请求
//        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, requestEntity, String.class);
//        // 处理响应
//        if (response.getStatusCode() == HttpStatus.OK) {
//            log.error("发送消息结果：" + response.getBody());
//            return response.getBody();
//        } else {
//            log.error("Failed to send message: " + response.getStatusCode());
//            throw new RuntimeException("Failed to send message: " + response.getStatusCode());
//        }
        return null;
    }


    @Override
    public void sendPurchaseAttitudeSummary(String activityId) {
        QueryWrapper<CustomerCharacter> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("activity_id", activityId);
        List<CustomerCharacter> characterList = customerCharacterMapper.selectList(queryWrapper);
        Map<String, PotentialCustomer> potentialCustomerMap = new HashMap<>();
        for (CustomerCharacter character : characterList) {
            // 完成购买，跳过不统计
            if (character.getCompletePurchaseStage()) {
                continue;
            }
            String ownerId = character.getOwnerId();
            PotentialCustomer potentialCustomer;
            if (!potentialCustomerMap.containsKey(ownerId)) {
                potentialCustomer = new PotentialCustomer();
            } else {
                potentialCustomer = potentialCustomerMap.get(ownerId);
            }
        }
        // 给每个业务员发送统计消息
        for (Map.Entry<String, PotentialCustomer> entry : potentialCustomerMap.entrySet()) {
            String message = String.format(AppConstant.PURCHASE_ATTITUDE_SUMMARY_TEMPLATE,
                    CommonUtils.convertStringFromList(entry.getValue().getHigh()),
                    CommonUtils.convertStringFromList(entry.getValue().getMiddle()));
            TextMessage textMessage = new TextMessage();
            TextMessage.TextContent textContent = new TextMessage.TextContent();
            textMessage.setTouser(entry.getKey());
            textMessage.setAgentid(getAgentId(entry.getKey()));
            textContent.setContent(message);
            textMessage.setMsgtype("markdown");
            textMessage.setMarkdown(textContent);
            sendMessageToChat(textMessage);
        }
    }

    @Override
    public void updateCustomerCharacter(String customerId, String activityId, boolean checkPurchaseAttitude) {
        CustomerBase customerBase = customerBaseMapper.selectByCustomerIdAndCampaignId(customerId, activityId);
        // info 表不存在，先触发同步
        if (Objects.isNull(customerBase)) {
            customerInfoService.queryCustomerById(customerId, activityId);
            customerBase = customerBaseMapper.selectByCustomerIdAndCampaignId(customerId, activityId);
        }
        CustomerProfile customerProfile = customerInfoService.queryCustomerById(customerBase.getCustomerId(), customerBase.getActivityId());
        CustomerFeatureResponse customerFeature = customerInfoService.queryCustomerFeatureById(customerBase.getCustomerId(), customerBase.getActivityId());

        CustomerCharacter customerCharacter = customerCharacterMapper.selectByCustomerIdAndActivityId(customerId, activityId);
        CustomerCharacter newCustomerCharacter = new CustomerCharacter();
        updateCharacter(newCustomerCharacter, customerBase, customerProfile, customerFeature);
        if (Objects.isNull(customerCharacter)) {
            // 新建
            customerCharacterMapper.insert(newCustomerCharacter);
        } else {
            // 更新
            if (!areEqual(customerCharacter, newCustomerCharacter)) {
                customerCharacterMapper.updateAllFields(newCustomerCharacter);
            }
        }
    }

    @Override
    public String getAccessToken(String userId) {
        for (Map.Entry<String, Set<String>> entry : AppConstant.staffIdMap.entrySet()) {
            if (entry.getValue().contains(userId)) {
                return AppConstant.accessTokenMap.get(entry.getKey());
            }
        }
        return null;
    }

    @Override
    public String getAgentId(String userId) {
        for (Map.Entry<String, Set<String>> entry : AppConstant.staffIdMap.entrySet()) {
            if (entry.getValue().contains(userId)) {
                return AppConstant.qiweiApplicationConfigMap.get(entry.getKey()).getAgentId();
            }
        }
        return null;
    }

    @Override
    public void sendTestMessageToSales(Map<String, String> message) {
        String userId = message.get("userId");
        String content = message.get("content");
        List<String> staffIds = Arrays.stream(userId.split(",")).map(item -> item.trim()).collect(Collectors.toList());
        for (String item : staffIds) {
            TextMessage textMessage = new TextMessage();
            TextMessage.TextContent textContent = new TextMessage.TextContent();
            textContent.setContent(content);
            textMessage.setAgentid(getAgentId(item));
            textMessage.setTouser(item);
            textMessage.setMsgtype("markdown");
            textMessage.setMarkdown(textContent);
            sendMessageToChat(textMessage);
        }
    }

    private boolean nightTime(){
        LocalTime now = LocalTime.now();
        LocalTime start = LocalTime.of(22, 0);
        LocalTime end = LocalTime.of(8, 0).plusHours(24); // 直接加24小时来包含次日的8点
        return now.isAfter(start) && now.isBefore(end);
    }

    public boolean areEqual(CustomerCharacter cc1, CustomerCharacter cc2) {
        if (cc1 == cc2) {
            return true;
        }
        if (cc1 == null || cc2 == null) {
            return false;
        }

        // 获取CustomerCharacter类的所有字段
        Field[] fields = CustomerCharacter.class.getDeclaredFields();

        for (Field field : fields) {
            // 跳过 createTime 和 updateTime 字段
            if ("createTime".equals(field.getName()) || "updateTimeTelephone".equals(field.getName())) {
                continue;
            }
            field.setAccessible(true);
            try {
                // 获取两个对象的字段值
                Object value1 = field.get(cc1);
                Object value2 = field.get(cc2);
                if (!Objects.equals(value1, value2)) { // 比较字段值，如果不相等，返回 false
                    return false;
                }
            } catch (IllegalAccessException e) {
                e.printStackTrace();
                return false;
            }
        }
        return true;
    }

    private void sendMessage(String id) {
        CustomerBase customerBase = customerBaseMapper.selectById(id);
        CustomerFeatureResponse featureResponse = customerInfoService.queryCustomerFeatureById(customerBase.getCustomerId(), customerBase.getActivityId());
        List<String> completeStatus = featureResponse.getSummary().getAdvantage();
        List<CustomerFeatureResponse.Question> incompleteStatus = featureResponse.getSummary().getQuestions();

        // 优点是空，不发送
        if (CollectionUtils.isEmpty(completeStatus)) {
            return;
        }
        StringBuilder complete = new StringBuilder();
        StringBuilder incomplete = new StringBuilder();
        int i = 1;
        for (String item : completeStatus) {
            complete.append(i++).append(". ").append(item).append("\n");
        }
        if (complete.length() < 1) {
            complete.append("暂无");
        }
        i = 1;
        for (CustomerFeatureResponse.Question item : incompleteStatus) {
            incomplete.append(i++).append(". ").append(item.getMessage()).append("\n");
        }
        if (incomplete.length() < 1) {
            incomplete.append("暂无");
        }
        String rateDesc;
        if (customerBase.getConversionRate().equals("low")) {
            rateDesc = conversionRateMap.get(customerBase.getConversionRate()) + "（不应重点跟进）";
        } else {
            rateDesc = conversionRateMap.get(customerBase.getConversionRate());
        }
        String message = String.format(AppConstant.CUSTOMER_SUMMARY_MARKDOWN_TEMPLATE, customerBase.getCustomerName(),
                customerBase.getCustomerId(),
                rateDesc,
                complete,
                incomplete,
                CUSTOMER_DASHBOARD_URL, CUSTOMER_DASHBOARD_URL);
        TextMessage textMessage = new TextMessage();
        TextMessage.TextContent textContent = new TextMessage.TextContent();
        textContent.setContent(message);
        textMessage.setMsgtype("markdown");
        textMessage.setMarkdown(textContent);
        sendMessageToChat(textMessage);
    }

    private void updateCharacter(CustomerCharacter latestCustomerCharacter, CustomerBase customerBase,
                                 CustomerProfile customerProfile, CustomerFeatureResponse customerFeature) {
        latestCustomerCharacter.setId(customerBase.getId());
        latestCustomerCharacter.setCustomerId(customerBase.getCustomerId());
        latestCustomerCharacter.setCustomerName(customerBase.getCustomerName());
        latestCustomerCharacter.setOwnerId(customerBase.getOwnerId());
        latestCustomerCharacter.setOwnerName(customerBase.getOwnerName());
        latestCustomerCharacter.setActivityName(customerBase.getActivityName());
        latestCustomerCharacter.setActivityId(customerBase.getActivityId());
        latestCustomerCharacter.setConversionRate(conversionRateMap.get(customerProfile.getConversionRate()));

        latestCustomerCharacter.setMatchingJudgmentStage(customerProfile.getCustomerStage().getMatchingJudgment() == 1);
        latestCustomerCharacter.setTransactionStyleStage(customerProfile.getCustomerStage().getTransactionStyle() == 1);
        latestCustomerCharacter.setFunctionIntroductionStage(customerProfile.getCustomerStage().getFunctionIntroduction() == 1);
        latestCustomerCharacter.setConfirmValueStage(customerProfile.getCustomerStage().getConfirmValue() == 1);
        latestCustomerCharacter.setConfirmPurchaseStage(customerProfile.getCustomerStage().getConfirmPurchase() == 1);
        latestCustomerCharacter.setCompletePurchaseStage(customerProfile.getCustomerStage().getCompletePurchase() == 1);

        latestCustomerCharacter.setClassAttendTimes(customerFeature.getWarmth().getClassAttendTimes());
        latestCustomerCharacter.setClassAttendDuration(customerFeature.getWarmth().getClassAttendDuration());
        latestCustomerCharacter.setCustomerResponse(customerFeature.getWarmth().getCustomerResponse());
        latestCustomerCharacter.setFundsVolume(FundsVolumeEnum.getTextByValue(
                Objects.nonNull(customerFeature.getWarmth().getFundsVolume().getCompareValue()) ? customerFeature.getWarmth().getFundsVolume().getCompareValue().toString() : null));
        latestCustomerCharacter.setStockPosition(StockPositonEnum.getTextByValue(
                Objects.nonNull(customerFeature.getWarmth().getStockPosition().getCompareValue()) ? customerFeature.getWarmth().getStockPosition().getCompareValue().toString() : null));
        latestCustomerCharacter.setTradingStyle(TradingStyleEnum.getTextByValue(
                Objects.nonNull(customerFeature.getWarmth().getTradingStyle().getCompareValue()) ? customerFeature.getWarmth().getTradingStyle().getCompareValue().toString() : null));
        latestCustomerCharacter.setPurchaseSimilarProduct(Objects.nonNull(customerFeature.getWarmth().getPurchaseSimilarProduct().getCompareValue()) ? customerFeature.getWarmth().getPurchaseSimilarProduct().getCompareValue().toString() : null);

        latestCustomerCharacter.setMemberStocksBuy(Objects.nonNull(customerFeature.getBasic().getMemberStocksBuy().getCustomerConclusion().getCompareValue()) ? customerFeature.getBasic().getMemberStocksBuy().getCustomerConclusion().getCompareValue().toString() : null);
        latestCustomerCharacter.setMemberStocksBuy(Objects.nonNull(customerFeature.getBasic().getMemberStocksPrice().getCustomerConclusion().getCompareValue()) ? customerFeature.getBasic().getMemberStocksPrice().getCustomerConclusion().getCompareValue().toString() : null);
        latestCustomerCharacter.setWelfareStocksBuy(Objects.nonNull(customerFeature.getBasic().getWelfareStocksBuy().getCustomerConclusion().getCompareValue()) ? customerFeature.getBasic().getWelfareStocksBuy().getCustomerConclusion().getCompareValue().toString() : null);
        latestCustomerCharacter.setWelfareStocksPrice(Objects.nonNull(customerFeature.getBasic().getWelfareStocksPrice().getCustomerConclusion().getCompareValue()) ? customerFeature.getBasic().getWelfareStocksPrice().getCustomerConclusion().getCompareValue().toString() : null);

        latestCustomerCharacter.setTeacherApprove(Objects.nonNull(customerFeature.getBasic().getTeacherApproval().getCustomerConclusion().getCompareValue()) ? customerFeature.getBasic().getTeacherApproval().getCustomerConclusion().getCompareValue().toString() : null);
        latestCustomerCharacter.setTeacherProfession(Objects.nonNull(customerFeature.getBasic().getTeacherApproval().getTeacherProfession()) ? customerFeature.getBasic().getTeacherApproval().getTeacherProfession().toString() : null);
        latestCustomerCharacter.setUpdateTime(customerProfile.getLastCommunicationDate());
        latestCustomerCharacter.setCreateTime(customerProfile.getAccessTime());
    }
}
