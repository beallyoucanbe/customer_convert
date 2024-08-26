package com.smart.sso.server.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.google.common.collect.ImmutableMap;
import com.smart.sso.server.constant.AppConstant;
import com.smart.sso.server.enums.ConfigTypeEnum;
import com.smart.sso.server.enums.EarningDesireEnum;
import com.smart.sso.server.enums.FundsVolumeEnum;
import com.smart.sso.server.enums.ProfitLossEnum;
import com.smart.sso.server.mapper.ConfigMapper;
import com.smart.sso.server.mapper.CustomerCharacterMapper;
import com.smart.sso.server.mapper.CustomerInfoMapper;
import com.smart.sso.server.model.Config;
import com.smart.sso.server.model.CustomerCharacter;
import com.smart.sso.server.model.CustomerInfo;
import com.smart.sso.server.model.CustomerStageStatus;
import com.smart.sso.server.model.TextMessage;
import com.smart.sso.server.model.VO.CustomerProfile;
import com.smart.sso.server.model.dto.CustomerFeatureResponse;
import com.smart.sso.server.model.dto.CustomerProcessSummaryResponse;
import com.smart.sso.server.model.dto.LeadMemberRequest;
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
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
    private ConfigMapper configMapper;
    @Autowired
    private CustomerInfoMapper customerInfoMapper;

    ImmutableMap<String, String> conversionRateMap = ImmutableMap.<String, String>builder().put("incomplete", "未完成判断").put("low", "较低").put("medium", "中等").put("high", "较高").build();

    @Override
    public String sendMessageToChat(String url, TextMessage message) {
        // 创建请求头
        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/json");
        log.error("发送消息内容：" + JsonUtil.serialize(message));
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
        CustomerCharacter newCustomerCharacter = new CustomerCharacter();
        updateCharacter(newCustomerCharacter, customerInfo, customerProfile, customerFeature, customerSummary);
        if (Objects.isNull(customerCharacter)) {
            // 新建
            customerCharacterMapper.insert(newCustomerCharacter);
        } else {
            // 更新
            if (!areEqual(customerCharacter, newCustomerCharacter)){
                customerCharacterMapper.updateById(newCustomerCharacter);
            }
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
        queryWrapper1.gt("update_time", dateTime);
        List<CustomerCharacter> characterList = customerCharacterMapper.selectList(queryWrapper1);
        Map<String, Integer> questions = new LinkedHashMap<>();
        questions.put("未完成客户匹配度判断", 0);
        questions.put("跟进错的客户", 0);
        questions.put("未完成客户交易风格了解", 0);
        questions.put("未完成针对性介绍功能", 0);
        questions.put("客户对老师和课程不认可", 0);
        questions.put("客户对软件功能不理解", 0);
        questions.put("客户对选股方法不认可", 0);
        questions.put("客户对自身问题不认可", 0);
        questions.put("客户对软件价值不认可", 0);
        questions.put("客户拒绝购买", 0);
        Map<String, Integer> advantages = new LinkedHashMap<>();
        advantages.put("完成客户匹配度判断", 0);
        advantages.put("完成客户交易风格了解", 0);
        advantages.put("客户认可老师和课程", 0);
        advantages.put("客户理解了软件功能", 0);
        advantages.put("客户认可选股方法", 0);
        advantages.put("客户认可自身问题", 0);
        advantages.put("客户认可软件价值", 0);
        advantages.put("客户确认购买", 0);
        advantages.put("客户完成购买", 0);
        // 对获取的所有客户进行总结
        execute(characterList, questions, advantages);

        StringBuilder complete = new StringBuilder();
        StringBuilder incomplete = new StringBuilder();
        int i = 1;
        for (Map.Entry<String, Integer> item : advantages.entrySet()) {
            if (item.getValue() == 0) {
                continue;
            }
            complete.append(i++).append(". ").append(item.getKey()).append("：过去半日共计").append(item.getValue()).append("个\n");
        }
        i = 1;
        for (Map.Entry<String, Integer> item : questions.entrySet()) {
            if (item.getValue() == 0) {
                continue;
            }
            incomplete.append(i++).append(". ").append(item.getKey()).append("：过去半日共计").append(item.getValue()).append("个\n");
        }
        String url = "http://172.16.192.61:8086/share/33/dashboard/1";
        String message = String.format(AppConstant.LEADER_SUMMARY_MARKDOWN_TEMPLATE, DateUtil.getFormatCurrentTime("yyyy-MM-dd HH:mm"), incomplete, complete, url, url);

        // 获取要发送的url
        QueryWrapper<Config> queryWrapper2 = new QueryWrapper<>();
        queryWrapper2.eq("type", ConfigTypeEnum.NOTIFY_URL.getValue());
        queryWrapper2.eq("name", area);
        Config config = configMapper.selectOne(queryWrapper2);
        String notifyUrl;
        if (Objects.isNull(config)) {
            log.error("没有配置该销售的报警url，暂不发送");
            return;
        } else {
            notifyUrl = config.getValue();
        }
        TextMessage textMessage = new TextMessage();
        TextMessage.TextContent textContent = new TextMessage.TextContent();
        textContent.setContent(message);
        textMessage.setMsgtype("markdown");
        textMessage.setMarkdown(textContent);
        sendMessageToChat(notifyUrl, textMessage);
    }

    @Override
    public void updateCustomerCharacter(String id) {
        CustomerInfo customerInfo = customerInfoMapper.selectById(id);
        CustomerProfile customerProfile = customerInfoService.queryCustomerById(id);
        CustomerFeatureResponse customerFeature = customerInfoService.queryCustomerFeatureById(id);
        CustomerProcessSummaryResponse customerSummary = customerInfoService.queryCustomerProcessSummaryById(id);
        CustomerCharacter customerCharacter = customerCharacterMapper.selectById(id);
        CustomerCharacter newCustomerCharacter = new CustomerCharacter();
        updateCharacter(newCustomerCharacter, customerInfo, customerProfile, customerFeature, customerSummary);
        if (Objects.isNull(customerCharacter)) {
            // 新建
            customerCharacterMapper.insert(newCustomerCharacter);
        } else {
            // 更新
            if (!areEqual(customerCharacter, newCustomerCharacter)){
                customerCharacterMapper.updateById(newCustomerCharacter);
            }
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
            if ("createTime".equals(field.getName())) {
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
        CustomerProfile customerProfile = customerInfoService.queryCustomerById(id);
        CustomerFeatureResponse customerFeature = customerInfoService.queryCustomerFeatureById(id);
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
            if (!(Boolean) recognition.getSoftwarePurchaseAttitude().getModelRecord()) {
                incompleteStatus.add("客户拒绝购买");
            }
        }
        // 优缺点都是空，不发送
        if (CollectionUtils.isEmpty(completeStatus) && CollectionUtils.isEmpty(incompleteStatus)) {
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
        for (String item : incompleteStatus) {
            incomplete.append(i++).append(". ").append(item).append("\n");
        }
        if (incomplete.length() < 1){
            incomplete.append("暂无");
        }
        String rateDesc;
        if (customerProfile.getConversionRate().equals("low")){
            rateDesc = conversionRateMap.get(customerProfile.getConversionRate()) + "（不应重点跟进）";
        } else {
            rateDesc = conversionRateMap.get(customerProfile.getConversionRate());
        }
        String url = String.format("https://newcmp.emoney.cn/chat/api/customer/redirect?customer_id=%s&active_id=%s", customerInfo.getCustomerId(), customerInfo.getCurrentCampaign());
        String message = String.format(AppConstant.CUSTOMER_SUMMARY_MARKDOWN_TEMPLATE, customerProfile.getCustomerName(),
                customerInfo.getCustomerId(),
                rateDesc,
                complete,
                incomplete,
                url, url);

        QueryWrapper<Config> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("type", ConfigTypeEnum.NOTIFY_URL.getValue());
        queryWrapper.eq("name", customerInfo.getOwnerName());
        Config config = configMapper.selectOne(queryWrapper);
        String notifyUrl = "";
        if (Objects.isNull(config)) {
            log.error("没有配置该销售的报警url，暂不发送");
            return;
        } else {
            notifyUrl = config.getValue();
        }
        TextMessage textMessage = new TextMessage();
        TextMessage.TextContent textContent = new TextMessage.TextContent();
        textContent.setContent(message);
        textMessage.setMsgtype("markdown");
        textMessage.setMarkdown(textContent);
        sendMessageToChat(notifyUrl, textMessage);
    }


    private void updateCharacter(CustomerCharacter latestCustomerCharacter, CustomerInfo customerInfo,
                                 CustomerProfile customerProfile, CustomerFeatureResponse customerFeature, CustomerProcessSummaryResponse customerSummary) {
        latestCustomerCharacter.setId(customerInfo.getId());
        latestCustomerCharacter.setCustomerId(customerInfo.getCustomerId());
        latestCustomerCharacter.setCustomerName(customerInfo.getCustomerName());
        latestCustomerCharacter.setOwnerId(customerInfo.getOwnerId());
        latestCustomerCharacter.setOwnerName(customerInfo.getOwnerName());
        latestCustomerCharacter.setCurrentCampaign(customerProfile.getCurrentCampaign());
        latestCustomerCharacter.setConversionRate(conversionRateMap.get(customerProfile.getConversionRate()));

        latestCustomerCharacter.setMatchingJudgmentStage(customerProfile.getCustomerStage().getMatchingJudgment() == 1);
        latestCustomerCharacter.setTransactionStyleStage(customerProfile.getCustomerStage().getTransactionStyle() == 1);
        latestCustomerCharacter.setFunctionIntroductionStage(customerProfile.getCustomerStage().getFunctionIntroduction() ==
                1);
        latestCustomerCharacter.setConfirmValueStage(customerProfile.getCustomerStage().getConfirmValue() == 1);
        latestCustomerCharacter.setConfirmPurchaseStage(customerProfile.getCustomerStage().getConfirmPurchase() == 1);
        latestCustomerCharacter.setCompletePurchaseStage(customerProfile.getCustomerStage().getCompletePurchase() == 1);

        latestCustomerCharacter.setFundsVolume(FundsVolumeEnum.getTextByValue(
                Objects.nonNull(customerFeature.getBasic().getFundsVolume().getCompareValue()) ? customerFeature.getBasic().getFundsVolume().getCompareValue().toString() : null));
        latestCustomerCharacter.setProfitLossSituation(ProfitLossEnum.getTextByValue(
                Objects.nonNull(customerFeature.getBasic().getProfitLossSituation().getCompareValue()) ? customerFeature.getBasic().getProfitLossSituation().getCompareValue().toString() : null));
        latestCustomerCharacter.setEarningDesire(EarningDesireEnum.getTextByValue(
                Objects.nonNull(customerFeature.getBasic().getEarningDesire().getCompareValue()) ? customerFeature.getBasic().getEarningDesire().getCompareValue().toString() : null));

        latestCustomerCharacter.setCourseTeacherApproval(Objects.nonNull(customerFeature.getRecognition().getCourseTeacherApproval().getCompareValue()) ? customerFeature.getRecognition().getCourseTeacherApproval().getCompareValue().toString() : null);
        latestCustomerCharacter.setSoftwareFunctionClarity(Objects.nonNull(customerFeature.getRecognition().getSoftwareFunctionClarity().getCompareValue()) ? customerFeature.getRecognition().getSoftwareFunctionClarity().getCompareValue().toString() : null);
        latestCustomerCharacter.setStockSelectionMethod(Objects.nonNull(customerFeature.getRecognition().getStockSelectionMethod().getCompareValue()) ? customerFeature.getRecognition().getStockSelectionMethod().getCompareValue().toString() : null);
        latestCustomerCharacter.setSelfIssueRecognition(Objects.nonNull(customerFeature.getRecognition().getSelfIssueRecognition().getCompareValue()) ? customerFeature.getRecognition().getSelfIssueRecognition().getCompareValue().toString() : null);
        latestCustomerCharacter.setSoftwareValueApproval(Objects.nonNull(customerFeature.getRecognition().getSoftwareValueApproval().getCompareValue()) ? customerFeature.getRecognition().getSoftwareValueApproval().getCompareValue().toString() : null);
        latestCustomerCharacter.setSoftwarePurchaseAttitude(
                Objects.nonNull(customerFeature.getRecognition().getSoftwarePurchaseAttitude().getCompareValue()) ? customerFeature.getRecognition().getSoftwarePurchaseAttitude().getCompareValue().toString() : null);
        latestCustomerCharacter.setContinuousLearnApproval(Objects.nonNull(customerFeature.getRecognition().getContinuousLearnApproval().getCompareValue()) ? customerFeature.getRecognition().getContinuousLearnApproval().getCompareValue().toString() : null);
        latestCustomerCharacter.setLearnNewMethodApproval(Objects.nonNull(customerFeature.getRecognition().getLearnNewMethodApproval().getCompareValue()) ? customerFeature.getRecognition().getLearnNewMethodApproval().getCompareValue().toString() : null);

        List<String> advantages = customerSummary.getSummary().getAdvantage();
        List<String> questions = customerSummary.getSummary().getQuestions();
        for (String item : advantages) {
            if (item.contains("完成客户匹配度判断")) {
                latestCustomerCharacter.setSummaryMatchJudgment("true");
            } else if (item.contains("完成客户交易风格了解")) {
                latestCustomerCharacter.setSummaryTransactionStyle("true");
            } else if (item.contains("跟进对的客户")) {
                latestCustomerCharacter.setSummaryFollowCustomer("true");
            } else if (item.contains("功能讲解让客户理解")) {
                latestCustomerCharacter.setSummaryFunctionIntroduction("true");
            } else if (item.contains("成功让客户认可价值")) {
                latestCustomerCharacter.setSummaryConfirmValue("true");
            } else if (item.contains("执行顺序正确")) {
                latestCustomerCharacter.setSummaryExecuteOrder("true");
            } else if (item.contains("邀约听课成功")) {
                latestCustomerCharacter.setSummaryInvitCourse("true");
            }
        }
        for (String item : questions) {
            if (item.contains("未完成客户匹配度判断")) {
                latestCustomerCharacter.setSummaryMatchJudgment("false");
            } else if (item.contains("未完成客户交易风格了解")) {
                latestCustomerCharacter.setSummaryTransactionStyle("false");
            } else if (item.contains("跟进错的客户")) {
                latestCustomerCharacter.setSummaryFollowCustomer("false");
            } else if (item.contains("功能讲解未让客户理解")) {
                latestCustomerCharacter.setSummaryFunctionIntroduction("false");
            } else if (item.contains("未让客户认可价值")) {
                latestCustomerCharacter.setSummaryConfirmValue("false");
            } else if (item.contains("执行顺序错误")) {
                latestCustomerCharacter.setSummaryExecuteOrder("false");
            } else if (item.contains("邀约听课失败")) {
                latestCustomerCharacter.setSummaryInvitCourse("false");
            }
        }
        // 总结质疑应对中
        int questionCount = 0;
        CustomerProcessSummaryResponse.ProcessApprovalAnalysis approvalAnalysis = customerSummary.getApprovalAnalysis();
        if (Objects.nonNull(approvalAnalysis)) {
            if (Objects.nonNull(approvalAnalysis.getMethod()) && !CollectionUtils.isEmpty(approvalAnalysis.getMethod().getChats())){
                questionCount += approvalAnalysis.getMethod().getChats().size();
            }
            if (Objects.nonNull(approvalAnalysis.getIssue()) && !CollectionUtils.isEmpty(approvalAnalysis.getIssue().getChats())){
                questionCount += approvalAnalysis.getIssue().getChats().size();
            }
            if (Objects.nonNull(approvalAnalysis.getValue()) && !CollectionUtils.isEmpty(approvalAnalysis.getValue().getChats())){
                questionCount += approvalAnalysis.getValue().getChats().size();
            }
            if (Objects.nonNull(approvalAnalysis.getPrice()) && !CollectionUtils.isEmpty(approvalAnalysis.getPrice().getChats())){
                questionCount += approvalAnalysis.getPrice().getChats().size();
            }
            if (Objects.nonNull(approvalAnalysis.getPurchase()) && !CollectionUtils.isEmpty(approvalAnalysis.getPurchase().getChats())){
                questionCount += approvalAnalysis.getPurchase().getChats().size();
            }
            if (Objects.nonNull(approvalAnalysis.getSoftwareOperation()) && !CollectionUtils.isEmpty(approvalAnalysis.getSoftwareOperation().getChats())){
                questionCount += approvalAnalysis.getSoftwareOperation().getChats().size();
            }
            if (Objects.nonNull(approvalAnalysis.getCourse()) && !CollectionUtils.isEmpty(approvalAnalysis.getCourse().getChats())){
                questionCount += approvalAnalysis.getCourse().getChats().size();
            }
            if (Objects.nonNull(approvalAnalysis.getNoMoney()) && !CollectionUtils.isEmpty(approvalAnalysis.getNoMoney().getChats())){
                questionCount += approvalAnalysis.getNoMoney().getChats().size();
            }
            if (Objects.nonNull(approvalAnalysis.getOthers()) && !CollectionUtils.isEmpty(approvalAnalysis.getOthers().getChats())){
                questionCount += approvalAnalysis.getOthers().getChats().size();
            }
        }
        latestCustomerCharacter.setQuestionCount(questionCount);
        latestCustomerCharacter.setUpdateTime(customerInfo.getUpdateTime());
    }


    private void execute(List<CustomerCharacter> characterList, Map<String, Integer> questions, Map<String, Integer> advantages) {
        if (CollectionUtils.isEmpty(characterList)) {
            return;
        }
        for (CustomerCharacter character : characterList) {
            sendMessage(character.getId());
            if (character.getMatchingJudgmentStage()) {
                advantages.merge("完成客户匹配度判断", 1, Integer::sum);
            } else {
                questions.merge("未完成客户匹配度判断", 1, Integer::sum);
            }
            if (character.getTransactionStyleStage()) {
                advantages.merge("完成客户交易风格了解", 1, Integer::sum);
            } else {
                questions.merge("未完成客户交易风格了解", 1, Integer::sum);
            }
            if (character.getFunctionIntroductionStage()) {
                advantages.merge("完成客户交易风格了解", 1, Integer::sum);
            } else {
                questions.merge("未完成针对性介绍功能", 1, Integer::sum);
            }
            if (character.getConfirmPurchaseStage()) {
                advantages.merge("客户确认购买", 1, Integer::sum);
            }
            if (character.getCompletePurchaseStage()) {
                advantages.merge("客户完成购买", 1, Integer::sum);
            }

            if (!StringUtils.isEmpty(character.getCourseTeacherApproval()) &&
                    Boolean.parseBoolean(character.getCourseTeacherApproval())) {
                advantages.merge("客户认可老师和课程", 1, Integer::sum);
            } else if (!StringUtils.isEmpty(character.getCourseTeacherApproval()) &&
                    !Boolean.parseBoolean(character.getCourseTeacherApproval())) {
                questions.merge("客户对老师和课程不认可", 1, Integer::sum);
            }
            if (!StringUtils.isEmpty(character.getSoftwareFunctionClarity()) &&
                    Boolean.parseBoolean(character.getSoftwareFunctionClarity())) {
                advantages.merge("客户理解了软件功能", 1, Integer::sum);
            } else if (!StringUtils.isEmpty(character.getSoftwareFunctionClarity()) &&
                    !Boolean.parseBoolean(character.getSoftwareFunctionClarity())) {
                questions.merge("客户对软件功能不理解", 1, Integer::sum);
            }
            if (!StringUtils.isEmpty(character.getStockSelectionMethod()) &&
                    Boolean.parseBoolean(character.getStockSelectionMethod())) {
                advantages.merge("客户认可选股方法", 1, Integer::sum);
            } else if (!StringUtils.isEmpty(character.getStockSelectionMethod()) &&
                    !Boolean.parseBoolean(character.getStockSelectionMethod())) {
                questions.merge("客户对选股方法不认可", 1, Integer::sum);
            }
            if (!StringUtils.isEmpty(character.getSelfIssueRecognition()) &&
                    Boolean.parseBoolean(character.getSelfIssueRecognition())) {
                advantages.merge("客户认可自身问题", 1, Integer::sum);
            } else if (!StringUtils.isEmpty(character.getSelfIssueRecognition()) &&
                    !Boolean.parseBoolean(character.getSelfIssueRecognition())) {
                questions.merge("客户对自身问题不认可", 1, Integer::sum);
            }
            if (!StringUtils.isEmpty(character.getSoftwareValueApproval()) &&
                    Boolean.parseBoolean(character.getSoftwareValueApproval())) {
                advantages.merge("客户认可软件价值", 1, Integer::sum);
            } else if (!StringUtils.isEmpty(character.getSoftwareValueApproval()) &&
                    !Boolean.parseBoolean(character.getSoftwareValueApproval())) {
                questions.merge("客户对软件价值不认可", 1, Integer::sum);
            }
            if (!StringUtils.isEmpty(character.getSoftwarePurchaseAttitude()) &&
                    !Boolean.parseBoolean(character.getSoftwarePurchaseAttitude())) {
                questions.merge("客户拒绝购买", 1, Integer::sum);
            }
            if (!StringUtils.isEmpty(character.getSummaryFollowCustomer()) &&
                    !Boolean.parseBoolean(character.getSummaryFollowCustomer())) {
                questions.merge("跟进错的客户", 1, Integer::sum);
            }
        }
    }
}
