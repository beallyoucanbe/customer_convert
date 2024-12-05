package com.smart.sso.server.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.collect.ImmutableMap;
import com.smart.sso.server.constant.AppConstant;
import com.smart.sso.server.enums.EarningDesireEnum;
import com.smart.sso.server.enums.FundsVolumeEnum;
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
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
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
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, requestEntity, String.class);
        // 处理响应
        if (response.getStatusCode() == HttpStatus.OK) {
            log.error("发送消息结果：" + response.getBody());
            Map<String, Object> StringMap = JsonUtil.readValue(response.getBody(), new TypeReference<Map<String, Object>>() {
            });
            if (!StringMap.get("errcode").toString().equals("0")) {
                configService.refreshCustomerConfig();
                url = String.format(AppConstant.SEND_APPLICATION_MESSAGE_URL, getAccessToken(message.getTouser()));
                restTemplate.exchange(url, HttpMethod.POST, requestEntity, String.class);
            }
            return response.getBody();
        } else {
            log.error("Failed to send message: " + response.getStatusCode());
            throw new RuntimeException("Failed to send message: " + response.getStatusCode());
        }
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
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, requestEntity, String.class);
        // 处理响应
        if (response.getStatusCode() == HttpStatus.OK) {
            log.error("发送消息结果：" + response.getBody());
            return response.getBody();
        } else {
            log.error("Failed to send message: " + response.getStatusCode());
            throw new RuntimeException("Failed to send message: " + response.getStatusCode());
        }
    }


    @Override
    public void sendPurchaseAttitudeSummary(String activityId) {
        QueryWrapper<CustomerCharacter> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("activity_id", activityId);
        List<CustomerCharacter> characterList = customerCharacterMapper.selectList(queryWrapper);
        Map<String, PotentialCustomer> potentialCustomerMap = new HashMap<>();
        String url = "http://172.16.192.61:8086/publish/E130491D3CA6E697A4E9479E1754C69E/dashboard/E55EFC762B3F0245C8F48FB6D6F17E4E2";
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
            // 认可数>=3旦态度为认可
            if (approvalCount >= 3 && Boolean.parseBoolean(character.getSoftwarePurchaseAttitude())) {
                potentialCustomer.getHigh().add(character.getCustomerName() + "，" + character.getCustomerId());
            } else if (approvalCount >= 3 && !Boolean.parseBoolean(character.getSoftwarePurchaseAttitude())) {
                potentialCustomer.getMiddle().add(character.getCustomerName() + "，" + character.getCustomerId());
            } else if (approvalCount <= 2 &&
                    (character.getConversionRate().equals("较高") || character.getConversionRate().equals("中等"))) {
                potentialCustomer.getLow().add(character.getCustomerName() + "，" + character.getCustomerId());
            }
            potentialCustomerMap.put(ownerId, potentialCustomer);
        }
        // 给每个业务员发送统计消息
        for (Map.Entry<String, PotentialCustomer> entry : potentialCustomerMap.entrySet()) {
            String message = String.format(AppConstant.PURCHASE_ATTITUDE_SUMMARY_TEMPLATE,
                    CommonUtils.convertStringFromList(entry.getValue().getHigh()),
                    CommonUtils.convertStringFromList(entry.getValue().getMiddle()),
                    CommonUtils.convertStringFromList(entry.getValue().getLow()),
                    url, url);
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
            int communicationDurationSum = recordService.getCommunicationTimeCurrentDay(customerId);
            if (communicationDurationSum < 10) {
                return;
            }
            String messageDescribe;
            if (Objects.nonNull(newCustomerCharacter.getSoftwarePurchaseAttitude())) {
                messageDescribe = Boolean.parseBoolean(newCustomerCharacter.getSoftwarePurchaseAttitude()) ?
                        "确认购买" : "尚未确认购买";
            } else {
                messageDescribe = "未提及";
            }
            String url = String.format("https://newcmp.emoney.cn/chat/api/customer/redirect?customer_id=%s&active_id=%s",
                    customerInfo.getCustomerId(), customerInfo.getActivityId());
            StringBuilder possibleReasonStringBuilder = new StringBuilder();
            int id = 1;
            if (!messageDescribe.equals("确认购买")) {
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
                    customerInfo.getUpdateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")),
                    newCustomerCharacter.getCustomerName(),
                    messageDescribe,
                    possibleReasonStringBuilder,
                    url, url);
            TextMessage textMessage = new TextMessage();
            TextMessage.TextContent textContent = new TextMessage.TextContent();
            textContent.setContent(message);
            textMessage.setMsgtype("markdown");
            textMessage.setMarkdown(textContent);
            sendMessageToChat(configService.getStaffAreaRobotUrl(customerInfo.getOwnerId()), textMessage);

            // 发送消息给业务员，发送给个人企微
            String target = "**";
            int index = textMessage.getMarkdown().getContent().indexOf(target, 5);
            textMessage.getMarkdown().setContent("您" + textMessage.getMarkdown().getContent().substring(index + 2));
            textMessage.setTouser(customerInfo.getOwnerId());
            textMessage.setAgentid(getAgentId(customerInfo.getOwnerId()));
            sendMessageToChat(textMessage);
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
        CustomerInfo customerInfo = customerInfoMapper.selectById(id);
        CustomerFeatureResponse featureResponse = customerInfoService.queryCustomerFeatureById(customerInfo.getCustomerId(), customerInfo.getActivityId());
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
        if (customerInfo.getConversionRate().equals("low")) {
            rateDesc = conversionRateMap.get(customerInfo.getConversionRate()) + "（不应重点跟进）";
        } else {
            rateDesc = conversionRateMap.get(customerInfo.getConversionRate());
        }
        String message = String.format(AppConstant.CUSTOMER_SUMMARY_MARKDOWN_TEMPLATE, customerInfo.getCustomerName(),
                customerInfo.getCustomerId(),
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
        latestCustomerCharacter.setEarningDesire(EarningDesireEnum.getTextByValue(
                Objects.nonNull(customerFeature.getBasic().getEarningDesire().getCustomerConclusion().getCompareValue()) ? customerFeature.getBasic().getEarningDesire().getCustomerConclusion().getCompareValue().toString() : null));

        latestCustomerCharacter.setSoftwareFunctionClarity(Objects.nonNull(customerFeature.getBasic().getSoftwareFunctionClarity().getCustomerConclusion().getCompareValue()) ? customerFeature.getBasic().getSoftwareFunctionClarity().getCustomerConclusion().getCompareValue().toString() : null);
        latestCustomerCharacter.setStockSelectionMethod(Objects.nonNull(customerFeature.getBasic().getStockSelectionMethod().getCustomerConclusion().getCompareValue()) ? customerFeature.getBasic().getStockSelectionMethod().getCustomerConclusion().getCompareValue().toString() : null);
        latestCustomerCharacter.setSelfIssueRecognition(Objects.nonNull(customerFeature.getBasic().getSelfIssueRecognition().getCustomerConclusion().getCompareValue()) ? customerFeature.getBasic().getSelfIssueRecognition().getCustomerConclusion().getCompareValue().toString() : null);
        latestCustomerCharacter.setSoftwareValueApproval(Objects.nonNull(customerFeature.getBasic().getSoftwareValueApproval().getCustomerConclusion().getCompareValue()) ? customerFeature.getBasic().getSoftwareValueApproval().getCustomerConclusion().getCompareValue().toString() : null);
        latestCustomerCharacter.setSoftwarePurchaseAttitude(
                Objects.nonNull(customerFeature.getBasic().getSoftwarePurchaseAttitude().getCustomerConclusion().getCompareValue()) ? customerFeature.getBasic().getSoftwarePurchaseAttitude().getCustomerConclusion().getCompareValue().toString() : null);

        latestCustomerCharacter.setStandardExplanationCompletion(Objects.nonNull(customerFeature.getBasic().getSoftwareFunctionClarity()) ? customerFeature.getBasic().getSoftwareFunctionClarity().getStandardProcess() : 0);

        List<String> advantages = customerFeature.getSummary().getAdvantage();
        List<CustomerFeatureResponse.Question> questions = customerFeature.getSummary().getQuestions();
        for (String item : advantages) {
            if (item.contains("完成客户匹配度判断")) {
                latestCustomerCharacter.setSummaryMatchJudgment("true");
            } else if (item.contains("完成客户交易风格了解")) {
                latestCustomerCharacter.setSummaryTransactionStyle("true");
            } else if (item.contains("跟进对的客户")) {
                latestCustomerCharacter.setSummaryFollowCustomer("true");
            } else if (item.contains("客户对软件功能理解清晰")) {
                latestCustomerCharacter.setSummaryFunctionIntroduction("true");
            } else if (item.contains("客户认可软件价值")) {
                latestCustomerCharacter.setSummaryConfirmValue("true");
            } else if (item.contains("执行顺序正确")) {
                latestCustomerCharacter.setSummaryExecuteOrder("true");
            } else if (item.contains("完成痛点和价值量化放大")) {
                latestCustomerCharacter.setIssuesValueQuantified("true");
            }
        }
        for (CustomerFeatureResponse.Question question : questions) {
            String item = question.getMessage();
            if (item.contains("未完成客户匹配度判断")) {
                latestCustomerCharacter.setSummaryMatchJudgment("false");
            } else if (item.contains("未完成客户交易风格了解")) {
                latestCustomerCharacter.setSummaryTransactionStyle("false");
            } else if (item.contains("跟进匹配度低的客户")) {
                latestCustomerCharacter.setSummaryFollowCustomer("false");
            } else if (item.contains("客户对软件功能尚未理解清晰")) {
                latestCustomerCharacter.setSummaryFunctionIntroduction("false");
            } else if (item.contains("客户对软件价值尚未认可")) {
                latestCustomerCharacter.setSummaryConfirmValue("false");
            } else if (item.contains("执行顺序错误")) {
                latestCustomerCharacter.setSummaryExecuteOrder("false");
            } else if (item.contains("未完成痛点和价值量化放大")) {
                latestCustomerCharacter.setIssuesValueQuantified("false");
            }
        }
        latestCustomerCharacter.setIsSend188(customerProfile.getIsSend188());
        latestCustomerCharacter.setUpdateTime(
                customerProfile.getLastCommunicationDate().toInstant().atZone(ZoneId.of("Asia/Shanghai")).toLocalDateTime());
    }


    private void execute(List<CustomerCharacter> characterList, Map<String, List<SummaryMessage>> ownerSummaryMessages) {
        if (CollectionUtils.isEmpty(characterList)) {
            return;
        }
        for (CustomerCharacter character : characterList) {
            SummaryMessage summaryMessage = new SummaryMessage();
            try {
                customerInfoService.updateCharacterCostTime(character.getId());
            } catch (Exception e) {
                log.error("更新特征的提取时间失败：" + character.getId());
            }
            if (character.getMatchingJudgmentStage()) {
                summaryMessage.getAdvantages().merge("完成客户匹配度判断", 1, Integer::sum);
            } else {
                summaryMessage.getQuestions().merge("尚未完成客户匹配度判断，需继续收集客户信息", 1, Integer::sum);
            }
            if (character.getTransactionStyleStage()) {
                summaryMessage.getAdvantages().merge("完成客户交易风格了解", 1, Integer::sum);
            } else {
                summaryMessage.getQuestions().merge("尚未完成客户交易风格了解，需继续收集客户信息", 1, Integer::sum);
            }
            if (character.getConfirmPurchaseStage()) {
                summaryMessage.getAdvantages().merge("客户确认购买", 1, Integer::sum);
            }
            if (character.getCompletePurchaseStage()) {
                summaryMessage.getAdvantages().merge("客户完成购买", 1, Integer::sum);
            }
            if (!StringUtils.isEmpty(character.getFundsVolume())) {
                summaryMessage.getAdvantages().merge("完成客户资金体量收集", 1, Integer::sum);
            } else {
                summaryMessage.getQuestions().merge("尚未完成客户资金体量收集，需继续收集客户信息", 1, Integer::sum);
            }
            if (!StringUtils.isEmpty(character.getConversionRate())) {
                if (character.getConversionRate().equals("high") || character.getConversionRate().equals("medium")) {
                    summaryMessage.getAdvantages().merge("跟进对的客户", 1, Integer::sum);
                } else if (character.getConversionRate().equals("low")) {
                    summaryMessage.getQuestions().merge("跟进匹配度低的客户，需确认匹配度高和中的客户都已跟进完毕再跟进匹配度低的客户", 1, Integer::sum);
                }
            }

            if (!StringUtils.isEmpty(character.getSummaryExecuteOrder()) &&
                    Boolean.parseBoolean(character.getSummaryExecuteOrder())) {
                summaryMessage.getAdvantages().merge("SOP执行顺序正确", 1, Integer::sum);
            } else if (!StringUtils.isEmpty(character.getSummaryExecuteOrder()) &&
                    !Boolean.parseBoolean(character.getSummaryExecuteOrder())) {
                summaryMessage.getQuestions().merge("SOP执行顺序错误，需完成前序任务", 1, Integer::sum);
            }
            if (!StringUtils.isEmpty(character.getIssuesValueQuantified()) &&
                    Boolean.parseBoolean(character.getIssuesValueQuantified())) {
                summaryMessage.getAdvantages().merge("痛点和价值量化放大", 1, Integer::sum);
            } else if (!StringUtils.isEmpty(character.getIssuesValueQuantified()) &&
                    !Boolean.parseBoolean(character.getIssuesValueQuantified())) {
                summaryMessage.getQuestions().merge("尚未完成痛点和价值量化放大，需后续完成", 1, Integer::sum);
            }
            if (!StringUtils.isEmpty(character.getSoftwareFunctionClarity()) &&
                    Boolean.parseBoolean(character.getSoftwareFunctionClarity())) {
                summaryMessage.getAdvantages().merge("客户对软件功能理解清晰", 1, Integer::sum);
            } else if (!StringUtils.isEmpty(character.getSoftwareFunctionClarity()) &&
                    !Boolean.parseBoolean(character.getSoftwareFunctionClarity())) {
                summaryMessage.getQuestions().merge("客户对软件功能尚未理解清晰，需根据客户学习能力更白话讲解", 1, Integer::sum);
            }
            if (!StringUtils.isEmpty(character.getStockSelectionMethod()) &&
                    Boolean.parseBoolean(character.getStockSelectionMethod())) {
                summaryMessage.getAdvantages().merge("客户认可选股方法", 1, Integer::sum);
            } else if (!StringUtils.isEmpty(character.getStockSelectionMethod()) &&
                    !Boolean.parseBoolean(character.getStockSelectionMethod())) {
                summaryMessage.getQuestions().merge("客户对选股方法尚未认可，需加强选股成功的真实案例证明", 1, Integer::sum);
            }
            if (!StringUtils.isEmpty(character.getSelfIssueRecognition()) &&
                    Boolean.parseBoolean(character.getSelfIssueRecognition())) {
                summaryMessage.getAdvantages().merge("客户认可自身问题", 1, Integer::sum);
            } else if (!StringUtils.isEmpty(character.getSelfIssueRecognition()) &&
                    !Boolean.parseBoolean(character.getSelfIssueRecognition())) {
                summaryMessage.getQuestions().merge("客户对自身问题尚未认可，需列举与客户相近的真实反面案例证明", 1, Integer::sum);
            }
            if (!StringUtils.isEmpty(character.getSoftwareValueApproval()) &&
                    Boolean.parseBoolean(character.getSoftwareValueApproval())) {
                summaryMessage.getAdvantages().merge("客户认可软件价值", 1, Integer::sum);
            } else if (!StringUtils.isEmpty(character.getSoftwareValueApproval()) &&
                    !Boolean.parseBoolean(character.getSoftwareValueApproval())) {
                summaryMessage.getQuestions().merge("客户对软件价值尚未认可，需加强使用软件的真实成功案例证明", 1, Integer::sum);
            }
            if (!StringUtils.isEmpty(character.getSoftwarePurchaseAttitude()) &&
                    !Boolean.parseBoolean(character.getSoftwarePurchaseAttitude())) {
                summaryMessage.getQuestions().merge("客户尚未确认购买，需暂停劝说客户购买，明确客户顾虑点进行针对性化解", 1, Integer::sum);
            }
            if (!StringUtils.isEmpty(character.getSummaryFollowCustomer()) &&
                    Boolean.parseBoolean(character.getSummaryFollowCustomer())) {
                summaryMessage.getAdvantages().merge("跟进对的客户", 1, Integer::sum);
            } else if (!StringUtils.isEmpty(character.getSummaryFollowCustomer()) &&
                    !Boolean.parseBoolean(character.getSummaryFollowCustomer())) {
                summaryMessage.getQuestions().merge("跟进匹配度低的客户，需确认匹配度高和中的客户都已跟进完毕再跟进匹配度低的客户", 1, Integer::sum);
            }

            if (ownerSummaryMessages.containsKey(character.getOwnerName())) {
                ownerSummaryMessages.get(character.getOwnerName()).add(summaryMessage);
            } else {
                List<SummaryMessage> messageList = new ArrayList<>();
                messageList.add(summaryMessage);
                ownerSummaryMessages.put(character.getOwnerName(), messageList);
            }
        }
        // 统计个人的消息，发送
        for (Map.Entry<String, List<SummaryMessage>> entry : ownerSummaryMessages.entrySet()) {
            SummaryMessage messages = new SummaryMessage();
            for (SummaryMessage item : entry.getValue()) {
                for (String key : messages.getAdvantages().keySet()) {
                    messages.getAdvantages().put(key,
                            messages.getAdvantages().get(key) + item.getAdvantages().get(key));
                }
                for (String key : messages.getQuestions().keySet()) {
                    messages.getQuestions().put(key,
                            messages.getQuestions().get(key) + item.getQuestions().get(key));
                }
            }

            StringBuilder complete = new StringBuilder();
            StringBuilder incomplete = new StringBuilder();
            int i = 1;
            for (Map.Entry<String, Integer> item : messages.getAdvantages().entrySet()) {
                if (item.getValue() == 0) {
                    continue;
                }
                complete.append(i++).append(". ").append(item.getKey()).append("：过去半日共计").append(item.getValue()).append("个\n");
            }
            i = 1;
            for (Map.Entry<String, Integer> item : messages.getQuestions().entrySet()) {
                if (item.getValue() == 0) {
                    continue;
                }
                incomplete.append(i++).append(". ").append(item.getKey()).append("：过去半日共计").append(item.getValue()).append("个\n");
            }

            String message = String.format(AppConstant.CUSTOMER_SUMMARY_MARKDOWN_TEMPLATE, DateUtil.getFormatCurrentTime("yyyy-MM-dd HH:mm"), complete, incomplete,
                    CUSTOMER_DASHBOARD_URL, CUSTOMER_DASHBOARD_URL);
            TextMessage textMessage = new TextMessage();
            TextMessage.TextContent textContent = new TextMessage.TextContent();
            textContent.setContent(message);
            textMessage.setMsgtype("markdown");
            textMessage.setMarkdown(textContent);
            sendMessageToChat(textMessage);
        }
    }
}
