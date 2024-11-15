package com.smart.sso.server.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.collect.ImmutableMap;
import com.smart.sso.server.constant.AppConstant;
import com.smart.sso.server.enums.ConfigTypeEnum;
import com.smart.sso.server.enums.EarningDesireEnum;
import com.smart.sso.server.enums.FundsVolumeEnum;
import com.smart.sso.server.mapper.ConfigMapper;
import com.smart.sso.server.mapper.CustomerCharacterMapper;
import com.smart.sso.server.mapper.CustomerInfoMapper;
import com.smart.sso.server.mapper.TelephoneRecordMapper;
import com.smart.sso.server.model.*;
import com.smart.sso.server.model.VO.CustomerProfile;
import com.smart.sso.server.model.dto.CustomerFeatureResponse;
import com.smart.sso.server.model.dto.LeadMemberRequest;
import com.smart.sso.server.service.ConfigService;
import com.smart.sso.server.service.CustomerInfoService;
import com.smart.sso.server.service.MessageService;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

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
    private ConfigMapper configMapper;
    @Autowired
    private CustomerInfoMapper customerInfoMapper;
    @Autowired
    private TelephoneRecordMapper telephoneRecordMapper;

    ImmutableMap<String, String> conversionRateMap = ImmutableMap.<String, String>builder().put("incomplete", "未完成判断").put("low", "较低").put("medium", "中等").put("high", "较高").build();

    @Override
    public String sendMessageToChat(TextMessage message) {
        // 创建请求头
        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/json");
        log.error("发送消息内容：" + JsonUtil.serialize(message));
        // 创建请求实体
        HttpEntity<TextMessage> requestEntity = new HttpEntity<>(message, headers);
        String url = String.format(AppConstant.SEND_APPLICATION_MESSAGE_URL, AppConstant.accessToken);
        // 发送 POST 请求
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, requestEntity, String.class);
        // 处理响应
        if (response.getStatusCode() == HttpStatus.OK) {
            return response.getBody();
        } else {
            log.error("Failed to send message: " + response.getStatusCode());
            throw new RuntimeException("Failed to send message: " + response.getStatusCode());
        }
    }

    @Override
    public void sendNoticeForLeader(LeadMemberRequest leadMember, String currentCampaign, LocalDateTime dateTime) {
        String area = leadMember.getArea();
        List<String> leaders = leadMember.getLeaders();
        List<String> members = leadMember.getMembers();
        // 获取销售的所有客户进行总结
        // 获取当前活动内的所有客户
        QueryWrapper<CustomerCharacter> queryWrapper1 = new QueryWrapper<>();
        queryWrapper1.in("owner_name", members);
        queryWrapper1.in("current_campaign", currentCampaign);
        queryWrapper1.gt("update_time_telephone", dateTime);
        List<CustomerCharacter> characterList = customerCharacterMapper.selectList(queryWrapper1);
        Map<String, List<SummaryMessage>> ownerSummaryMessages = new HashMap<>();
        // 对获取的所有客户进行总结
        execute(characterList, ownerSummaryMessages);
        SummaryMessage summaryMessage = new SummaryMessage();
        for (List<SummaryMessage> entry : ownerSummaryMessages.values()) {
            for (SummaryMessage item : entry) {
                for (String key : summaryMessage.getAdvantages().keySet()) {
                    summaryMessage.getAdvantages().put(key,
                            summaryMessage.getAdvantages().get(key) + item.getAdvantages().get(key));
                }
                for (String key : summaryMessage.getQuestions().keySet()) {
                    summaryMessage.getQuestions().put(key,
                            summaryMessage.getQuestions().get(key) + item.getQuestions().get(key));
                }
            }
        }
        StringBuilder complete = new StringBuilder();
        StringBuilder incomplete = new StringBuilder();
        int i = 1;
        for (Map.Entry<String, Integer> item : summaryMessage.getAdvantages().entrySet()) {
            if (item.getValue() == 0) {
                continue;
            }
            complete.append(i++).append(". ").append(item.getKey()).append("：过去半日共计").append(item.getValue()).append("个\n");
        }
        i = 1;
        for (Map.Entry<String, Integer> item : summaryMessage.getQuestions().entrySet()) {
            if (item.getValue() == 0) {
                continue;
            }
            incomplete.append(i++).append(". ").append(item.getKey()).append("：过去半日共计").append(item.getValue()).append("个\n");
        }
        String message = String.format(AppConstant.LEADER_SUMMARY_MARKDOWN_TEMPLATE, DateUtil.getFormatCurrentTime("yyyy-MM-dd HH:mm"), complete, incomplete,
                CUSTOMER_DASHBOARD_URL, CUSTOMER_DASHBOARD_URL);
        TextMessage textMessage = new TextMessage();
        TextMessage.TextContent textContent = new TextMessage.TextContent();
        textContent.setContent(message);
        textMessage.setMarkdown(textContent);
        sendMessageToChat(textMessage);
    }

    @Override
    public void updateCustomerCharacter(String id,  boolean checkPurchaseAttitude) {
        CustomerInfo customerInfo = customerInfoMapper.selectById(id);
        CustomerProfile customerProfile = customerInfoService.queryCustomerById(customerInfo.getCustomerId(), customerInfo.getActivityId());
        CustomerFeatureResponse customerFeature = customerInfoService.queryCustomerFeatureById(customerInfo.getCustomerId(), customerInfo.getActivityId());

        CustomerCharacter customerCharacter = customerCharacterMapper.selectById(id);
        CustomerCharacter newCustomerCharacter = new CustomerCharacter();
        updateCharacter(newCustomerCharacter, customerInfo, customerProfile, customerFeature);
        if (Objects.isNull(customerCharacter)) {
            // 新建
            customerCharacterMapper.insert(newCustomerCharacter);
        } else {
            // 更新
            if (!areEqual(customerCharacter, newCustomerCharacter)){
                customerCharacterMapper.updateAllFields(newCustomerCharacter);
            }
        }
        // 如果判断出"客户对购买软件的态度”有值不为空，则给对应的组长发送消息,客户已经购买的不用再发送
        if (checkPurchaseAttitude && Objects.nonNull(newCustomerCharacter.getSoftwarePurchaseAttitude()) &&
                !newCustomerCharacter.getCompletePurchaseStage()){
            // 给该客户当天的通话时间大于30分钟
            QueryWrapper<TelephoneRecord> queryWrapperInfo = new QueryWrapper<>();
            LocalDateTime startOfDay = LocalDateTime.of(LocalDate.now(), LocalTime.MIDNIGHT);
            queryWrapperInfo.eq("customer_id", customerInfo.getCustomerId());
            queryWrapperInfo.gt("communication_time", startOfDay);
            // 查看该客户的所有通话记录，并且按照顺序排列
            List<TelephoneRecord> telephoneRecordList = telephoneRecordMapper.selectList(queryWrapperInfo);
            int communicationDurationSum = 0;
            for (TelephoneRecord item : telephoneRecordList) {
                communicationDurationSum += item.getCommunicationDuration();
            }
            if (communicationDurationSum < 30) {
                return;
            }
            String messageDescribe = Boolean.parseBoolean(newCustomerCharacter.getSoftwarePurchaseAttitude()) ?
                    "确认购买" : "尚未确认购买";
            String leaderMessageUrl = getLeaderMessageUrl(newCustomerCharacter.getOwnerName());
            if (Objects.nonNull(leaderMessageUrl)){
                String url = String.format("https://newcmp.emoney.cn/chat/api/customer/redirect?customer_id=%s&active_id=%s",
                        customerInfo.getCustomerId(), customerInfo.getActivityId());
                StringBuilder possibleReasonStringBuilder = new StringBuilder();
                if (messageDescribe.equals("尚未确认购买")) {
                    if (!StringUtils.isEmpty(newCustomerCharacter.getSoftwareFunctionClarity()) && newCustomerCharacter.getSoftwareFunctionClarity().equals("false")){
                        possibleReasonStringBuilder.append("客户对软件功能尚未理解清晰，需根据客户学习能力更白话讲解。\n");
                    }
                    if (!StringUtils.isEmpty(newCustomerCharacter.getStockSelectionMethod()) && newCustomerCharacter.getStockSelectionMethod().equals("false")){
                        possibleReasonStringBuilder.append("客户对选股方法尚未认可，需加强选股成功的真实案例证明。\n");
                    }
                    if (!StringUtils.isEmpty(newCustomerCharacter.getSelfIssueRecognition()) && newCustomerCharacter.getSelfIssueRecognition().equals("false")){
                        possibleReasonStringBuilder.append("客户对自身问题尚未认可，需列举与客户相近的真实反面案例证明。\n");
                    }
                    if (!StringUtils.isEmpty(newCustomerCharacter.getSoftwareValueApproval()) && newCustomerCharacter.getSoftwareValueApproval().equals("false")){
                        possibleReasonStringBuilder.append("客户对软件价值尚未认可，需加强使用软件的真实成功案例证明。\n");
                    }
                }
                if (possibleReasonStringBuilder.length() > 1){
                    possibleReasonStringBuilder.insert(0, "客户的顾虑点可能是：\n<font color=\"info\">");
                    possibleReasonStringBuilder.append("</font>");
                }
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
                sendMessageToChat(textMessage);
                String target = "**";
                int index = textMessage.getMarkdown().getContent().indexOf(target, 5);
                textMessage.getMarkdown().setContent("您" + textMessage.getMarkdown().getContent().substring(index + 2));
                sendMessageToChat(textMessage);
            }
        }
    }

    @Override
    public String getAccessToken() {
        if (StringUtils.isEmpty(AppConstant.agentId)){
            QiweiApplicationConfig applicationConfig = configService.getQiweiApplicationConfig();
            AppConstant.agentId = applicationConfig.getAgentId();
            AppConstant.corpId = applicationConfig.getCorpId();
            AppConstant.corpSecret = applicationConfig.getCorpSecret();
        }
        String url = String.format(AppConstant.GET_SECRET_URL, AppConstant.corpId, AppConstant.corpSecret);
        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
        // 处理响应
        try {
            if (response.getStatusCode() == HttpStatus.OK) {
                AccessTokenResponse accessTokenResponse = JsonUtil.readValue(response.getBody(), new TypeReference<AccessTokenResponse>() {
                });
                return accessTokenResponse.getAccessToken();
            } else {
                throw new RuntimeException("Failed to get access token: " + response.getStatusCode());
            }
        } catch (Exception e) {
            log.error("Failed to get access token: " + response.getStatusCode());
            throw new RuntimeException("Failed to get access token: " + response.getStatusCode());
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
            if ("createTime".equals(field.getName()) || "update_time_telephone".equals(field.getName())) {
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
        if (complete.length() < 1){
            complete.append("暂无");
        }
        i = 1;
        for (CustomerFeatureResponse.Question item : incompleteStatus) {
            incomplete.append(i++).append(". ").append(item.getMessage()).append("\n");
        }
        if (incomplete.length() < 1){
            incomplete.append("暂无");
        }
        String rateDesc;
        if (customerInfo.getConversionRate().equals("low")){
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
        latestCustomerCharacter.setUpdateTime(customerInfo.getUpdateTime());
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
            if (!StringUtils.isEmpty(character.getConversionRate())){
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

            if (ownerSummaryMessages.containsKey(character.getOwnerName())){
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
                for (String key : messages.getAdvantages().keySet()){
                    messages.getAdvantages().put(key,
                            messages.getAdvantages().get(key) + item.getAdvantages().get(key));
                }
                for (String key : messages.getQuestions().keySet()){
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

    /**
     * 根据组员名称获取组长的企微消息推送url
     * @param ownerName
     * @return
     */
    private String getLeaderMessageUrl(String ownerName){
        QueryWrapper<Config> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("type", ConfigTypeEnum.COMMON.getValue());
        queryWrapper.eq("name", ConfigTypeEnum.LEADER.getValue());
        Config config = configMapper.selectOne(queryWrapper);
        if (Objects.isNull(config)) {
            log.error("没有配置组长信息，请先配置");
            return null;
        }
        List<LeadMemberRequest> leadMemberList = JsonUtil.readValue(config.getValue(), new TypeReference<List<LeadMemberRequest>>() {
        });
        return null;
    }

}
