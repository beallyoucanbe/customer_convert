package com.smart.sso.server.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.collect.ImmutableMap;
import com.smart.sso.server.constant.AppConstant;
import com.smart.sso.server.enums.EarningDesireEnum;
import com.smart.sso.server.enums.FundsVolumeEnum;
import com.smart.sso.server.model.VO.MessageSendVO;
import com.smart.sso.server.primary.mapper.CustomerCharacterMapper;
import com.smart.sso.server.primary.mapper.CustomerInfoMapper;
import com.smart.sso.server.model.*;
import com.smart.sso.server.model.VO.CustomerProfile;
import com.smart.sso.server.model.dto.CustomerFeatureResponse;
import com.smart.sso.server.service.ConfigService;
import com.smart.sso.server.service.CustomerInfoService;
import com.smart.sso.server.service.MessageService;
import com.smart.sso.server.service.TelephoneRecordService;
import com.smart.sso.server.util.CommonUtils;
import com.smart.sso.server.util.DateUtil;
import com.smart.sso.server.util.JsonUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpHeaders;

import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.smart.sso.server.constant.AppConstant.*;


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
    private CustomerInfoMapper customerInfoMapper;
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
        return null;
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
        return null;
//        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, requestEntity, String.class);
//        // 处理响应
//        if (response.getStatusCode() == HttpStatus.OK) {
//            log.error("发送消息结果：" + response.getBody());
//            return response.getBody();
//        } else {
//            log.error("Failed to send message: " + response.getStatusCode());
//            throw new RuntimeException("Failed to send message: " + response.getStatusCode());
//        }
    }


    @Override
    public void sendPurchaseAttitudeSummary(String activityId) {
        QueryWrapper<CustomerCharacter> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("activity_id", activityId);
        List<CustomerCharacter> characterList = customerCharacterMapper.selectList(queryWrapper);
        List<CustomerInfo> customerInfoLongTimeNoSee = customerInfoService.getCustomerInfoLongTimeNoSee(activityId);
        Map<String, CustomerInfo> customerInfoLongTimeNoSeeMap = customerInfoLongTimeNoSee.stream()
                .collect(Collectors.toMap(CustomerInfo::getCustomerId, Function.identity()));
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
                potentialCustomerMap.put(ownerId, potentialCustomer);
            } else {
                potentialCustomer = potentialCustomerMap.get(ownerId);
            }
            // 判断 认可度次数
            int approvalCount = 0;
            if (Boolean.parseBoolean(character.getSoftwareFunctionClarity())) {
                approvalCount++;
            }
            if (Boolean.parseBoolean(character.getStockSelectionMethod())) {
                approvalCount++;
            }
            if (Boolean.parseBoolean(character.getSelfIssueRecognition())) {
                approvalCount++;
            }
            if (Boolean.parseBoolean(character.getSoftwareValueApproval())) {
                approvalCount++;
            }
            // 资金量≥5万且认可数≥3，购买态度为确认购买
            if (approvalCount >= 3 && Boolean.parseBoolean(character.getSoftwarePurchaseAttitude()) &&
                    !StringUtils.isEmpty(character.getFundsVolume()) &&
                    (character.getFundsVolume().equals("大于10万")||character.getFundsVolume().equals("5到10万之间"))) {
                potentialCustomer.getHigh().add(character.getOwnerName() + "，" + character.getCustomerName() + "，" + character.getCustomerId());
            } else if (approvalCount >= 3 && !Boolean.parseBoolean(character.getSoftwarePurchaseAttitude()) &&
                    !StringUtils.isEmpty(character.getFundsVolume()) &&
                    (character.getFundsVolume().equals("大于10万")||character.getFundsVolume().equals("5到10万之间"))) { // 资金量≥5且认可数≥3，购买态度为尚未确认购买
                potentialCustomer.getMiddle().add(character.getOwnerName() + "，" + character.getCustomerName() + "，" + character.getCustomerId());
            } else if (approvalCount <= 2 &&
                    !StringUtils.isEmpty(character.getFundsVolume()) &&
                    (character.getFundsVolume().equals("大于10万")||character.getFundsVolume().equals("5到10万之间"))) { // 资金量≥5万且认可数≤2
                potentialCustomer.getLow().add(character.getOwnerName() + "，" + character.getCustomerName() + "，" + character.getCustomerId());
            }
            if (!StringUtils.isEmpty(character.getFundsVolume()) &&
                    (character.getFundsVolume().equals("大于10万")||character.getFundsVolume().equals("5到10万之间"))
                    && customerInfoLongTimeNoSeeMap.containsKey(character.getCustomerId())) {
                potentialCustomer.getLongTimeNoSee().add(character.getOwnerName() + "，" + character.getCustomerName() + "，" + character.getCustomerId());
            }
        }
        // 给每个业务员发送统计消息
        for (Map.Entry<String, PotentialCustomer> entry : potentialCustomerMap.entrySet()) {
            String message = String.format(AppConstant.PURCHASE_ATTITUDE_SUMMARY_TEMPLATE,
                    CommonUtils.convertStringFromList(entry.getValue().getHigh()),
                    CommonUtils.convertStringFromList(entry.getValue().getMiddle()),
                    CommonUtils.convertStringFromList(entry.getValue().getLongTimeNoSee()));
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
        CustomerInfo customerInfo = customerInfoMapper.selectByCustomerIdAndCampaignId(customerId, activityId);
        // info 表不存在，先触发同步
        if (Objects.isNull(customerInfo)) {
            customerInfoService.queryCustomerById(customerId, activityId);
            customerInfo = customerInfoMapper.selectByCustomerIdAndCampaignId(customerId, activityId);
        }
        CustomerProfile customerProfile = customerInfoService.queryCustomerById(customerInfo.getCustomerId(), customerInfo.getActivityId());
        CustomerFeatureResponse customerFeature = customerInfoService.queryCustomerFeatureById(customerInfo.getCustomerId(), customerInfo.getActivityId());

        CustomerCharacter customerCharacter = customerCharacterMapper.selectByCustomerIdAndActivityId(customerId, activityId);
        CustomerCharacter newCustomerCharacter = new CustomerCharacter();
        updateCharacter(newCustomerCharacter, customerInfo, customerProfile, customerFeature);
        if (Objects.isNull(customerCharacter)) {
            // 新建
            customerCharacterMapper.insert(newCustomerCharacter);
        } else {
            // 更新
            if (!areEqual(customerCharacter, newCustomerCharacter)) {
                customerCharacterMapper.updateAllFields(newCustomerCharacter);
            }
        }
        // 如果判断出"客户对购买软件的态度”有值不为空，则给对应的组长发送消息,客户已经购买的不用再发送
        if (checkPurchaseAttitude && !newCustomerCharacter.getCompletePurchaseStage()) {
            // 给该客户当天的通话时间大于30分钟
            int communicationDurationSum = recordService.getCommunicationTimeCurrentDay(customerId, newCustomerCharacter.getUpdateTime());
            if (communicationDurationSum < 30) {
                return;
            }
            String purchaseMessageDescribe = getPurchaseAttitude(newCustomerCharacter.getSoftwarePurchaseAttitude());
            String fundsMessageDescribe = Objects.nonNull(newCustomerCharacter.getFundsVolume()) ? newCustomerCharacter.getFundsVolume() : "未提及";
            String url = String.format("https://newcmp.emoney.cn/chat/api/customer/redirect?customer_id=%s&active_id=%s&owner_id=%s&owner=%s",
                    customerInfo.getCustomerId(), customerInfo.getActivityId(), customerInfo.getOwnerId(), customerInfo.getOwnerName());
            StringBuilder possibleReasonStringBuilder = new StringBuilder();
            int id = 1;
            if (!purchaseMessageDescribe.equals("确认购买")) {
                for (CustomerFeatureResponse.Question question : customerFeature.getSummary().getQuestions()) {
                    if (question.getMessage().contains("客户对软件功能尚未理解清晰")) {
                        possibleReasonStringBuilder.append(id++).append(".客户对软件功能尚未理解清晰，");
                        if (!StringUtils.isEmpty(question.getQuantify())) {
                            possibleReasonStringBuilder.append(question.getQuantify()).append("，");
                        }
                        if (!StringUtils.isEmpty(question.getIncomplete())) {
                            possibleReasonStringBuilder.append("<font color=\"info\">").append(question.getIncomplete()).append("</font>，");
                        } else if (!StringUtils.isEmpty(question.getComplete())) {
                            possibleReasonStringBuilder.append(question.getComplete()).append("，");
                        }
                        possibleReasonStringBuilder.append("客户问题是：")
                                .append("<font color=\"info\">").append(question.getQuestion()).append("</font>，")
                                .append("需根据客户学习能力更白话讲解。\n");
                    } else if (question.getMessage().contains("客户对选股方法尚未认可")) {
                        possibleReasonStringBuilder.append(id++).append(".客户对选股方法尚未认可，");
                        if (!StringUtils.isEmpty(question.getQuantify())) {
                            possibleReasonStringBuilder.append(question.getQuantify()).append("，");
                        }
                        if (!StringUtils.isEmpty(question.getIncomplete())) {
                            possibleReasonStringBuilder.append("<font color=\"info\">").append(question.getIncomplete()).append("</font>，");
                        } else if (!StringUtils.isEmpty(question.getComplete())) {
                            possibleReasonStringBuilder.append(question.getComplete()).append("，");
                        }
                        possibleReasonStringBuilder.append("客户问题是：")
                                .append("<font color=\"info\">").append(question.getQuestion()).append("</font>，")
                                .append("需加强选股成功的真实案例证明。\n");
                    } else if (question.getMessage().contains("客户对自身问题尚未认可")) {
                        possibleReasonStringBuilder.append(id++).append(".客户对自身问题尚未认可，");
                        if (!StringUtils.isEmpty(question.getQuantify())) {
                            possibleReasonStringBuilder.append(question.getQuantify()).append("，");
                        }
                        if (!StringUtils.isEmpty(question.getIncomplete())) {
                            possibleReasonStringBuilder.append("<font color=\"info\">").append(question.getIncomplete()).append("</font>，");
                        } else if (!StringUtils.isEmpty(question.getComplete())) {
                            possibleReasonStringBuilder.append(question.getComplete()).append("，");
                        }
                        possibleReasonStringBuilder.append("客户问题是：")
                                .append("<font color=\"info\">").append(question.getQuestion()).append("</font>，")
                                .append("需列举与客户相近的真实反面案例证明。\n");
                    } else if (question.getMessage().contains("客户对软件价值尚未认可")) {
                        possibleReasonStringBuilder.append(id++).append(".客户对软件价值尚未认可，");
                        if (!StringUtils.isEmpty(question.getQuantify())) {
                            possibleReasonStringBuilder.append(question.getQuantify()).append("，");
                        }
                        if (!StringUtils.isEmpty(question.getIncomplete())) {
                            possibleReasonStringBuilder.append("<font color=\"info\">").append(question.getIncomplete()).append("</font>，");
                        } else if (!StringUtils.isEmpty(question.getComplete())) {
                            possibleReasonStringBuilder.append(question.getComplete()).append("，");
                        }
                        possibleReasonStringBuilder.append("客户问题是：")
                                .append("<font color=\"info\">").append(question.getQuestion()).append("</font>，")
                                .append("需加强使用软件的真实成功案例证明。\n");
                    }
                }
            }
            if (possibleReasonStringBuilder.length() > 1) {
                possibleReasonStringBuilder.insert(0, "客户的顾虑点是：\n");
            }

            // 发送消息给领导，发送到微信群
            String message = String.format(AppConstant.CUSTOMER_PURCHASE_TEMPLATE,
                    newCustomerCharacter.getOwnerName(),
                    new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(customerProfile.getLastCommunicationDate()),
                    newCustomerCharacter.getCustomerName(),
                    fundsMessageDescribe,
                    purchaseMessageDescribe,
                    getApprovalCount(newCustomerCharacter),
                    possibleReasonStringBuilder,
                    url, url);
            TextMessage textMessage = new TextMessage();
            TextMessage.TextContent textContent = new TextMessage.TextContent();
            textContent.setContent(message);
            textMessage.setMsgtype("markdown");
            textMessage.setMarkdown(textContent);
            if (nightTime()) {
                log.error("延迟发送消息");
                MessageSendVO vo = new MessageSendVO(configService.getStaffAreaRobotUrl(customerInfo.getOwnerId()), textMessage);
                AppConstant.messageNeedSend.add(vo);
            } else {
                sendMessageToChat(configService.getStaffAreaRobotUrl(customerInfo.getOwnerId()), textMessage);
            }

            // 发送消息给业务员，发送给个人企微
            String target = "**";
            int index = textMessage.getMarkdown().getContent().indexOf(target, 5);
            textMessage.getMarkdown().setContent("您" + textMessage.getMarkdown().getContent().substring(index + 2));
            textMessage.setTouser(customerInfo.getOwnerId());
            textMessage.setAgentid(getAgentId(customerInfo.getOwnerId()));
            if (nightTime()) {
                log.error("延迟发送消息");
                MessageSendVO vo = new MessageSendVO(null, textMessage);
                AppConstant.messageNeedSend.add(vo);
            } else {
                sendMessageToChat(textMessage);
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

    @Override
    public void sendMessageForPerLeader(String userId) {
        // 获取该领导下的所有员工
        Map<String, List<String>> staffIdsLeader = configService.getStaffIdsLeader();
        String activityId = configService.getCurrentActivityId();
        QueryWrapper<CustomerCharacter> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("activity_id", activityId);
        List<CustomerCharacter> characterList = customerCharacterMapper.selectList(queryWrapper);
        List<CustomerInfo> customerInfoLongTimeNoSee = customerInfoService.getCustomerInfoLongTimeNoSee(activityId);
        Map<String, CustomerInfo> customerInfoLongTimeNoSeeMap = customerInfoLongTimeNoSee.stream()
                .collect(Collectors.toMap(CustomerInfo::getCustomerId, Function.identity()));
        Map<String, PotentialCustomer> potentialCustomerMap = new HashMap<>();
        for (CustomerCharacter character : characterList) {
            // 完成购买，跳过不统计
            if (character.getCompletePurchaseStage()) {
                continue;
            }
            String ownerId = character.getOwnerName();
            PotentialCustomer potentialCustomer;
            if (!potentialCustomerMap.containsKey(ownerId)) {
                potentialCustomer = new PotentialCustomer();
                potentialCustomerMap.put(ownerId, potentialCustomer);
            } else {
                potentialCustomer = potentialCustomerMap.get(ownerId);
            }
            // 判断 认可度次数
            int approvalCount = getApprovalCount(character);
            // 资金量≥5万且认可数≥3，购买态度为确认购买
            if (approvalCount >= 3 && Boolean.parseBoolean(character.getSoftwarePurchaseAttitude()) &&
                    !StringUtils.isEmpty(character.getFundsVolume()) &&
                    (character.getFundsVolume().equals("大于10万")||character.getFundsVolume().equals("5到10万之间"))) {
                potentialCustomer.getHigh().add(ownerId + "，" + character.getCustomerName() + "，" + character.getCustomerId());
            } else if (approvalCount >= 3 && !Boolean.parseBoolean(character.getSoftwarePurchaseAttitude()) &&
                    !StringUtils.isEmpty(character.getFundsVolume()) &&
                    (character.getFundsVolume().equals("大于10万")||character.getFundsVolume().equals("5到10万之间"))) { // 资金量≥5且认可数≥3，购买态度为尚未确认购买
                potentialCustomer.getMiddle().add(ownerId + "，" + character.getCustomerName() + "，" + character.getCustomerId());
            } else if (approvalCount <= 2 &&
                    !StringUtils.isEmpty(character.getFundsVolume()) &&
                    (character.getFundsVolume().equals("大于10万")||character.getFundsVolume().equals("5到10万之间"))) { // 资金量≥5万且认可数≤2
                potentialCustomer.getLow().add(ownerId + "，" + character.getCustomerName() + "，" + character.getCustomerId());
            }
            if (!StringUtils.isEmpty(character.getFundsVolume())
                    && (character.getFundsVolume().equals("大于10万")||character.getFundsVolume().equals("5到10万之间"))
                    && customerInfoLongTimeNoSeeMap.containsKey(character.getCustomerId())) {
                potentialCustomer.getLongTimeNoSee().add(character.getOwnerName() + "，" + character.getCustomerName() + "，" + character.getCustomerId());
            }
        }

        for (Map.Entry<String, List<String>> entry : staffIdsLeader.entrySet()) {
            String leaderId = entry.getKey();
            for (String memberName : entry.getValue()) {
                PotentialCustomer potentialCustomer = potentialCustomerMap.get(memberName);
                if (Objects.isNull(potentialCustomer)){
                    continue;
                }
                String message = String.format(AppConstant.PURCHASE_ATTITUDE_SUMMARY_FOR_LEADER_TEMPLATE, memberName,
                                CommonUtils.convertStringFromList(potentialCustomer.getHigh()),
                                CommonUtils.convertStringFromList(potentialCustomer.getMiddle()),
                                CommonUtils.convertStringFromList(potentialCustomer.getLongTimeNoSee()));
                TextMessage textMessage = new TextMessage();
                TextMessage.TextContent textContent = new TextMessage.TextContent();
                textMessage.setTouser(leaderId);
                textMessage.setAgentid("1000021");
                textContent.setContent(message);
                textMessage.setMsgtype("markdown");
                textMessage.setMarkdown(textContent);
                sendMessageToChat(textMessage);
            }
        }
    }

    @Override
    public void sendCommunicationSummary(List<String> ownerIdList, String activityId, String day) {
        QueryWrapper<CustomerCharacter> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("activity_id", activityId);
        List<CustomerCharacter> characterList = customerCharacterMapper.selectList(queryWrapper);
        Map<String, CustomerCharacter> characterMap = characterList.stream().collect(Collectors.toMap(CustomerCharacter::getCustomerId, Function.identity()));
        List<String> allCustomerExceed8Hour = recordService.selectCustomerExceed8Hour(activityId);

        Map<String, String> staffLeaderMap = configService.getStaffLeaderMap();

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
                potentialCustomerMap.put(ownerId, potentialCustomer);
            } else {
                potentialCustomer = potentialCustomerMap.get(ownerId);
            }
            // 判断 认可度次数
            int approvalCount = getApprovalCount(character);
            // 资金量≥5万且认可数≥3，购买态度为确认购买
            if (approvalCount >= 3 && Boolean.parseBoolean(character.getSoftwarePurchaseAttitude()) &&
                    !StringUtils.isEmpty(character.getFundsVolume()) &&
                    (character.getFundsVolume().equals("大于10万")||character.getFundsVolume().equals("5到10万之间"))) {
                potentialCustomer.getHigh().add(character.getCustomerId());
            } else if (approvalCount >= 3 && !Boolean.parseBoolean(character.getSoftwarePurchaseAttitude()) &&
                    !StringUtils.isEmpty(character.getFundsVolume()) &&
                    (character.getFundsVolume().equals("大于10万")||character.getFundsVolume().equals("5到10万之间"))) { // 资金量≥5且认可数≥3，购买态度为尚未确认购买
                potentialCustomer.getMiddle().add(character.getCustomerId());
            } else if (approvalCount <= 2 &&
                    !StringUtils.isEmpty(character.getFundsVolume()) &&
                    (character.getFundsVolume().equals("大于10万")||character.getFundsVolume().equals("5到10万之间"))) { // 资金量≥5万且认可数≤2
                potentialCustomer.getLow().add(character.getCustomerId());
            }
        }
        for (String ownerId : ownerIdList){
            // 获取某个人在某个时间段内所有的通话
            List<TelephoneRecord> telephoneRecordList;
            if (day.equals("today")) {
                telephoneRecordList = recordService.getOwnerTelephoneRecordToday(ownerId);
            } else {
                telephoneRecordList = recordService.getOwnerTelephoneRecordYesterday(ownerId);
            }
            String ownerName = telephoneRecordList.get(0).getOwnerName();
            Map<String, Integer> teleTimeMap = new HashMap<>();
            // 统计每个客户的通话时长，按照分钟统计
            for (TelephoneRecord item : telephoneRecordList) {
                if (teleTimeMap.containsKey(item.getCustomerId())) {
                    teleTimeMap.put(item.getCustomerId(), teleTimeMap.get(item.getCustomerId()) + item.getCommunicationDuration());
                } else {
                    teleTimeMap.put(item.getCustomerId(), item.getCommunicationDuration());
                }
            }
            int communicationDurationToday = teleTimeMap.values().stream().mapToInt(Integer::intValue).sum();
            // 统计该销售下所有客户的特征信息
            int highCount = 0;
            int highTime = 0;
            int middleCount = 0;
            int middleTime = 0;
            int lowCount = 0;
            int lowTime = 0;
            int elseTime = 0;
            StringBuilder elseString = new StringBuilder();
            PotentialCustomer potentialCustomer = potentialCustomerMap.get(ownerId);
            Set<String> highSet = new HashSet<>(potentialCustomer.getHigh());
            Set<String> middleSet = new HashSet<>(potentialCustomer.getMiddle());
            Set<String> lowSet = new HashSet<>(potentialCustomer.getLow());
            for (Map.Entry<String, Integer> entry: teleTimeMap.entrySet()) {
                // 双重检查
                if (characterMap.containsKey(entry.getKey())) {
                    updateCustomerCharacter(entry.getKey(), activityId, false);
                    characterMap.put(entry.getKey(), customerCharacterMapper.selectByCustomerIdAndActivityId(entry.getKey(), activityId));
                }
                if (highSet.contains(entry.getKey())){
                    highCount++;
                    highTime += entry.getValue();
                } else if (middleSet.contains(entry.getKey())){
                    middleCount++;
                    middleTime += entry.getValue();
                }  else if (lowSet.contains(entry.getKey())){
                    lowCount++;
                    lowTime += entry.getValue();
                } else {
                    try {
                        elseString.append(characterMap.get(entry.getKey()).getCustomerName()).append("，").append(entry.getKey())
                                .append("（资金体量为：").append(StringUtils.isEmpty(characterMap.get(entry.getKey()).getFundsVolume()) ? "未提及" : characterMap.get(entry.getKey()).getFundsVolume())
                                .append("，认可数为：").append(getApprovalCount(characterMap.get(entry.getKey()))).append("个，购买态度为：")
                                .append(getPurchaseAttitude(characterMap.get(entry.getKey()).getSoftwarePurchaseAttitude())).append("）\n");
                        elseTime += entry.getValue();
                    } catch (Exception e) {

                    }
                }
            }
            if (StringUtils.hasText(elseString)) {
                elseString.append("总时长为**").append(getTimeString(elseTime)).append("**");
            }

            // 构建超长通话
            StringBuilder customerExceedTimeStr = new StringBuilder();
            // 是否有单个客户当天通话超过2个小时
            Set<String> customerExceed2Hour = teleTimeMap.entrySet().stream().filter(item -> item.getValue() >= 120).map(Map.Entry::getKey).collect(Collectors.toSet());
            if (!CollectionUtils.isEmpty(customerExceed2Hour)) {
                for (String one : customerExceed2Hour) {
                    try {
                        customerExceedTimeStr.append(characterMap.get(one).getCustomerName()).append("，").append(characterMap.get(one).getCustomerId())
                                .append("，单通通话超过2小时（资金体量为：").append(StringUtils.isEmpty(characterMap.get(one).getFundsVolume()) ? "未提及" : characterMap.get(one).getFundsVolume())
                                .append("，认可数为：").append(getApprovalCount(characterMap.get(one))).append("个，购买态度为：")
                                .append(getPurchaseAttitude(characterMap.get(one).getSoftwarePurchaseAttitude())).append("）\n");
                    } catch (Exception e) {
                    }
                }
            }
            // 是否有客户累计通话超过8小时
            Set<String> customerExceed8Hour = allCustomerExceed8Hour.stream().filter(teleTimeMap::containsKey).collect(Collectors.toSet());
            if (!CollectionUtils.isEmpty(customerExceed8Hour)) {
                for (String one : customerExceed8Hour) {
                    try {
                        customerExceedTimeStr.append(characterMap.get(one).getCustomerName()).append("，").append(characterMap.get(one).getCustomerId())
                                .append("，累计通话超过4小时（资金体量为：").append(StringUtils.isEmpty(characterMap.get(one).getFundsVolume()) ? "未提及" : characterMap.get(one).getFundsVolume())
                                .append("，认可数为：").append(getApprovalCount(characterMap.get(one))).append("个，购买态度为：")
                                .append(getPurchaseAttitude(characterMap.get(one).getSoftwarePurchaseAttitude())).append("）\n");
                    } catch (Exception e) {
                    }
                }
            }
            String dayStr = day.equals("today") ? "今日" : "昨日";
            String message = String.format(COMMUNICATION_TIME_SUMMARY_FOR_STAFF_TEMPLATE,
                    dayStr, getTimeString(communicationDurationToday),
                    String.format(COMMUNICATION_TIME_SUMMARY_FOR_STAFF, highSet.size(), highCount, getTimeString(highTime)),
                    String.format(COMMUNICATION_TIME_SUMMARY_FOR_STAFF, middleSet.size(), middleCount, getTimeString(middleTime)),
                    String.format(COMMUNICATION_TIME_SUMMARY_FOR_STAFF, lowSet.size(), lowCount, getTimeString(lowTime)),
                    elseString,
                    customerExceedTimeStr
                    );
            TextMessage textMessage = new TextMessage();
            TextMessage.TextContent textContent = new TextMessage.TextContent();
            textMessage.setTouser(ownerId);
            textMessage.setAgentid("1000021");
            textContent.setContent(message);
            textMessage.setMsgtype("markdown");
            textMessage.setMarkdown(textContent);
            sendMessageToChat(textMessage);

            message = String.format( "业务员：%s:\n", ownerName) + message;
            textMessage.getMarkdown().setContent(message);
            textMessage.setTouser(staffLeaderMap.get(ownerId));
            // 发送给主管
            sendMessageToChat(textMessage);
        }
    }

    private String getTimeString(int minutes){
        // 计算小时数和剩余的分钟数
        int hours = minutes / 60;
        int remainingMinutes = minutes % 60;

        // 如果小时数大于0，则返回小时和分钟的格式
        if (hours > 0) {
            return hours + "小时" + remainingMinutes + "分钟";
        } else {
            // 如果小时数为0，则只返回分钟数
            return remainingMinutes + "分钟";
        }
    }

    private String getPurchaseAttitude(String purchaseAttitude){
        if (StringUtils.isEmpty(purchaseAttitude)) {
            return "未提及";
        }
        return Boolean.parseBoolean(purchaseAttitude) ? "确认购买" : "尚未确认购买";
    }

    private int getApprovalCount(CustomerCharacter character){
        // 判断 认可度次数
        int approvalCount = 0;
        if (Boolean.parseBoolean(character.getSoftwareFunctionClarity())) {
            approvalCount++;
        }
        if (Boolean.parseBoolean(character.getStockSelectionMethod())) {
            approvalCount++;
        }
        if (Boolean.parseBoolean(character.getSelfIssueRecognition())) {
            approvalCount++;
        }
        if (Boolean.parseBoolean(character.getSoftwareValueApproval())) {
            approvalCount++;
        }

        return approvalCount;
    }

    private boolean nightTime(){
        LocalTime now = LocalTime.now();
        LocalTime start = LocalTime.of(22, 0);
        LocalTime end = LocalTime.of(8, 0);
        return now.isAfter(start) || now.isBefore(end);
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

    private void updateCharacter(CustomerCharacter latestCustomerCharacter, CustomerInfo customerInfo,
                                 CustomerProfile customerProfile, CustomerFeatureResponse customerFeature) {
        latestCustomerCharacter.setId(customerInfo.getId());
        latestCustomerCharacter.setCustomerId(customerInfo.getCustomerId());
        latestCustomerCharacter.setCustomerName(customerInfo.getCustomerName());
        latestCustomerCharacter.setOwnerId(customerInfo.getOwnerId());
        latestCustomerCharacter.setOwnerName(customerInfo.getOwnerName());
        latestCustomerCharacter.setActivityName(customerInfo.getActivityName());
        latestCustomerCharacter.setActivityId(customerInfo.getActivityId());
        latestCustomerCharacter.setConversionRate(conversionRateMap.get(customerProfile.getConversionRate()));

        latestCustomerCharacter.setMatchingJudgmentStage(customerProfile.getCustomerStage().getMatchingJudgment() == 1);
        latestCustomerCharacter.setTransactionStyleStage(customerProfile.getCustomerStage().getTransactionStyle() == 1);
        latestCustomerCharacter.setFunctionIntroductionStage(customerProfile.getCustomerStage().getFunctionIntroduction() ==
                1);
        latestCustomerCharacter.setConfirmValueStage(customerProfile.getCustomerStage().getConfirmValue() == 1);
        latestCustomerCharacter.setConfirmPurchaseStage(customerProfile.getCustomerStage().getConfirmPurchase() == 1);
        latestCustomerCharacter.setCompletePurchaseStage(customerProfile.getCustomerStage().getCompletePurchase() == 1);

        latestCustomerCharacter.setFundsVolume(FundsVolumeEnum.getTextByValue(
                Objects.nonNull(customerFeature.getBasic().getFundsVolume().getCustomerConclusion().getCompareValue()) ? customerFeature.getBasic().getFundsVolume().getCustomerConclusion().getCompareValue().toString() : null));

        latestCustomerCharacter.setSoftwareFunctionClarity(Objects.nonNull(customerFeature.getBasic().getSoftwareFunctionClarity().getCustomerConclusion().getCompareValue()) ? customerFeature.getBasic().getSoftwareFunctionClarity().getCustomerConclusion().getCompareValue().toString() : null);
        latestCustomerCharacter.setStockSelectionMethod(Objects.nonNull(customerFeature.getBasic().getStockSelectionMethod().getCustomerConclusion().getCompareValue()) ? customerFeature.getBasic().getStockSelectionMethod().getCustomerConclusion().getCompareValue().toString() : null);
        latestCustomerCharacter.setSelfIssueRecognition(Objects.nonNull(customerFeature.getBasic().getSelfIssueRecognition().getCustomerConclusion().getCompareValue()) ? customerFeature.getBasic().getSelfIssueRecognition().getCustomerConclusion().getCompareValue().toString() : null);
        latestCustomerCharacter.setSoftwareValueApproval(Objects.nonNull(customerFeature.getBasic().getSoftwareValueApproval().getCustomerConclusion().getCompareValue()) ? customerFeature.getBasic().getSoftwareValueApproval().getCustomerConclusion().getCompareValue().toString() : null);
        latestCustomerCharacter.setSoftwarePurchaseAttitude(
                Objects.nonNull(customerFeature.getBasic().getSoftwarePurchaseAttitude().getCustomerConclusion().getCompareValue()) ? customerFeature.getBasic().getSoftwarePurchaseAttitude().getCustomerConclusion().getCompareValue().toString() : null);

        latestCustomerCharacter.setStandardExplanationCompletion(Objects.nonNull(customerFeature.getBasic().getSoftwareFunctionClarity()) ? customerFeature.getBasic().getSoftwareFunctionClarity().getStandardProcess() : 0);
        latestCustomerCharacter.setCustomerContinueCommunicate(Objects.nonNull(customerFeature.getBasic().getCustomerContinueCommunicate().getCustomerConclusion().getCompareValue()) ? customerFeature.getBasic().getCustomerContinueCommunicate().getCustomerConclusion().getCompareValue().toString() : null);
        latestCustomerCharacter.setOwnerPackagingCourse(Objects.nonNull(customerFeature.getBasic().getOwnerPackagingCourse().getCustomerConclusion().getCompareValue()) ? customerFeature.getBasic().getOwnerPackagingCourse().getCustomerConclusion().getCompareValue().toString() : null);
        latestCustomerCharacter.setOwnerPackagingFunction(Objects.nonNull(customerFeature.getBasic().getOwnerPackagingFunction().getCustomerConclusion().getCompareValue()) ? customerFeature.getBasic().getOwnerPackagingFunction().getCustomerConclusion().getCompareValue().toString() : null);

        List<String> advantages = customerFeature.getSummary().getAdvantage();
        List<CustomerFeatureResponse.Question> questions = customerFeature.getSummary().getQuestions();
        for (String item : advantages) {
            if (item.contains("完成资金量收集")) {
                latestCustomerCharacter.setSummaryMatchJudgment("true");
            } else if (item.contains("完成客户交易风格了解")) {
                latestCustomerCharacter.setSummaryTransactionStyle("true");
            } else if (item.contains("客户对软件功能理解清晰")) {
                latestCustomerCharacter.setSummaryFunctionIntroduction("true");
            } else if (item.contains("客户认可软件价值")) {
                latestCustomerCharacter.setSummaryConfirmValue("true");
            } else if (item.contains("完成痛点和价值量化放大")) {
                latestCustomerCharacter.setIssuesValueQuantified("true");
            }
        }
        for (CustomerFeatureResponse.Question question : questions) {
            String item = question.getMessage();
            if (item.contains("未完成资金量收集")) {
                latestCustomerCharacter.setSummaryMatchJudgment("false");
            } else if (item.contains("未完成客户交易风格了解")) {
                latestCustomerCharacter.setSummaryTransactionStyle("false");
            } else if (item.contains("客户对软件功能尚未理解清晰")) {
                latestCustomerCharacter.setSummaryFunctionIntroduction("false");
            } else if (item.contains("客户对软件价值尚未认可")) {
                latestCustomerCharacter.setSummaryConfirmValue("false");
            } else if (item.contains("未完成痛点和价值量化放大")) {
                latestCustomerCharacter.setIssuesValueQuantified("false");
            }
        }
        latestCustomerCharacter.setIsSend188(customerProfile.getIsSend188());
        latestCustomerCharacter.setUpdateTime(
                customerProfile.getLastCommunicationDate().toInstant().atZone(ZoneId.of("Asia/Shanghai")).toLocalDateTime());
    }

}
