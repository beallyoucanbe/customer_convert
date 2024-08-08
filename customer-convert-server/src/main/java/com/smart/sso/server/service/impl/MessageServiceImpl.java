package com.smart.sso.server.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.google.common.collect.ImmutableMap;
import com.smart.sso.server.constant.AppConstant;
import com.smart.sso.server.mapper.CustomerCharacterMapper;
import com.smart.sso.server.mapper.CustomerInfoMapper;
import com.smart.sso.server.mapper.NotifyRelationMapper;
import com.smart.sso.server.model.CustomerCharacter;
import com.smart.sso.server.model.CustomerInfo;
import com.smart.sso.server.model.CustomerStageStatus;
import com.smart.sso.server.model.NotifyRelation;
import com.smart.sso.server.model.TextMessage;
import com.smart.sso.server.model.VO.CustomerProfile;
import com.smart.sso.server.model.dto.CustomerFeatureResponse;
import com.smart.sso.server.model.dto.CustomerProcessSummaryResponse;
import com.smart.sso.server.service.CustomerInfoService;
import com.smart.sso.server.service.MessageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpHeaders;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


@Service
@Slf4j
public class MessageServiceImpl implements MessageService {

    @Autowired
    private RestTemplate restTemplate;
    @Autowired
    private CustomerInfoService customerInfoService;
    @Autowired
    private CustomerCharacterMapper customerCharacterMapper;
    @Autowired
    private NotifyRelationMapper notifyRelationMapper;
    @Autowired
    private CustomerInfoMapper customerInfoMapper;

    ImmutableMap<String, String> conversionRateMap = ImmutableMap.<String, String>builder().put("incomplete", "未完成判断").put("low", "较低").put("medium", "中等").put("high", "较高").build();

    @Override
    public String sendMessageToChat(String url, TextMessage message) {
        // 创建请求头
        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/json");

        // 创建请求实体
        HttpEntity<TextMessage> requestEntity = new HttpEntity<>(message, headers);

        // 发送 POST 请求
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, requestEntity, String.class);

        // 处理响应
        if (response.getStatusCode() == HttpStatus.OK) {
            return response.getBody();
        } else {
            throw new RuntimeException("Failed to send message: " + response.getStatusCode());
        }
    }

    /**
     * 消息内容示例（列表里的值出自“阶段”的值和“客户认可度对应的模型记录”的值：
     * 您刚和客户XXX通完电话，该客户的匹配度较高/中等/较低（不应重点跟进）/未完成判断。
     * 截至本次通话已完成：
     * 1、客户交易风格了解
     * 2、客户认可老师和课程
     * 3、客户理解了软件功能
     * 4、客户认可选股方法
     * 5、客户认可自身问题
     * 6、客户认可软件价值
     * 7、客户确认购买
     * 8、客户完成购买
     * 截至本次通话遗留问题，待下次通话解决：
     * 1、客户对老师和课程不认可
     * 2、客户对软件功能不理解
     * 3、客户对选股方法不认可
     * 4、客户对自身问题不认可
     * 5、客户对软件价值不认可
     * 6、客户拒绝购买
     * <p>
     * 详细内容链接：http://xxxxxxxxx（嵌入天网的该客户详情页链接）
     *
     * @param id
     * @return
     */
    @Override
    public void sendNoticeForSingle(String id) {
        CustomerInfo customerInfo = customerInfoMapper.selectById(id);
        CustomerProfile customerProfile = customerInfoService.queryCustomerById(id);
        CustomerFeatureResponse customerFeature = customerInfoService.queryCustomerFeatureById(id);
        CustomerProcessSummaryResponse customerSummary = customerInfoService.queryCustomerProcessSummaryById(id);
        CustomerCharacter customerCharacter = customerCharacterMapper.selectById(id);
        if (Objects.isNull(customerCharacter)) {
            // 新建
            CustomerCharacter newCustomerCharacter = new CustomerCharacter();
            newCustomerCharacter.setId(id);
            newCustomerCharacter.setCustomerId(customerInfo.getCustomerId());
            newCustomerCharacter.setOwnerId(customerInfo.getOwnerId());
            newCustomerCharacter.setCurrentCampaign(customerProfile.getCurrentCampaign());
            newCustomerCharacter.setConversionRate(customerProfile.getConversionRate());

            newCustomerCharacter.setMatchingJudgmentStage(customerProfile.getCustomerStage().getMatchingJudgment() == 1);
            newCustomerCharacter.setTransactionStyleStage(customerProfile.getCustomerStage().getTransactionStyle() == 1);
            newCustomerCharacter.setFunctionIntroductionStage(customerProfile.getCustomerStage().getFunctionIntroduction() == 1);
            newCustomerCharacter.setConfirmValueStage(customerProfile.getCustomerStage().getConfirmValue() == 1);
            newCustomerCharacter.setConfirmPurchaseStage(customerProfile.getCustomerStage().getConfirmPurchase() == 1);
            newCustomerCharacter.setCompletePurchaseStage(customerProfile.getCustomerStage().getCompletePurchase() == 1);

            newCustomerCharacter.setFundsVolume(Objects.nonNull(customerFeature.getBasic().getFundsVolume().getModelRecord()) ? customerFeature.getBasic().getFundsVolume().getModelRecord().toString() : null);
            newCustomerCharacter.setProfitLossSituation(Objects.nonNull(customerFeature.getBasic().getProfitLossSituation().getModelRecord()) ? customerFeature.getBasic().getProfitLossSituation().getModelRecord().toString() : null);
            newCustomerCharacter.setEarningDesire(Objects.nonNull(customerFeature.getBasic().getEarningDesire().getModelRecord()) ? customerFeature.getBasic().getEarningDesire().getModelRecord().toString() : null);


            customerCharacterMapper.insert(newCustomerCharacter);
        } else {

            customerCharacterMapper.updateById(customerCharacter);
        }
        sendMessage(customerInfo, customerProfile, customerFeature);
    }

    @Override
    public void sendNoticeForLeader(String id) {

    }

    private void sendMessage(CustomerInfo customerInfo, CustomerProfile customerProfile, CustomerFeatureResponse customerFeature) {

        CustomerFeatureResponse.Recognition recognition = customerFeature.getRecognition();
        List<String> completeStatus = new ArrayList<>();
        List<String> incompleteStatus = new ArrayList<>();

        CustomerStageStatus customerStage = customerProfile.getCustomerStage();
        if (customerStage.getTransactionStyle() == 1) {
            completeStatus.add("客户交易风格了解");
        }

        if (Objects.nonNull(recognition.getCourseTeacherApproval().getModelRecord())) {
            if ((Boolean) recognition.getCourseTeacherApproval().getModelRecord()) {
                completeStatus.add("客户认可老师和课程");
            } else {
                incompleteStatus.add("客户对老师和课程不认可");
            }
        }
        if (Objects.nonNull(recognition.getSoftwareFunctionClarity().getModelRecord())) {
            if ((Boolean) recognition.getSoftwareFunctionClarity().getModelRecord()) {
                completeStatus.add("客户理解了软件功能");
            } else {
                incompleteStatus.add("客户对软件功能不理解");
            }
        }
        if (Objects.nonNull(recognition.getStockSelectionMethod().getModelRecord())) {
            if ((Boolean) recognition.getStockSelectionMethod().getModelRecord()) {
                completeStatus.add("客户认可选股方法");
            } else {
                incompleteStatus.add("客户对选股方法不认可");
            }
        }
        if (Objects.nonNull(recognition.getSelfIssueRecognition().getModelRecord())) {
            if ((Boolean) recognition.getSelfIssueRecognition().getModelRecord()) {
                completeStatus.add("客户认可自身问题");
            } else {
                incompleteStatus.add("客户对自身问题不认可");
            }
        }
        if (Objects.nonNull(recognition.getSoftwareValueApproval().getModelRecord())) {
            if ((Boolean) recognition.getSoftwareValueApproval().getModelRecord()) {
                completeStatus.add("客户认可软件价值");
            } else {
                incompleteStatus.add("客户对软件价值不认可");
            }
        }
        if (customerStage.getConfirmPurchase() == 1) {
            completeStatus.add("客户完成购买");
        }

        if (Objects.nonNull(recognition.getSoftwarePurchaseAttitude().getModelRecord())) {
            if (!(Boolean) recognition.getSoftwareValueApproval().getModelRecord()) {
                incompleteStatus.add("客户拒绝购买");
            }
        }

        StringBuilder complete = new StringBuilder();
        StringBuilder incomplete = new StringBuilder();
        int i = 1;
        for (String item : completeStatus) {
            complete.append(i++).append(". ").append(item).append("\n");
        }
        i = 1;
        for (String item : incompleteStatus) {
            incomplete.append(i++).append(". ").append(item).append("\n");
        }
        String url = "https://newcmp.emoney.cn/chat/customer?id=" + customerInfo.getId();
        String message = String.format(AppConstant.CUSTOMER_SUMMARY_MARKDOWN_TEMPLATE, customerProfile.getCustomerName(),
                conversionRateMap.get(customerProfile.getConversionRate()),
                complete,
                incomplete,
                url, url);


        QueryWrapper<NotifyRelation> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("owner_id", customerInfo.getOwnerId());
        NotifyRelation notifyRelation = notifyRelationMapper.selectOne(queryWrapper);
        String notifyUrl = "";
        if (Objects.isNull(notifyRelation)) {
            log.error("没有配置该销售的报警url，使用默认的报警配置");
            notifyUrl = "";
        } else {
            notifyUrl = notifyRelation.getUrl();
        }
        TextMessage textMessage = new TextMessage();
        TextMessage.TextContent textContent = new TextMessage.TextContent();
        textContent.setContent(message);
        textMessage.setMsgType("markdown");
        textMessage.setMarkdown(textContent);
        sendMessageToChat(notifyUrl, textMessage);
    }
}
