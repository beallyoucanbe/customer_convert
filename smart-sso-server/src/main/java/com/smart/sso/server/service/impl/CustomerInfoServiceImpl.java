package com.smart.sso.server.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smart.sso.server.enums.EarningDesireEnum;
import com.smart.sso.server.enums.FundsVolumeEnum;
import com.smart.sso.server.enums.ProfitLossEnum;
import com.smart.sso.server.mapper.CustomerFeatureMapper;
import com.smart.sso.server.mapper.CustomerInfoMapper;
import com.smart.sso.server.mapper.CustomerSummaryMapper;
import com.smart.sso.server.model.*;
import com.smart.sso.server.model.VO.CustomerListVO;
import com.smart.sso.server.model.VO.CustomerProfile;
import com.smart.sso.server.model.dto.CustomerFeatureResponse;
import com.smart.sso.server.model.dto.CustomerInfoListRequest;
import com.smart.sso.server.model.dto.CustomerInfoListResponse;
import com.smart.sso.server.model.dto.CustomerProcessSummaryResponse;
import com.smart.sso.server.service.CustomerInfoService;
import com.smart.sso.server.util.JsonUtil;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.smart.sso.server.util.CommonUtils.deletePunctuation;

@Service
public class CustomerInfoServiceImpl implements CustomerInfoService {

    @Autowired
    private CustomerInfoMapper customerInfoMapper;
    @Autowired
    private CustomerFeatureMapper customerFeatureMapper;
    @Autowired
    private CustomerSummaryMapper customerSummaryMapper;

    @Override
    public CustomerInfoListResponse queryCustomerInfoList(CustomerInfoListRequest params) {
        Page<CustomerInfo> selectPage = new Page<>(params.getPage(), params.getLimit());
        QueryWrapper<CustomerInfo> queryWrapper = new QueryWrapper<>();

        if (!StringUtils.isEmpty(params.getCustomerName())) {
            queryWrapper.like("customer_name", params.getCustomerName());
        }
        if (!StringUtils.isEmpty(params.getOwnerName())) {
            queryWrapper.like("owner_name", params.getOwnerName());
        }
        if (!StringUtils.isEmpty(params.getConversionRate())) {
            queryWrapper.eq("conversion_rate", params.getConversionRate());
        }
        if (!StringUtils.isEmpty(params.getCurrentCampaign())) {
            queryWrapper.like("current_campaign", params.getCurrentCampaign());
        }

        String sortOrder = params.getSortBy();
        boolean isAsc = "asc".equalsIgnoreCase(params.getOrder());
        if ("conversion_rate".equals(sortOrder)) {
            queryWrapper.last("ORDER BY FIELD(conversion_rate, 'incomplete', 'low', 'medium', 'high') " + (isAsc ? "ASC" : "DESC"));
        } else {
            queryWrapper.orderBy(true, isAsc, sortOrder);
        }
        Page<CustomerInfo> resultPage = customerInfoMapper.selectPage(selectPage, queryWrapper);
        CustomerInfoListResponse result = new CustomerInfoListResponse();
        result.setTotal(resultPage.getTotal());
        result.setLimit(params.getLimit());
        result.setOffset(params.getPage());
        result.setCustomers(convert(resultPage.getRecords()));
        return result;
    }

    @Override
    public CustomerProfile queryCustomerById(String id) {
        CustomerInfo customerInfo = customerInfoMapper.selectById(id);
        CustomerFeature customerFeature = customerFeatureMapper.selectById(id);
        CustomerSummary customerSummary = customerSummaryMapper.selectById(id);
        CustomerProfile customerProfile = convert2CustomerProfile(customerInfo);
        customerProfile.setCustomerStage(getCustomerStageStatus(customerFeature, customerSummary));
        return customerProfile;
    }

    @Override
    public CustomerFeatureResponse queryCustomerFeatureById(String id) {
        CustomerFeature customerFeature = customerFeatureMapper.selectById(id);
        return convert2CustomerFeatureResponse(customerFeature);
    }

    @Override
    public CustomerProcessSummaryResponse queryCustomerProcessSummaryById(String id) {
        CustomerSummary customerSummary = customerSummaryMapper.selectById(id);
        CustomerFeature customerFeature = customerFeatureMapper.selectById(id);
        CustomerInfo customerInfo = customerInfoMapper.selectById(id);
        CustomerProcessSummaryResponse summaryResponse = convert2CustomerProcessSummaryResponse(customerSummary);
        summaryResponse.setSummary(getProcessSummary(customerFeature, customerInfo));
        return summaryResponse;
    }

    @Override
    public String getConversionRate(CustomerFeature customerFeature) {
        // "high", "medium", "low", "incomplete"
        // -较高：资金体量=“充裕”或“大于等于10万” and 赚钱欲望=“高”
        // -中等：(资金体量=“匮乏”或“小于10万” and 赚钱欲望=“高”) or (资金体量=“充裕”或“大于等于10万” and 赚钱欲望=“低”)
        // -较低：资金体量=“匮乏”或“小于10万” and 赚钱欲望=“低”
        // -未完成判断：资金体量=空 or 赚钱欲望=空
        String result = "incomplete";
        List<FeatureContent> fundsVolumeModel = customerFeature.getFundsVolumeModel();
        List<FeatureContent> earningDesireModel = customerFeature.getEarningDesireModel();
        if (CollectionUtils.isEmpty(fundsVolumeModel) || CollectionUtils.isEmpty(earningDesireModel)) {
            return result;
        }
        String fundsVolume = null;
        String earningDesire = null;

        // 找到最后一个非null的值
        for (int i = fundsVolumeModel.size() - 1; i >= 0; i--) {
            if (!StringUtils.isEmpty(fundsVolumeModel.get(i).getAnswer())) {
                fundsVolume = fundsVolumeModel.get(i).getAnswer();
                break;
            }
        }
        for (int i = earningDesireModel.size() - 1; i >= 0; i--) {
            if (!StringUtils.isEmpty(earningDesireModel.get(i).getAnswer())) {
                earningDesire = earningDesireModel.get(i).getAnswer();
                break;
            }
        }
        if (StringUtils.isEmpty(fundsVolume) || StringUtils.isEmpty(earningDesire)) {
            return result;
        }

        if ((fundsVolume.equals("充裕") || fundsVolume.equals("大于等于10万")) && earningDesire.equals("高")) {
            return "high";
        }
        if ((fundsVolume.equals("匮乏") || fundsVolume.equals("小于10万")) && earningDesire.equals("高")) {
            return "medium";
        }
        if ((fundsVolume.equals("充裕") || fundsVolume.equals("大于等于10万")) && earningDesire.equals("低")) {
            return "medium";
        }
        if ((fundsVolume.equals("匮乏") || fundsVolume.equals("小于10万")) && earningDesire.equals("低")) {
            return "low";
        }
        return result;
    }

    @Override
    public CustomerStageStatus getCustomerStageStatus(CustomerFeature customerFeature, CustomerSummary customerSummary) {
        CustomerFeatureResponse customerFeatureResponse = convert2CustomerFeatureResponse(customerFeature);
        CustomerProcessSummaryResponse summaryResponse = convert2CustomerProcessSummaryResponse(customerSummary);
        CustomerStageStatus stageStatus = new CustomerStageStatus();
        // 客户匹配度判断 值不为“未完成判断”
        if (!"incomplete".equals(getConversionRate(customerFeature))) {
            stageStatus.setMatchingJudgment(1);
        }
        // 客户交易风格了解 相关字段全部有值——“客户当前持仓或关注的股票”、“客户为什么买这些股票”、“客户怎么决定的买卖这些股票的时机”、“客户的交易风格”、“客户的股龄”
        CustomerFeatureResponse.TradingMethod tradingMethod = customerFeatureResponse.getTradingMethod();
        if (Objects.nonNull(tradingMethod.getCurrentStocks().getModelRecord()) && Objects.nonNull(tradingMethod.getStockPurchaseReason().getModelRecord()) && Objects.nonNull(tradingMethod.getTradeTimingDecision().getModelRecord()) && Objects.nonNull(tradingMethod.getTradingStyle().getModelRecord()) && Objects.nonNull(tradingMethod.getStockMarketAge().getModelRecord())) {
            stageStatus.setTransactionStyle(1);
        }
        // 针对性功能介绍 相关字段的值全部为“是”——“销售有结合客户的股票举例”、“销售有基于客户交易风格做针对性的功能介绍”、“销售有点评客户的选股方法”、“销售有点评客户的选股时机”
        CustomerProcessSummaryResponse.ProcessInfoExplanation infoExplanation = summaryResponse.getInfoExplanation();
        if (Objects.nonNull(infoExplanation.getStock()) && (Boolean) infoExplanation.getStock() && Objects.nonNull(infoExplanation.getStockPickReview()) && (Boolean) infoExplanation.getStockPickReview() && Objects.nonNull(infoExplanation.getStockTimingReview()) && (Boolean) infoExplanation.getStockTimingReview() && Objects.nonNull(infoExplanation.getTradeBasedIntro()) && (Boolean) infoExplanation.getTradeBasedIntro()) {
            stageStatus.setFunctionIntroduction(1);
        }
        // 客户确认价值 相关字段的值全部为“是”——“客户对软件功能的清晰度”、“客户对销售讲的选股方法的认可度”、“客户对自身问题及影响的认可度”、“客户对软件价值的认可度”
        CustomerFeatureResponse.Recognition recognition = customerFeatureResponse.getRecognition();
        if (Objects.nonNull(recognition.getSoftwareFunctionClarity().getModelRecord()) && (Boolean) recognition.getSoftwareFunctionClarity().getModelRecord() && Objects.nonNull(recognition.getStockSelectionMethod().getModelRecord()) && (Boolean) recognition.getStockSelectionMethod().getModelRecord() && Objects.nonNull(recognition.getSelfIssueRecognition().getModelRecord()) && (Boolean) recognition.getSelfIssueRecognition().getModelRecord() && Objects.nonNull(recognition.getSoftwareValueApproval().getModelRecord()) && (Boolean) recognition.getSoftwareValueApproval().getModelRecord()) {
            stageStatus.setConfirmValue(1);
        }
        // 客户确认购买 客户对购买软件的态度”的值为“是” or 已支付定金（天网系统取值）
        if (Objects.nonNull(summaryResponse.getApprovalAnalysis().getPurchase()) && !StringUtils.isEmpty(summaryResponse.getApprovalAnalysis().getPurchase().getRecognition()) && "approved".equals(summaryResponse.getApprovalAnalysis().getPurchase().getRecognition())) {
            stageStatus.setConfirmPurchase(1);
        }
        return stageStatus;
    }

    public List<CustomerListVO> convert(List<CustomerInfo> customerInfoList) {
        return customerInfoList.stream().map(item -> {
            CustomerListVO customerListVO = new CustomerListVO();
            BeanUtils.copyProperties(item, customerListVO);
            return customerListVO;
        }).collect(Collectors.toList());
    }

    public CustomerProfile convert2CustomerProfile(CustomerInfo customerInfo) {
        if (Objects.isNull(customerInfo)) {
            return null;
        }
        CustomerProfile customerProfile = new CustomerProfile();
        BeanUtils.copyProperties(customerInfo, customerProfile);
        return customerProfile;
    }

    public CustomerFeatureResponse convert2CustomerFeatureResponse(CustomerFeature customerFeature) {
        if (Objects.isNull(customerFeature)) {
            return null;
        }
        CustomerFeatureResponse customerFeatureResponse = new CustomerFeatureResponse();
        // Profile
        CustomerFeatureResponse.Profile profile = new CustomerFeatureResponse.Profile();
        profile.setCustomerLifecycle(customerFeature.getCustomerLifecycle());
        profile.setHasComputerVersion(customerFeature.getHasComputerVersion());
        profile.setClassCount(customerFeature.getClassCount());
        profile.setPasswordEarnest(customerFeature.getPasswordEarnest());
        profile.setUsageFrequency(customerFeature.getUsageFrequency());
        profile.setClassLength(customerFeature.getClassLength());
        customerFeatureResponse.setProfile(profile);
        // Basic 基本信息
        CustomerFeatureResponse.Basic basic = new CustomerFeatureResponse.Basic();
        basic.setFundsVolume(convertFeatureByOverwrite(customerFeature.getFundsVolumeModel(), customerFeature.getFundsVolumeSales(), FundsVolumeEnum.class, String.class));
        basic.setProfitLossSituation(convertFeatureByOverwrite(customerFeature.getProfitLossSituationModel(), customerFeature.getProfitLossSituationSales(), ProfitLossEnum.class, String.class));
        basic.setEarningDesire(convertFeatureByOverwrite(customerFeature.getEarningDesireModel(), customerFeature.getEarningDesireSales(), EarningDesireEnum.class, String.class));
        customerFeatureResponse.setBasic(basic);

        // TradingMethod 客户自己的交易方法
        CustomerFeatureResponse.TradingMethod tradingMethod = new CustomerFeatureResponse.TradingMethod();
        tradingMethod.setCurrentStocks(converFeaturetByAppend(customerFeature.getCurrentStocksModel(), customerFeature.getCurrentStocksSales()));
        tradingMethod.setStockPurchaseReason(converFeaturetByAppend(customerFeature.getStockPurchaseReasonModel(), customerFeature.getStockPurchaseReasonSales()));
        tradingMethod.setTradeTimingDecision(converFeaturetByAppend(customerFeature.getTradeTimingDecisionModel(), customerFeature.getTradeTimingDecisionSales()));
        tradingMethod.setTradingStyle(convertFeatureByOverwrite(customerFeature.getTradingStyleModel(), customerFeature.getTradingStyleSales(), null, String.class));
        tradingMethod.setStockMarketAge(convertFeatureByOverwrite(customerFeature.getStockMarketAgeModel(), customerFeature.getStockMarketAgeSales(), null, String.class));
        tradingMethod.setLearningAbility(convertFeatureByOverwrite(customerFeature.getLearningAbilityModel(), customerFeature.getLearningAbilitySales(), null, String.class));
        customerFeatureResponse.setTradingMethod(tradingMethod);

        // Recognition 客户认可度
        CustomerFeatureResponse.Recognition recognition = new CustomerFeatureResponse.Recognition();
        recognition.setCourseTeacherApproval(convertFeatureByOverwrite(customerFeature.getCourseTeacherApprovalModel(), customerFeature.getCourseTeacherApprovalSales(), null, Boolean.class));
        recognition.setSoftwareFunctionClarity(convertFeatureByOverwrite(customerFeature.getSoftwareFunctionClarityModel(), customerFeature.getSoftwareFunctionClaritySales(), null, Boolean.class));
        recognition.setStockSelectionMethod(convertFeatureByOverwrite(customerFeature.getStockSelectionMethodModel(), customerFeature.getStockSelectionMethodSales(), null, Boolean.class));
        recognition.setSelfIssueRecognition(convertFeatureByOverwrite(customerFeature.getSelfIssueRecognitionModel(), customerFeature.getSelfIssueRecognitionSales(), null, Boolean.class));
        recognition.setSoftwareValueApproval(convertFeatureByOverwrite(customerFeature.getSoftwareValueApprovalModel(), customerFeature.getSoftwareValueApprovalSales(), null, Boolean.class));
        recognition.setSoftwarePurchaseAttitude(convertFeatureByOverwrite(customerFeature.getSoftwarePurchaseAttitudeModel(), customerFeature.getSoftwarePurchaseAttitudeSales(), null, Boolean.class));
        customerFeatureResponse.setRecognition(recognition);
        // Note
        customerFeatureResponse.setNote(customerFeature.getNote());

        return customerFeatureResponse;
    }

    public CustomerProcessSummaryResponse convert2CustomerProcessSummaryResponse(CustomerSummary customerSummary) {
        if (Objects.isNull(customerSummary)) {
            return null;
        }
        CustomerProcessSummaryResponse customerSummaryResponse = new CustomerProcessSummaryResponse();

        CustomerProcessSummaryResponse.ProcessSummary processSummary = new CustomerProcessSummaryResponse.ProcessSummary();
        processSummary.setAdvantage(customerSummary.getSummaryAdvantage());
        processSummary.setQuestions(customerSummary.getSummaryQuestions());
        customerSummaryResponse.setSummary(processSummary);

        CustomerProcessSummaryResponse.ProcessInfoExplanation infoExplanation = new CustomerProcessSummaryResponse.ProcessInfoExplanation();
        infoExplanation.setStock(convertSummaryByOverwrite(customerSummary.getIllustrateBasedStock()));
        infoExplanation.setTradeBasedIntro(convertSummaryByOverwrite(customerSummary.getTradeStyleIntroduce()));
        infoExplanation.setStockPickReview(convertSummaryByOverwrite(customerSummary.getStockPickMethodReview()));
        infoExplanation.setStockTimingReview(convertSummaryByOverwrite(customerSummary.getStockPickTimingReview()));
        customerSummaryResponse.setInfoExplanation(infoExplanation);

        CustomerProcessSummaryResponse.ProcessApprovalAnalysis approvalAnalysis = new CustomerProcessSummaryResponse.ProcessApprovalAnalysis();
        approvalAnalysis.setMethod(convertProcessContent(customerSummary.getApprovalAnalysisMethod()));
        approvalAnalysis.setIssue(convertProcessContent(customerSummary.getApprovalAnalysisIssue()));
        approvalAnalysis.setPrice(convertProcessContent(customerSummary.getApprovalAnalysisPrice()));
        approvalAnalysis.setPurchase(convertProcessContent(customerSummary.getApprovalAnalysisPurchase()));
        approvalAnalysis.setPrice(convertProcessContent(customerSummary.getApprovalAnalysisPrice()));
        customerSummaryResponse.setApprovalAnalysis(approvalAnalysis);

        return customerSummaryResponse;
    }


    private CustomerFeatureResponse.Feature convertFeatureByOverwrite(List<FeatureContent> featureContentByModel, List<FeatureContent> featureContentBySales, Class<? extends Enum<?>> enumClass, Class type) {
        CustomerFeatureResponse.Feature featureVO = new CustomerFeatureResponse.Feature();
        // 多通电话覆盖+规则加工
        String resultAnswer = null;
        String resultAnswerLatest = null;
        // 获取
        if (!CollectionUtils.isEmpty(featureContentByModel)) {
            for (int i = featureContentByModel.size() - 1; i >= 0; i--) {
                if (!StringUtils.isEmpty(featureContentByModel.get(i).getAnswer()) && !featureContentByModel.get(i).getAnswer().equals("无") && !featureContentByModel.get(i).getAnswer().equals("null")) {
                    resultAnswerLatest = featureContentByModel.get(i).getAnswer();
                    break;
                }
            }
        }
        // 如果最后一个非空值为null，结果就是null
        if (!StringUtils.isEmpty(resultAnswerLatest)) {
            // 没有候选值枚举，直接返回最后一个非空（如果存在）记录值
            if (Objects.isNull(enumClass)) {
                resultAnswer = resultAnswerLatest;
            } else {
                // 有候选值枚举，需要比较最后一个非空记录值是否跟候选值相同，不同则返回为空
                for (Enum<?> enumConstant : enumClass.getEnumConstants()) {
                    // 获取枚举对象的 `value` 和 `text` 字段值
                    String value = getFieldValue(enumConstant, "value");
                    String enumText = getFieldValue(enumConstant, "text");
                    // 判断文本是否匹配`text`
                    if (resultAnswerLatest.trim().equals(enumText)) {
                        resultAnswer = value;
                        break;
                    }
                }
            }
            // 返回值类型是boolen
            if (type == Boolean.class) {
                resultAnswer = deletePunctuation(resultAnswer);
                if ("是".equals(resultAnswer)) {
                    featureVO.setModelRecord(Boolean.TRUE);
                } else if ("否".equals(resultAnswer)) {
                    featureVO.setModelRecord(Boolean.FALSE);
                }
            } else {
                featureVO.setModelRecord(resultAnswer);
            }
        }
        featureVO.setSalesRecord(CollectionUtils.isEmpty(featureContentBySales) ? null : featureContentBySales.get(featureContentBySales.size() - 1).getAnswer());
        //“已询问”有三个值：“是”、“否”、“不需要”。

        if (!CollectionUtils.isEmpty(featureContentByModel)) {
            //如果 funds_volume_model json list 中有一个 question 有值，就是 ‘是’;
            for (int i = featureContentByModel.size() - 1; i >= 0; i--) {
                if (!StringUtils.isEmpty(featureContentByModel.get(i).getQuestion()) && !featureContentByModel.get(i).getQuestion().equals("无") && !featureContentByModel.get(i).getQuestion().equals("null")) {
                    featureVO.setInquired("yes");
                    break;
                }
            }
            //如果都没有 question 或者 question 都没值，但是有 answer 有值，就是‘不需要’；
            if (featureVO.getInquired().equals("no")) {
                for (int i = featureContentByModel.size() - 1; i >= 0; i--) {
                    if (!StringUtils.isEmpty(featureContentByModel.get(i).getAnswer()) && !featureContentByModel.get(i).getAnswer().equals("无") && !featureContentByModel.get(i).getAnswer().equals("null")) {
                        featureVO.setInquired("no-need");
                        break;
                    }
                }
            }
        }
        //否则就是 ‘否’
        return featureVO;
    }


    private String getFieldValue(Enum<?> enumConstant, String fieldName) {
        try {
            Field field = enumConstant.getClass().getDeclaredField(fieldName);
            field.setAccessible(true); // 设置字段的可访问性
            return (String) field.get(enumConstant);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    private CustomerFeatureResponse.Feature converFeaturetByAppend(List<FeatureContent> featureContentByModel, List<FeatureContent> featureContentBySales) {
        CustomerFeatureResponse.Feature featureVO = new CustomerFeatureResponse.Feature();
        // 多通电话追加+规则加工，跳过null值
        List<String> modelRecord = new ArrayList<>();
        if (!CollectionUtils.isEmpty(featureContentByModel)) {
            ListIterator<FeatureContent> iterator = featureContentByModel.listIterator(featureContentByModel.size());
            while (iterator.hasPrevious()) {
                FeatureContent item = iterator.previous();
                if (!StringUtils.isEmpty(item.getAnswer())) {
                    modelRecord.add(item.getAnswer());
                }
            }
        }

        List<String> sailRecord = new ArrayList<>();
        if (!CollectionUtils.isEmpty(featureContentBySales)) {
            ListIterator<FeatureContent> iterator = featureContentBySales.listIterator(featureContentBySales.size());
            while (iterator.hasPrevious()) {
                FeatureContent item = iterator.previous();
                if (!StringUtils.isEmpty(item)) {
                    sailRecord.add(item.getAnswer());
                }
            }
        }

        featureVO.setModelRecord(CollectionUtils.isEmpty(modelRecord) ? null : JsonUtil.serialize(modelRecord));
        featureVO.setSalesRecord(CollectionUtils.isEmpty(sailRecord) ? null : JsonUtil.serialize(sailRecord));
        //“已询问”有三个值：“是”、“否”、“不需要”。
        // “是”代表模型提取出了销售有询问，“否”代表模型提取出了销售没询问，“不需要”代表“客户情况（模型记录）或（销售补充）”有值且销售没询问（即客户主动说了，销售不需要询问了）
        return featureVO;
    }

    private Object convertSummaryByOverwrite(List<SummaryContent> summaryContentList) {
        if (CollectionUtils.isEmpty(summaryContentList)) {
            return Boolean.FALSE;
        }
        // 多通电话覆盖+规则加工
        for (SummaryContent item : summaryContentList) {
            if (!StringUtils.isEmpty(item.getContent().trim())) {
                return Boolean.TRUE;
            }
        }
        return Boolean.FALSE;
    }

    private CustomerProcessSummaryResponse.ProcessContent convertProcessContent(List<SummaryContent> summaryContentList) {
        CustomerProcessSummaryResponse.ProcessContent processContent = new CustomerProcessSummaryResponse.ProcessContent();
        List<CustomerProcessSummaryResponse.Chat> chatList = new ArrayList<>();
        if (!CollectionUtils.isEmpty(summaryContentList)) {
            for (SummaryContent item : summaryContentList) {
                CustomerProcessSummaryResponse.Chat chat = new CustomerProcessSummaryResponse.Chat();
                List<CustomerProcessSummaryResponse.Message> messageList = new ArrayList<>();
                CustomerProcessSummaryResponse.Message message = new CustomerProcessSummaryResponse.Message();
                message.setContent(item.getContent());
                messageList.add(message);
                chat.setMessages(messageList);
                chatList.add(chat);
            }
        }
        processContent.setChats(chatList);
        return processContent;
    }

    private CustomerProcessSummaryResponse.ProcessSummary getProcessSummary(CustomerFeature customerFeature, CustomerInfo customerInfo) {
        CustomerProcessSummaryResponse.ProcessSummary processSummary = new CustomerProcessSummaryResponse.ProcessSummary();

        CustomerFeatureResponse customerFeatureResponse = convert2CustomerFeatureResponse(customerFeature);
        List<String> advantage = new ArrayList<>();
        List<String> questions = new ArrayList<>();

        // 客户客户匹配度判断
        String conversionRate = customerInfo.getConversionRate();
        // 优点：-提前完成客户匹配度判断：通话次数等于0 and 客户匹配度判断的值不为“未完成判断”
        // 优点：-完成客户匹配度判断：客户匹配度判断的值不为“未完成判断”（如果有了“提前完成客户匹配度判断”，则本条不用再判断）
        // 缺点：-未完成客户匹配度判断：客户匹配度判断的值为“未完成判断”，并列出缺具体哪个字段的信息（前提条件是通话次数大于等于1 and 通话总时长大于等于2分钟）
        if (Objects.nonNull(customerInfo.getCommunicationRounds()) && customerInfo.getCommunicationRounds().equals(0) && !conversionRate.equals("incomplete")) {
            advantage.add("提前完成客户匹配度判断");
        } else if (!conversionRate.equals("incomplete")) {
            advantage.add("完成客户匹配度判断");
        } else if (conversionRate.equals("incomplete") && Objects.nonNull(customerInfo.getCommunicationRounds()) && customerInfo.getCommunicationRounds() >= 1 && Objects.nonNull(customerInfo.getTotalDuration()) && customerInfo.getTotalDuration() >= 120) {
            questions.add("未完成客户匹配度判断");
        }

        // 客户交易风格了解
        // 优点：-提前完成客户交易风格了解：通话次数等于0 and “客户交易风格了解”的值为“完成”
        // 优点：-完成客户交易风格了解：“客户交易风格了解”的值为“完成”（如果有了“提前完成客户交易风格了解”，则本条不用再判断）
        // 缺点：-未完成客户交易风格了解：“客户交易风格了解”的值为“未完成”，并列出缺具体哪个字段的信息（前提条件是通话次数大于等于1 and 通话总时长大于等于2分钟）
        String tradingStyleInquired = customerFeatureResponse.getTradingMethod().getTradingStyle().getInquired();
        if (Objects.nonNull(customerInfo.getCommunicationRounds()) && customerInfo.getCommunicationRounds().equals(0) && "yes".equals(tradingStyleInquired)) {
            advantage.add("提前完成客户交易风格了解");
        } else if ("yes".equals(tradingStyleInquired)) {
            advantage.add("完成客户交易风格了解");
        } else if ("no".equals(tradingStyleInquired) && Objects.nonNull(customerInfo.getCommunicationRounds()) && customerInfo.getCommunicationRounds() >= 1 && Objects.nonNull(customerInfo.getTotalDuration()) && customerInfo.getTotalDuration() >= 120) {
            questions.add("未完成客户交易风格了解");
        }

        // 跟进的客户
        // 优点：-跟进对的客户：销售跟进的不是客户匹配度判断的值为“较低”的客户（通话次数有增加）
        // 缺点：-跟进错的客户：销售跟进的是客户匹配度判断的值为“较低”的客户（通话次数有增加）
        if (!conversionRate.equals("low")) {
            advantage.add("跟进对的客户");
        } else {
            questions.add("跟进错的客户");
        }

        // 功能讲解
        // 优点：-功能讲解让客户理解：“客户对软件功能的清晰度”的值为“是”
        // 缺点：-功能讲解未让客户理解：“客户对软件功能的清晰度”的值为“否”
        if (Objects.nonNull(customerFeatureResponse.getRecognition().getSoftwareFunctionClarity().getModelRecord()) && (Boolean) customerFeatureResponse.getRecognition().getSoftwareFunctionClarity().getModelRecord()) {
            advantage.add("功能讲解让客户理解");
        } else {
            questions.add("功能讲解未让客户理解");
        }

        // 让客户认可价值
        // 优点：-成功让客户认可价值：相关字段全部为“是”——“客户对软件功能的清晰度”、“客户对销售讲的选股方法的认可度”、“客户对自身问题及影响的认可度”、“客户对软件价值的认可度”
        // 缺点：-未让客户认可价值：相关字段不全部为“是”——“客户对软件功能的清晰度”、“客户对销售讲的选股方法的认可度”、“客户对自身问题及影响的认可度”、“客户对软件价值的认可度”，并列出缺具体哪个字段不为“是”
        CustomerFeatureResponse.Recognition recognition = customerFeatureResponse.getRecognition();
        if (Objects.nonNull(recognition.getSoftwareFunctionClarity().getModelRecord()) && (Boolean) recognition.getSoftwareFunctionClarity().getModelRecord() && Objects.nonNull(recognition.getStockSelectionMethod().getModelRecord()) && (Boolean) recognition.getStockSelectionMethod().getModelRecord() && Objects.nonNull(recognition.getSelfIssueRecognition().getModelRecord()) && (Boolean) recognition.getSelfIssueRecognition().getModelRecord() && Objects.nonNull(recognition.getSoftwareValueApproval().getModelRecord()) && (Boolean) recognition.getSoftwareValueApproval().getModelRecord()) {
            advantage.add("成功让客户认可价值");
        } else {
            questions.add("未让客户认可价值");
        }


        //-收集信息快（涉及时间戳，可考虑先去掉）
        //-邀约听课成功：“客户回答自己是否会参加课程”的值为“是”（或者用听课次数和听课时长来判断？）
        //-SOP 执行顺序正确：阶段是逐个按顺序完成的

        //-收集信息慢（涉及时间戳，可考虑先去掉）
        //-邀约听课失败：“客户回答自己是否会参加课程”的值为“否”或空（或者用听课次数和听课时长来判断？）（前提条件是通话次数大于等于1 and 通话总时长大于等于2分钟）
        //-SOP 执行顺序错误：阶段不是逐个按顺序完成的，并列出哪几个阶段未按顺序完成
        //-质疑应对失败：单个类别的质疑不认可的对话组数大于等于5，并列出是哪几类的质疑应对失败
        processSummary.setAdvantage(advantage);
        processSummary.setQuestions(questions);
        return processSummary;
    }

}
