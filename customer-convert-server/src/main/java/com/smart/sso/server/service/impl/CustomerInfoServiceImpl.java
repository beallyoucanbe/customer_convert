package com.smart.sso.server.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smart.sso.server.enums.EarningDesireEnum;
import com.smart.sso.server.enums.FundsVolumeEnum;
import com.smart.sso.server.enums.LearningAbilityEnum;
import com.smart.sso.server.primary.mapper.CharacterCostTimeMapper;
import com.smart.sso.server.primary.mapper.CustomerFeatureMapper;
import com.smart.sso.server.primary.mapper.CustomerInfoMapper;
import com.smart.sso.server.primary.mapper.TelephoneRecordMapper;
import com.smart.sso.server.model.*;
import com.smart.sso.server.model.VO.CustomerListVO;
import com.smart.sso.server.model.VO.CustomerProfile;
import com.smart.sso.server.model.dto.*;
import com.smart.sso.server.secondary.mapper.CustomerInfoOldMapper;
import com.smart.sso.server.service.ConfigService;
import com.smart.sso.server.service.CustomerInfoService;
import com.smart.sso.server.service.CustomerRelationService;
import com.smart.sso.server.service.MessageService;
import com.smart.sso.server.service.TelephoneRecordService;
import com.smart.sso.server.util.CommonUtils;
import com.smart.sso.server.util.DateUtil;
import com.smart.sso.server.util.JsonUtil;
import com.smart.sso.server.util.ShellUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static com.smart.sso.server.constant.AppConstant.SOURCEID_KEY_PREFIX;
import static com.smart.sso.server.enums.FundsVolumeEnum.FIVE_TO_TEN_MILLION;
import static com.smart.sso.server.enums.FundsVolumeEnum.GREAT_TEN_MILLION;
import static com.smart.sso.server.enums.FundsVolumeEnum.LESS_FIVE_MILLION;
import static com.smart.sso.server.util.CommonUtils.deletePunctuation;

@Service
@Slf4j
public class CustomerInfoServiceImpl implements CustomerInfoService {

    @Autowired
    private CustomerInfoMapper customerInfoMapper;
    @Autowired
    private CustomerInfoOldMapper customerInfoOldMapper;
    @Autowired
    private CustomerFeatureMapper customerFeatureMapper;
    @Autowired
    private ConfigService configService;
    @Autowired
    private CustomerRelationService customerRelationService;
    @Autowired
    private TelephoneRecordMapper telephoneRecordMapper;
    @Autowired
    private CharacterCostTimeMapper characterCostTimeMapper;
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    @Autowired
    private TelephoneRecordService recordService;
    @Autowired
    @Lazy
    private MessageService messageService;


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
        if (!StringUtils.isEmpty(params.getActivityName())) {
            queryWrapper.like("activity_name", params.getActivityName());
        } else {
            String activityId = configService.getCurrentActivityId();
            queryWrapper.like("activity_id", activityId);
        }

        String sortOrder = params.getSortBy();
        if (sortOrder.equals("owner")) {
            sortOrder = "owner_name";
        } else if (sortOrder.equals("name")) {
            sortOrder = "customer_name";
        }
        boolean isAsc = "asc".equalsIgnoreCase(params.getOrder());
        if ("conversion_rate".equals(sortOrder)) {
            queryWrapper.last("ORDER BY FIELD(conversion_rate, 'incomplete', 'low', 'medium', 'high') " +
                    (isAsc ? "ASC" : "DESC"));
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
    public CustomerProfile queryCustomerById(String customerId, String activityId) {
        CustomerInfo customerInfo = customerInfoMapper.selectByCustomerIdAndCampaignId(customerId, activityId);
        if (Objects.isNull(customerInfo)) {
            customerInfo = recordService.syncCustomerInfoFromRecord(customerId, customerId);
            if (Objects.isNull(customerInfo)) {
                return null;
            }
        }
        CustomerFeature featureFromSale = customerFeatureMapper.selectById(customerInfo.getId());
        CustomerFeatureFromLLM featureFromLLM = recordService.getCustomerFeatureFromLLM(customerId, activityId);

        CustomerFeatureResponse customerFeature = convert2CustomerFeatureResponse(featureFromSale, featureFromLLM);

        CustomerProfile customerProfile = convert2CustomerProfile(customerInfo);
        customerProfile.setCustomerStage(getCustomerStageStatus(customerInfo, featureFromSale, featureFromLLM));
        customerProfile.setIsSend188(customerInfo.getIsSend188());
        if (Objects.isNull(customerProfile.getCommunicationRounds())) {
            customerProfile.setCommunicationRounds(0);
        }
        // 这里重新判断下打电话的次数
        TelephoneRecordStatics round = recordService.getCommunicationRound(customerId, activityId);
        if (customerProfile.getCommunicationRounds() != round.getTotalCalls()) {
            customerInfoMapper.updateCommunicationRounds(customerId, activityId, round.getTotalCalls(), round.getLatestCommunicationTime());
            customerProfile.setCommunicationRounds(round.getTotalCalls());
        }
        // 重新判断一下匹配度，防止更新不及时的情况
        String conversionRate = getConversionRate(customerFeature);
        if (!customerInfo.getConversionRate().equals(conversionRate)) {
            customerInfoMapper.updateConversionRateById(customerInfo.getId(), conversionRate);
            customerProfile.setConversionRate(conversionRate);
        }
        customerProfile.setLastCommunicationDate(Objects.isNull(featureFromLLM) ? null : featureFromLLM.getCommunicationTime());
        return customerProfile;
    }

    @Override
    public CustomerFeatureResponse queryCustomerFeatureById(String customerId, String activityId) {
        CustomerInfo customerInfo = customerInfoMapper.selectByCustomerIdAndCampaignId(customerId, activityId);
        if (Objects.isNull(customerInfo)) {
            customerInfo = recordService.syncCustomerInfoFromRecord(customerId, customerId);
            if (Objects.isNull(customerInfo)) {
                return null;
            }
        }
        CustomerFeature featureFromSale = customerFeatureMapper.selectById(customerInfo.getId());
        CustomerFeatureFromLLM featureFromLLM = recordService.getCustomerFeatureFromLLM(customerId, activityId);
        // 没有通话记录，直接返回
        if (Objects.isNull(featureFromLLM)) {
            featureFromLLM = new CustomerFeatureFromLLM();
        }
        CustomerProcessSummary summaryResponse = convert2CustomerProcessSummaryResponse(featureFromLLM, featureFromSale);
        CustomerStageStatus stageStatus = getCustomerStageStatus(customerInfo, featureFromSale, featureFromLLM);
        CustomerFeatureResponse customerFeature = convert2CustomerFeatureResponse(featureFromSale, featureFromLLM);
        if (Objects.nonNull(customerFeature)) {
            customerFeature.setTradingMethod(Objects.isNull(summaryResponse) ? null : summaryResponse.getTradingMethod());
            getStandardExplanationCompletion(customerFeature);
            customerFeature.setSummary(getProcessSummary(customerFeature, customerInfo, stageStatus, summaryResponse));
        }
        return customerFeature;
    }

    @Override
    public CustomerProcessSummary queryCustomerProcessSummaryById(String id) {
        CustomerInfo customerInfo = customerInfoMapper.selectById(id);
        CustomerFeature featureFromSale = customerFeatureMapper.selectById(id);
        CustomerFeatureFromLLM featureFromLLM = recordService.getCustomerFeatureFromLLM(customerInfo.getCustomerId(), customerInfo.getActivityId());
        return convert2CustomerProcessSummaryResponse(featureFromLLM, featureFromSale);
    }

    @Override
    public String getConversionRate(CustomerFeatureResponse customerFeature) {
        // "high", "medium", "low", "incomplete"
        // -较高：资金体量=“大于10万”
        // -中等：资金体量=“5到10万”
        // -较低：资金体量=“小于5万”
        // -未完成判断：资金体量=空
        String result = "incomplete";
        if (Objects.isNull(customerFeature) || Objects.isNull(customerFeature.getBasic())
                || Objects.isNull(customerFeature.getBasic().getFundsVolume())
                || Objects.isNull(customerFeature.getBasic().getFundsVolume().getCustomerConclusion())) {
            return result;
        }
        Feature.CustomerConclusion conclusion = customerFeature.getBasic().getFundsVolume().getCustomerConclusion();
        if (StringUtils.isEmpty(conclusion.getCompareValue())) {
            return result;
        }
        if (conclusion.getCompareValue().equals(GREAT_TEN_MILLION.getValue())) {
            return "high";
        }
        if (conclusion.getCompareValue().equals(FIVE_TO_TEN_MILLION.getValue())) {
            return "medium";
        }
        if (conclusion.getCompareValue().equals(LESS_FIVE_MILLION.getValue())) {
            return "low";
        }
        return result;
    }

    @Override
    public CustomerStageStatus getCustomerStageStatus(CustomerInfo customerInfo, CustomerFeature featureFromSale, CustomerFeatureFromLLM featureFromLLM) {
        CustomerFeatureResponse customerFeature = convert2CustomerFeatureResponse(featureFromSale, featureFromLLM);
        CustomerProcessSummary summaryResponse = convert2CustomerProcessSummaryResponse(featureFromLLM, featureFromSale);
        CustomerStageStatus stageStatus = new CustomerStageStatus();
        // 客户匹配度判断 值不为“未完成判断”
        if (!"incomplete".equals(getConversionRate(customerFeature))) {
            stageStatus.setMatchingJudgment(1);
        }

        if (Objects.nonNull(customerFeature)) {
            // 客户交易风格了解 相关字段全部有值——“客户当前持仓或关注的股票”、“客户为什么买这些股票”、“客户怎么决定的买卖这些股票的时机”、“客户的交易风格”.这4项的“客户结论”都有值
            CustomerProcessSummary.TradingMethod tradingMethod = summaryResponse.getTradingMethod();
            try {
                if (Objects.nonNull(tradingMethod.getCurrentStocks().getCustomerConclusion().getCompareValue()) &&
                        !tradingMethod.getCurrentStocks().getCustomerConclusion().getCompareValue().equals("无") &&
                        !tradingMethod.getCurrentStocks().getCustomerConclusion().getCompareValue().equals("null") &&
                        Objects.nonNull(tradingMethod.getStockPurchaseReason().getCustomerConclusion().getCompareValue()) &&
                        !tradingMethod.getStockPurchaseReason().getCustomerConclusion().getCompareValue().equals("无") &&
                        !tradingMethod.getStockPurchaseReason().getCustomerConclusion().getCompareValue().equals("null") &&
                        Objects.nonNull(tradingMethod.getTradeTimingDecision().getCustomerConclusion().getCompareValue()) &&
                        !tradingMethod.getTradeTimingDecision().getCustomerConclusion().getCompareValue().equals("无") &&
                        !tradingMethod.getTradeTimingDecision().getCustomerConclusion().getCompareValue().equals("null") &&
                        Objects.nonNull(tradingMethod.getTradingStyle().getCustomerConclusion().getCompareValue()) &&
                        !tradingMethod.getTradingStyle().getCustomerConclusion().getCompareValue().equals("无") &&
                        !tradingMethod.getTradingStyle().getCustomerConclusion().getCompareValue().equals("null")) {
                    stageStatus.setTransactionStyle(1);
                }
            } catch (Exception e) {
                // 有异常就不变
            }
            // 客户确认价值 相关字段的值全部为“是”——“客户对软件功能的清晰度”、“客户对销售讲的选股方法的认可度”、“客户对自身问题及影响的认可度”、“客户对软件价值的认可度”
            try {
                if ((Boolean) customerFeature.getBasic().getSoftwareFunctionClarity().getCustomerConclusion().getCompareValue() &&
                        (Boolean) customerFeature.getBasic().getStockSelectionMethod().getCustomerConclusion().getCompareValue() &&
                        (Boolean) customerFeature.getBasic().getSelfIssueRecognition().getCustomerConclusion().getCompareValue() &&
                        (Boolean) customerFeature.getBasic().getSoftwareValueApproval().getCustomerConclusion().getCompareValue()) {
                    stageStatus.setConfirmValue(1);
                }
            } catch (Exception e) {
                // 有异常就不变
            }
            // 客户确认购买 客户对购买软件的态度”的值为“是”
            try {
                if ((Boolean) customerFeature.getBasic().getSoftwarePurchaseAttitude().getCustomerConclusion().getCompareValue()) {
                    stageStatus.setConfirmPurchase(1);
                }
            } catch (Exception e) {
                // 有异常就不变
            }
        }

        if (Objects.nonNull(summaryResponse) && Objects.nonNull(summaryResponse.getInfoExplanation())) {
            // 针对性功能介绍 相关字段的值全部为“是”——“痛点量化放大”、“价值量化放大”
            CustomerProcessSummary.ProcessInfoExplanation infoExplanation = summaryResponse.getInfoExplanation();
            try {
                if (Objects.nonNull(infoExplanation.getSoftwareValueQuantified()) &&
                        infoExplanation.getSoftwareValueQuantified().getResult() &&
                        Objects.nonNull(infoExplanation.getTradeBasedIntro()) &&
                        infoExplanation.getTradeBasedIntro().getResult()) {
                    stageStatus.setFunctionIntroduction(1);
                }
            } catch (Exception e) {
                // 有异常就不变
            }
        }
        // 客户完成购买”，规则是看客户提供的字段“成交状态”来直接判定，这个数值从数据库中提取
        try {
            CustomerRelation customerRelation = customerRelationService.getByActivityAndCustomer(customerInfo.getCustomerId(),
                    customerInfo.getOwnerId(), customerInfo.getActivityId());
            if (Objects.nonNull(customerRelation) && Objects.nonNull(customerRelation.getCustomerSigned())
                    && customerRelation.getCustomerSigned()) {
                stageStatus.setCompletePurchase(1);
            }
            customerInfo.setIsSend188(customerRelation.getIsSend188());
        } catch (Exception e) {
            log.error("判断确认购买状态失败, ID={}", customerInfo.getCustomerId());
        }
        return stageStatus;
    }

    @Override
    public List<ActivityInfoWithVersion> getActivityInfoByCustomerId(String customerId) {
        List<ActivityInfoWithVersion> result = new ArrayList<>();
        List<ActivityInfo> newActivity = customerInfoMapper.selectActivityInfoByCustomerId(customerId);
        for (ActivityInfo activityInfo : newActivity) {
            ActivityInfoWithVersion activityInfoWithVersion = new ActivityInfoWithVersion(activityInfo);
            result.add(activityInfoWithVersion);
        }
        // 是否有旧活动
        try {
            String activity = customerInfoOldMapper.selectActivityByCustomerId(customerId);
            if (!StringUtils.isEmpty(activity)) {
                Map<String, String> activityIdNames = configService.getActivityIdNames();
                ActivityInfoWithVersion activityInfoWithVersion = new ActivityInfoWithVersion(Boolean.TRUE);
                activityInfoWithVersion.setActivityId(activity);
                activityInfoWithVersion.setActivityName(activityIdNames.containsKey(activity) ? activityIdNames.get(activity) : activity);
                result.add(activityInfoWithVersion);
            }
        } catch (Exception e) {
            log.error("获取旧活动失败，跳过");
        }
        return result;
    }

    @Override
    public void modifyCustomerFeatureById(String customerId, String activityId, CustomerFeatureResponse customerFeatureRequest) {
        CustomerInfo customerInfo = customerInfoMapper.selectByCustomerIdAndCampaignId(customerId, activityId);
        CustomerFeature customerFeature = customerFeatureMapper.selectById(customerInfo.getId());
        if (Objects.isNull(customerFeature)) {
            customerFeature = new CustomerFeature();
            customerFeature.setId(customerInfo.getId());
            customerFeatureMapper.insert(customerFeature);
        }
        if (Objects.nonNull(customerFeatureRequest.getBasic())) {
            if (Objects.nonNull(customerFeatureRequest.getBasic().getFundsVolume()) &&
                    Objects.nonNull(customerFeatureRequest.getBasic().getFundsVolume().getCustomerConclusion()) &&
                    (Objects.nonNull(customerFeatureRequest.getBasic().getFundsVolume().getCustomerConclusion().getSalesRecord()) ||
                            Objects.nonNull(customerFeatureRequest.getBasic().getFundsVolume().getCustomerConclusion().getSalesManualTag()))) {
                customerFeature.setFundsVolumeSales(new FeatureContentSales(customerFeatureRequest.getBasic().getFundsVolume().getCustomerConclusion().getSalesRecord(),
                        customerFeatureRequest.getBasic().getFundsVolume().getCustomerConclusion().getSalesManualTag(), DateUtil.getCurrentDateTime()));
            }
            if (Objects.nonNull(customerFeatureRequest.getBasic().getEarningDesire()) &&
                    Objects.nonNull(customerFeatureRequest.getBasic().getEarningDesire().getCustomerConclusion()) &&
                    (Objects.nonNull(customerFeatureRequest.getBasic().getEarningDesire().getCustomerConclusion().getSalesRecord()) ||
                            Objects.nonNull(customerFeatureRequest.getBasic().getEarningDesire().getCustomerConclusion().getSalesManualTag()))) {
                customerFeature.setEarningDesireSales(new FeatureContentSales(customerFeatureRequest.getBasic().getEarningDesire().getCustomerConclusion().getSalesRecord(),
                        customerFeatureRequest.getBasic().getEarningDesire().getCustomerConclusion().getSalesManualTag(), DateUtil.getCurrentDateTime()));
            }

            if (Objects.nonNull(customerFeatureRequest.getBasic().getSelfIssueRecognition()) &&
                    Objects.nonNull(customerFeatureRequest.getBasic().getSelfIssueRecognition().getCustomerConclusion()) &&
                    (Objects.nonNull(customerFeatureRequest.getBasic().getSelfIssueRecognition().getCustomerConclusion().getSalesRecord()) ||
                            Objects.nonNull(customerFeatureRequest.getBasic().getSelfIssueRecognition().getCustomerConclusion().getSalesManualTag()))) {
                customerFeature.setSelfIssueRecognitionSales(new FeatureContentSales(customerFeatureRequest.getBasic().getSelfIssueRecognition().getCustomerConclusion().getSalesRecord(),
                        customerFeatureRequest.getBasic().getSelfIssueRecognition().getCustomerConclusion().getSalesManualTag(), DateUtil.getCurrentDateTime()));
            }
            if (Objects.nonNull(customerFeatureRequest.getBasic().getSoftwareFunctionClarity()) &&
                    Objects.nonNull(customerFeatureRequest.getBasic().getSoftwareFunctionClarity().getCustomerConclusion()) &&
                    (Objects.nonNull(customerFeatureRequest.getBasic().getSoftwareFunctionClarity().getCustomerConclusion().getSalesRecord()) ||
                            Objects.nonNull(customerFeatureRequest.getBasic().getSoftwareFunctionClarity().getCustomerConclusion().getSalesManualTag()))) {
                customerFeature.setSoftwareFunctionClaritySales(new FeatureContentSales(customerFeatureRequest.getBasic().getSoftwareFunctionClarity().getCustomerConclusion().getSalesRecord(),
                        customerFeatureRequest.getBasic().getSoftwareFunctionClarity().getCustomerConclusion().getSalesManualTag(), DateUtil.getCurrentDateTime()));
            }
            if (Objects.nonNull(customerFeatureRequest.getBasic().getSoftwarePurchaseAttitude()) &&
                    Objects.nonNull(customerFeatureRequest.getBasic().getSoftwarePurchaseAttitude().getCustomerConclusion()) &&
                    (Objects.nonNull(customerFeatureRequest.getBasic().getSoftwarePurchaseAttitude().getCustomerConclusion().getSalesRecord()) ||
                            Objects.nonNull(customerFeatureRequest.getBasic().getSoftwarePurchaseAttitude().getCustomerConclusion().getSalesManualTag()))) {
                customerFeature.setSoftwarePurchaseAttitudeSales(new FeatureContentSales(customerFeatureRequest.getBasic().getSoftwarePurchaseAttitude().getCustomerConclusion().getSalesRecord(),
                        customerFeatureRequest.getBasic().getSoftwarePurchaseAttitude().getCustomerConclusion().getSalesManualTag(), DateUtil.getCurrentDateTime()));
            }
            if (Objects.nonNull(customerFeatureRequest.getBasic().getSoftwareValueApproval()) &&
                    Objects.nonNull(customerFeatureRequest.getBasic().getSoftwareValueApproval().getCustomerConclusion()) &&
                    (Objects.nonNull(customerFeatureRequest.getBasic().getSoftwareValueApproval().getCustomerConclusion().getSalesRecord()) ||
                            Objects.nonNull(customerFeatureRequest.getBasic().getSoftwareValueApproval().getCustomerConclusion().getSalesManualTag()))) {
                customerFeature.setSoftwareValueApprovalSales(new FeatureContentSales(customerFeatureRequest.getBasic().getSoftwareValueApproval().getCustomerConclusion().getSalesRecord(),
                        customerFeatureRequest.getBasic().getSoftwareValueApproval().getCustomerConclusion().getSalesManualTag(), DateUtil.getCurrentDateTime()));
            }
            if (Objects.nonNull(customerFeatureRequest.getBasic().getStockSelectionMethod()) &&
                    Objects.nonNull(customerFeatureRequest.getBasic().getStockSelectionMethod().getCustomerConclusion()) &&
                    (Objects.nonNull(customerFeatureRequest.getBasic().getStockSelectionMethod().getCustomerConclusion().getSalesRecord()) ||
                            Objects.nonNull(customerFeatureRequest.getBasic().getStockSelectionMethod().getCustomerConclusion().getSalesManualTag()))) {
                customerFeature.setStockSelectionMethodSales(new FeatureContentSales(customerFeatureRequest.getBasic().getStockSelectionMethod().getCustomerConclusion().getSalesRecord(),
                        customerFeatureRequest.getBasic().getStockSelectionMethod().getCustomerConclusion().getSalesManualTag(), DateUtil.getCurrentDateTime()));
            }
        }
        customerFeatureMapper.updateById(customerFeature);
        try {
            messageService.updateCustomerCharacter(customerId, activityId, false);
        } catch (Exception e) {
            log.error("更新CustomerCharacter失败，CustomerId={}, activityId={}", customerId, activityId, e);
        }
    }

    @Override
    @Async
    public void callback(String sourceId) {
        try {
            // 将sourceId 写入文件
            String filePath = "/opt/customer-convert/callback/v1/sourceid.txt";
            CommonUtils.appendTextToFile(filePath, sourceId);
            String[] params = {sourceId};
            Process process = ShellUtils.saPythonRun("/home/opsuser/hsw/chat_insight_v1/process_text.py", params.length, params);
            // 等待脚本执行完成
            int exitCode = process.waitFor();
            String redisKey = SOURCEID_KEY_PREFIX + sourceId;
            redisTemplate.opsForValue().set(redisKey, "processed");

//            Process process1 = ShellUtils.saPythonRun("/home/opsuser/hsw/test_chat/process_text.py", params.length, params);
//            exitCode = process1.waitFor();
            log.error("Python脚本执行完成，退出码：" + exitCode);
        } catch (Exception e) {
            // 这里只负责调用对用的脚本
            log.error("执行脚本报错", e);
        }
    }

    @Override
    public String getRedirectUrl(String customerId, String activeId, String from, String manager) {
        String urlFormatter = "https://newcmp.emoney.cn/chat/customer?customer_id=%s&activity_id=%s&embed=true";
        if (!StringUtils.isEmpty(from)) {
            urlFormatter = urlFormatter + "&from=" + from;
        }
        if (!StringUtils.isEmpty(manager)) {
            urlFormatter = urlFormatter + "&manager=" + manager;
        }
        return String.format(urlFormatter, customerId, activeId);
    }

    @Override
    public String getRedirectUrlOld(String customerId, String activeId) {
        QueryWrapper<CustomerInfoOld> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("customer_id", customerId);
        queryWrapper.eq("current_campaign", activeId);

        CustomerInfoOld customerInfo = customerInfoOldMapper.selectOne(queryWrapper);
        String id = "";
        if (Objects.isNull(customerInfo)) {
            log.error("获取客户失败");
        } else {
            id = customerInfo.getId();
        }
        String urlFormatter = "https://newcmp.emoney.cn/chat/old-frontend/customer?id=%s";
        return String.format(urlFormatter, id);
    }


    @Override
    public void updateCharacterCostTime(String id) {
        // 总结各个特征的花费的时间
        CustomerInfo customerInfo = customerInfoMapper.selectById(id);
        QueryWrapper<TelephoneRecord> queryWrapperInfo = new QueryWrapper<>();
        queryWrapperInfo.eq("customer_id", customerInfo.getCustomerId());
        queryWrapperInfo.orderByAsc("communication_time");
        // 查看该客户的所有通话记录，并且按照顺序排列
        List<TelephoneRecord> customerFeatureList = telephoneRecordMapper.selectList(queryWrapperInfo);
        CharacterCostTime characterCostTime = new CharacterCostTime();
        characterCostTime.setId(customerInfo.getId());
        characterCostTime.setCustomerId(customerInfo.getCustomerId());
        characterCostTime.setCustomerName(customerInfo.getCustomerName());
        characterCostTime.setOwnerId(customerInfo.getOwnerId());
        characterCostTime.setOwnerName(customerInfo.getOwnerName());
        int communicationTime = 0; // 累计通话时间
        int communicationRound = 1; // 累计通话轮次
        for (TelephoneRecord telephoneRecord : customerFeatureList) {
            // 该次通话有记录特征提取的时间，并且之前没有记录过
            if (!CollectionUtils.isEmpty(telephoneRecord.getFundsVolume()) &&
                    !StringUtils.isEmpty(telephoneRecord.getFundsVolume().get(0).getTs()) &&
                    Objects.isNull(characterCostTime.getCommunicationRoundFundsVolume())) {
                characterCostTime.setCommunicationDurationFundsVolume(communicationTime +
                        Integer.parseInt(telephoneRecord.getFundsVolume().get(0).getTs()));
                characterCostTime.setCommunicationRoundFundsVolume(communicationRound);
            }
            if (!CollectionUtils.isEmpty(telephoneRecord.getEarningDesire()) &&
                    !StringUtils.isEmpty(telephoneRecord.getEarningDesire().get(0).getTs()) &&
                    Objects.isNull(characterCostTime.getCommunicationRoundEarningDesire())) {
                characterCostTime.setCommunicationDurationFearningDesire(communicationTime +
                        Integer.parseInt(telephoneRecord.getEarningDesire().get(0).getTs()));
                characterCostTime.setCommunicationRoundEarningDesire(communicationRound);
            }
            if (!CollectionUtils.isEmpty(telephoneRecord.getSoftwareFunctionClarity()) &&
                    !StringUtils.isEmpty(telephoneRecord.getSoftwareFunctionClarity().get(0).getTs()) &&
                    Objects.isNull(characterCostTime.getCommunicationRoundSoftwareFunctionClarity())) {
                characterCostTime.setCommunicationDurationSoftwareFunctionClarity(communicationTime +
                        Integer.parseInt(telephoneRecord.getSoftwareFunctionClarity().get(0).getTs()));
                characterCostTime.setCommunicationRoundSoftwareFunctionClarity(communicationRound);
            }
            if (!CollectionUtils.isEmpty(telephoneRecord.getStockSelectionMethod()) &&
                    !StringUtils.isEmpty(telephoneRecord.getStockSelectionMethod().get(0).getTs()) &&
                    Objects.isNull(characterCostTime.getCommunicationRoundStockSelectionMethod())) {
                characterCostTime.setCommunicationDurationStockSelectionMethod(communicationTime +
                        Integer.parseInt(telephoneRecord.getStockSelectionMethod().get(0).getTs()));
                characterCostTime.setCommunicationRoundStockSelectionMethod(communicationRound);
            }
            if (!CollectionUtils.isEmpty(telephoneRecord.getSelfIssueRecognition()) &&
                    !StringUtils.isEmpty(telephoneRecord.getSelfIssueRecognition().get(0).getTs()) &&
                    Objects.isNull(characterCostTime.getCommunicationRoundSelfIssueRecognition())) {
                characterCostTime.setCommunicationDurationSelfIssueRecognition(communicationTime +
                        Integer.parseInt(telephoneRecord.getSelfIssueRecognition().get(0).getTs()));
                characterCostTime.setCommunicationRoundSelfIssueRecognition(communicationRound);
            }
            if (!CollectionUtils.isEmpty(telephoneRecord.getSoftwareValueApproval()) &&
                    !StringUtils.isEmpty(telephoneRecord.getSoftwareValueApproval().get(0).getTs()) &&
                    Objects.isNull(characterCostTime.getCommunicationRoundSoftwareValueApproval())) {
                characterCostTime.setCommunicationDurationSoftwareValueApproval(communicationTime +
                        Integer.parseInt(telephoneRecord.getSoftwareValueApproval().get(0).getTs()));
                characterCostTime.setCommunicationRoundSoftwareValueApproval(communicationRound);
            }
            if (!CollectionUtils.isEmpty(telephoneRecord.getSoftwarePurchaseAttitude()) &&
                    !StringUtils.isEmpty(telephoneRecord.getSoftwarePurchaseAttitude().get(0).getTs()) &&
                    Objects.isNull(characterCostTime.getCommunicationRoundSoftwarePurchaseAttitude())) {
                characterCostTime.setCommunicationDurationSoftwarePurchaseAttitude(communicationTime +
                        Integer.parseInt(telephoneRecord.getSoftwarePurchaseAttitude().get(0).getTs()));
                characterCostTime.setCommunicationRoundSoftwarePurchaseAttitude(communicationRound);
            }
            if (!CollectionUtils.isEmpty(telephoneRecord.getCustomerIssuesQuantified()) &&
                    !StringUtils.isEmpty(telephoneRecord.getCustomerIssuesQuantified().get(0).getTs()) &&
                    Objects.isNull(characterCostTime.getCommunicationRoundCustomerIssuesQuantified())) {
                characterCostTime.setCommunicationDurationCustomerIssuesQuantified(communicationTime +
                        Integer.parseInt(telephoneRecord.getCustomerIssuesQuantified().get(0).getTs()));
                characterCostTime.setCommunicationRoundCustomerIssuesQuantified(communicationRound);
            }
            if (!CollectionUtils.isEmpty(telephoneRecord.getSoftwareValueQuantified()) &&
                    !StringUtils.isEmpty(telephoneRecord.getSoftwareValueQuantified().get(0).getTs()) &&
                    Objects.isNull(characterCostTime.getCommunicationRoundSoftwareValueQuantified())) {
                characterCostTime.setCommunicationDurationSoftwareValueQuantified(communicationTime +
                        Integer.parseInt(telephoneRecord.getSoftwareValueQuantified().get(0).getTs()));
                characterCostTime.setCommunicationRoundSoftwareValueQuantified(communicationRound);
            }
            communicationRound++;
            communicationTime += telephoneRecord.getCommunicationDuration();
        }
        // 是否发生变化，有变化则更新
        CharacterCostTime oldCharacterCostTime = characterCostTimeMapper.selectById(id);
        if (Objects.isNull(oldCharacterCostTime)) {
            // 新建
            characterCostTimeMapper.insert(characterCostTime);
        } else {
            // 更新
            if (!areEqual(oldCharacterCostTime, characterCostTime)) {
                characterCostTimeMapper.updateById(characterCostTime);
            }
        }
    }



    @Override
    public void statistics() {
        QueryWrapper<CustomerInfo> queryWrapperInfo = new QueryWrapper<>();
        // 筛选时间
        queryWrapperInfo.eq("activity_id", "373");
        List<CustomerInfo> customerFeatureList = customerInfoMapper.selectList(queryWrapperInfo);
        System.out.println("总客户数：" + customerFeatureList.size());
        int customerNum = 0;
        int featureNum = 0;
        List<String> result = new ArrayList<>();
        for (CustomerInfo customerInfo : customerFeatureList) {
            CustomerFeatureResponse featureProfile = queryCustomerFeatureById(customerInfo.getCustomerId(), customerInfo.getActivityId());
            boolean tttt = true;
            if (!equal(featureProfile.getBasic().getFundsVolume())) {
                tttt = false;
                featureNum++;
            }
            if (!equal(featureProfile.getBasic().getEarningDesire())) {
                tttt = false;
                featureNum++;
            }
            if (!equal(featureProfile.getBasic().getSoftwareFunctionClarity())) {
                tttt = false;
                featureNum++;
            }
            if (!equal(featureProfile.getBasic().getStockSelectionMethod())) {
                tttt = false;
                featureNum++;
            }
            if (!equal(featureProfile.getBasic().getSelfIssueRecognition())) {
                tttt = false;
                featureNum++;
            }
            if (!equal(featureProfile.getBasic().getSoftwareValueApproval())) {
                tttt = false;
                featureNum++;
            }
            if (!equal(featureProfile.getBasic().getSoftwarePurchaseAttitude())) {
                tttt = false;
                featureNum++;
            }

            if (!tttt) {
                customerNum++;
                result.add(customerInfo.getId());
            }
        }
        System.out.println("customerNum=" + customerNum + ", featureNum = " + featureNum);
        System.out.println(JsonUtil.serialize(result));
    }

    @Override
    public void syncCustomerInfoFromRelation() {
        String activityId = configService.getCurrentActivityId();
        if (Objects.isNull(activityId)) {
            log.error("没有当前的活动，请先配置");
            return;
        }
        List<CustomerRelation> characterList = customerRelationService.getByActivityAndUpdateTime(activityId,
                LocalDateTime.of(2024, 1, 1, 12, 0, 0));
        Map<String, String> activityIdNames = configService.getActivityIdNames();
        for (CustomerRelation relation : characterList) {
            CustomerInfo customerInfo = customerInfoMapper.selectByCustomerIdAndCampaignId(Long.toString(relation.getCustomerId()), activityId);
            if (Objects.nonNull(customerInfo)) {
                continue;
            }
            customerInfo = new CustomerInfo();
            customerInfo.setId(CommonUtils.generatePrimaryKey());
            customerInfo.setCustomerId(Long.toString(relation.getCustomerId()));
            customerInfo.setOwnerId(relation.getOwnerId());
            customerInfo.setCustomerName(relation.getCustomerName());
            customerInfo.setOwnerName(relation.getOwnerName());
            customerInfo.setActivityId(activityId);
            customerInfo.setActivityName(activityIdNames.get(activityId));
            customerInfo.setUpdateTimeTelephone(LocalDateTime.now());
            customerInfoMapper.insert(customerInfo);
        }
    }

    /**
     *
     * @param activeId
     * @return
     */
    @Override
    public List<CustomerInfo> getCustomerInfoLongTimeNoSee(String activeId) {
        // 获取当前时间
        LocalDateTime currentTime = LocalDateTime.now();
        // 获取三天前的日期
        LocalDateTime threeDayBefore = currentTime.minusDays(3);
        return customerInfoMapper.getCustomerInfoByUpdateTime(threeDayBefore, activeId);
    }

    private boolean equal(Feature feature) {
        if (Objects.isNull(feature.getCustomerConclusion().getSalesManualTag())) {
            return true;
        }
        if (Objects.isNull(feature.getCustomerConclusion().getModelRecord())) {
            return false;
        } else if (feature.getCustomerConclusion().getModelRecord().equals(feature.getCustomerConclusion().getSalesManualTag())) {
            return true;
        } else {
            return false;
        }
    }

    public boolean areEqual(CharacterCostTime cc1, CharacterCostTime cc2) {
        if (cc1 == cc2) {
            return true;
        }
        if (cc1 == null || cc2 == null) {
            return false;
        }

        // 获取CustomerCharacter类的所有字段
        Field[] fields = CharacterCostTime.class.getDeclaredFields();

        for (Field field : fields) {
            // 跳过 createTime 和 updateTime 字段
            if ("createTime".equals(field.getName()) || "update_time".equals(field.getName())) {
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

    public CustomerFeatureResponse convert2CustomerFeatureResponse(CustomerFeature featureFromSale, CustomerFeatureFromLLM featureFromLLM) {
        if (Objects.isNull(featureFromLLM)) {
            return null;
        }
        CustomerFeatureResponse customerFeatureResponse = new CustomerFeatureResponse();
        // Basic 基本信息
        CustomerFeatureResponse.Basic basic = new CustomerFeatureResponse.Basic();
        basic.setFundsVolume(convertBaseFeatureByOverwrite(featureFromLLM.getFundsVolume(), Objects.isNull(featureFromSale) ? null : featureFromSale.getFundsVolumeSales(), FundsVolumeEnum.class, String.class));
        basic.setEarningDesire(convertBaseFeatureByOverwrite(featureFromLLM.getEarningDesire(), Objects.isNull(featureFromSale) ? null : featureFromSale.getEarningDesireSales(), EarningDesireEnum.class, String.class));

        // 量化信息
        CustomerFeatureResponse.Quantified quantified = new CustomerFeatureResponse.Quantified();
        quantified.setCustomerIssuesQuantified(convertSummaryByOverwrite(featureFromLLM.getCustomerIssuesQuantified()));
        quantified.setSoftwareValueQuantified(convertSummaryByOverwrite(featureFromLLM.getSoftwareValueQuantified()));
        basic.setQuantified(quantified);

        basic.setSoftwareFunctionClarity(convertBaseFeatureByOverwrite(featureFromLLM.getSoftwareFunctionClarity(), Objects.isNull(featureFromSale) ? null : featureFromSale.getSoftwareFunctionClaritySales(), null, Boolean.class));
        basic.setStockSelectionMethod(convertBaseFeatureByOverwrite(featureFromLLM.getStockSelectionMethod(), Objects.isNull(featureFromSale) ? null : featureFromSale.getStockSelectionMethodSales(), null, Boolean.class));
        basic.setSelfIssueRecognition(convertBaseFeatureByOverwrite(featureFromLLM.getSelfIssueRecognition(), Objects.isNull(featureFromSale) ? null : featureFromSale.getSelfIssueRecognitionSales(), null, Boolean.class));
        basic.setSoftwareValueApproval(convertBaseFeatureByOverwrite(featureFromLLM.getSoftwareValueApproval(), Objects.isNull(featureFromSale) ? null : featureFromSale.getSoftwareValueApprovalSales(), null, Boolean.class));
        basic.setSoftwarePurchaseAttitude(convertBaseFeatureByOverwrite(featureFromLLM.getSoftwarePurchaseAttitude(), Objects.isNull(featureFromSale) ? null : featureFromSale.getSoftwarePurchaseAttitudeSales(), null, Boolean.class));
        customerFeatureResponse.setBasic(basic);
        return customerFeatureResponse;
    }

    public CustomerProcessSummary convert2CustomerProcessSummaryResponse(CustomerFeatureFromLLM featureFromLLM, CustomerFeature featureFromSale) {
        if (Objects.isNull(featureFromLLM)) {
            return null;
        }
        CustomerProcessSummary customerSummaryResponse = new CustomerProcessSummary();
        CustomerProcessSummary.ProcessInfoExplanation infoExplanation = new CustomerProcessSummary.ProcessInfoExplanation();
        infoExplanation.setStock(convertSummaryByOverwrite(featureFromLLM.getIllustrateBasedStock()));
        infoExplanation.setTradeBasedIntro(convertSummaryByOverwrite(featureFromLLM.getTradeStyleIntroduce()));
        infoExplanation.setStockPickReview(convertSummaryByOverwrite(featureFromLLM.getStockPickMethodReview()));
        infoExplanation.setStockTimingReview(convertSummaryByOverwrite(featureFromLLM.getStockPickTimingReview()));
        infoExplanation.setSoftwareValueQuantified(convertSummaryByOverwrite(featureFromLLM.getSoftwareValueQuantified()));
        infoExplanation.setCustomerIssuesQuantified(convertSummaryByOverwrite(featureFromLLM.getCustomerIssuesQuantified()));
        customerSummaryResponse.setInfoExplanation(infoExplanation);

        CustomerProcessSummary.TradingMethod tradingMethod = new CustomerProcessSummary.TradingMethod();
        tradingMethod.setCurrentStocks(convertTradeMethodFeatureByOverwrite(featureFromLLM.getCurrentStocks(), Objects.isNull(featureFromSale) ? null : featureFromSale.getCurrentStocksSales(), null, String.class));
        tradingMethod.setStockPurchaseReason(convertTradeMethodFeatureByOverwrite(featureFromLLM.getStockPurchaseReason(), Objects.isNull(featureFromSale) ? null : featureFromSale.getStockPurchaseReasonSales(), null, String.class));
        tradingMethod.setTradeTimingDecision(convertTradeMethodFeatureByOverwrite(featureFromLLM.getTradeTimingDecision(), Objects.isNull(featureFromSale) ? null : featureFromSale.getTradeTimingDecisionSales(), null, String.class));
        tradingMethod.setTradingStyle(convertTradeMethodFeatureByOverwrite(featureFromLLM.getTradingStyle(), Objects.isNull(featureFromSale) ? null : featureFromSale.getTradingStyleSales(), null, String.class));
        tradingMethod.setStockMarketAge(convertTradeMethodFeatureByOverwrite(featureFromLLM.getStockMarketAge(), Objects.isNull(featureFromSale) ? null : featureFromSale.getStockMarketAgeSales(), null, String.class));
        tradingMethod.setLearningAbility(convertTradeMethodFeatureByOverwrite(featureFromLLM.getLearningAbility(), Objects.isNull(featureFromSale) ? null : featureFromSale.getLearningAbilitySales(), LearningAbilityEnum.class, String.class));

        tradingMethod.getCurrentStocks().setStandardAction(infoExplanation.getStock());
        tradingMethod.getStockPurchaseReason().setStandardAction(infoExplanation.getStockPickReview());
        tradingMethod.getTradeTimingDecision().setStandardAction(infoExplanation.getStockTimingReview());
        tradingMethod.getTradingStyle().setStandardAction(infoExplanation.getTradeBasedIntro());

        customerSummaryResponse.setTradingMethod(tradingMethod);
        return customerSummaryResponse;
    }


    private BaseFeature convertBaseFeatureByOverwrite(CommunicationContent featureContentByModel, FeatureContentSales featureContentBySales, Class<? extends Enum<?>> enumClass, Class type) {
        BaseFeature baseFeature = new BaseFeature(convertFeatureByOverwrite(featureContentByModel, featureContentBySales, enumClass, type, true));
        // 构建问题
        if (Objects.nonNull(featureContentByModel) && !StringUtils.isEmpty(featureContentByModel.getDoubtTag())) {
            BaseFeature.CustomerQuestion customerQuestion = new BaseFeature.CustomerQuestion();
            customerQuestion.setModelRecord(featureContentByModel.getDoubtTag());
            customerQuestion.setOriginChat(CommonUtils.getOriginChatFromChatText(featureContentByModel.getCallId(), featureContentByModel.getDoubtText()));
            baseFeature.setCustomerQuestion(customerQuestion);
        }
        return baseFeature;
    }

    private TradeMethodFeature convertTradeMethodFeatureByOverwrite(CommunicationContent featureContentByModel, FeatureContentSales featureContentBySales, Class<? extends Enum<?>> enumClass, Class type) {
        return new TradeMethodFeature(convertFeatureByOverwrite(featureContentByModel, featureContentBySales, enumClass, type, false));
    }

    private Feature convertFeatureByOverwrite(CommunicationContent featureContentByModel, FeatureContentSales featureContentBySales, Class<? extends Enum<?>> enumClass, Class type, boolean isTag) {
        Feature featureVO = new Feature();
        //“已询问”有三个值：“是”、“否”、“不需要”。
        if (Objects.nonNull(featureContentByModel)) {
            //如果question 有值，就是 ‘是’;
            if (!StringUtils.isEmpty(featureContentByModel.getQuestion()) &&
                    !featureContentByModel.getQuestion().equals("无") &&
                    !featureContentByModel.getQuestion().equals("null")) {
                featureVO.setInquired("yes");
                featureVO.setInquiredOriginChat(CommonUtils.getOriginChatFromChatText(
                        StringUtils.isEmpty(featureContentByModel.getQuestionCallId()) ? featureContentByModel.getCallId() : featureContentByModel.getQuestionCallId(),
                        featureContentByModel.getQuestion()));
            }
            //如果都没有 question 或者 question 都没值，但是有 answer 有值，就是‘不需要’，这种情况下是没有原文的；
            if (featureVO.getInquired().equals("no")) {
                if (!StringUtils.isEmpty(featureContentByModel.getAnswerText()) &&
                        !featureContentByModel.getAnswerText().equals("无") &&
                        !featureContentByModel.getAnswerText().equals("null")) {
                    featureVO.setInquired("no-need");
                }
            }
        }

        // 构建结论
        Feature.CustomerConclusion customerConclusion = new Feature.CustomerConclusion();
        if (isTag) {
            if (Objects.nonNull(featureContentByModel) && !StringUtils.isEmpty(featureContentByModel.getAnswerText())) {
                // 没有候选值枚举，直接返回最后一个非空（如果存在）记录值
                if (Objects.isNull(enumClass)) {
                    customerConclusion.setModelRecord(featureContentByModel.getAnswerTag());
                    customerConclusion.setOriginChat(CommonUtils.getOriginChatFromChatText(
                            StringUtils.isEmpty(featureContentByModel.getAnswerCallId()) ? featureContentByModel.getCallId() : featureContentByModel.getAnswerCallId(),
                            featureContentByModel.getAnswerText()));
                } else {
                    // 有候选值枚举，需要比较最后一个非空记录值是否跟候选值相同，不同则返回为空
                    for (Enum<?> enumConstant : enumClass.getEnumConstants()) {
                        // 获取枚举对象的 `value` 和 `text` 字段值
                        String value = getFieldValue(enumConstant, "value");
                        String enumText = getFieldValue(enumConstant, "text");
                        // 判断文本是否匹配`text`
                        if (featureContentByModel.getAnswerTag().trim().equals(enumText)) {
                            customerConclusion.setModelRecord(value);
                            customerConclusion.setOriginChat(CommonUtils.getOriginChatFromChatText(
                                    StringUtils.isEmpty(featureContentByModel.getAnswerCallId()) ? featureContentByModel.getCallId() : featureContentByModel.getAnswerCallId(),
                                    featureContentByModel.getAnswerText()));
                        }
                    }
                }
                // 返回值类型是boolen
                if (type == Boolean.class) {
                    String resultAnswer = deletePunctuation(customerConclusion.getModelRecord());
                    if ("是".equals(resultAnswer) ||
                            "有购买意向".equals(resultAnswer) ||
                            "认可".equals(resultAnswer) ||
                            "清晰".equals(resultAnswer)) {
                        customerConclusion.setModelRecord(Boolean.TRUE);
                    } else if ("否".equals(resultAnswer) ||
                            "无购买意向".equals(resultAnswer) ||
                            "不认可".equals(resultAnswer) ||
                            "不清晰".equals(resultAnswer)) {
                        customerConclusion.setModelRecord(Boolean.FALSE);
                    } else {
                        customerConclusion.setModelRecord(null);
                    }
                }
            }
        } else {
            if (Objects.nonNull(featureContentByModel) && !StringUtils.isEmpty(featureContentByModel.getAnswerText())) {
                customerConclusion.setModelRecord(featureContentByModel.getAnswerText());
                customerConclusion.setOriginChat(CommonUtils.getOriginChatFromChatText(
                        StringUtils.isEmpty(featureContentByModel.getAnswerCallId()) ? featureContentByModel.getCallId() : featureContentByModel.getAnswerCallId(),
                        featureContentByModel.getAnswerText()));
            }
        }
        customerConclusion.setSalesRecord(Objects.isNull(featureContentBySales) ||
                StringUtils.isEmpty(featureContentBySales.getContent()) ? null : featureContentBySales.getContent());
        customerConclusion.setSalesManualTag(Objects.isNull(featureContentBySales) ||
                StringUtils.isEmpty(featureContentBySales.getTag()) ? null : featureContentBySales.getTag());
        customerConclusion.setUpdateTime(Objects.isNull(featureContentBySales) ? null : featureContentBySales.getUpdateTime());
        customerConclusion.setCompareValue(Objects.nonNull(customerConclusion.getSalesManualTag()) ? customerConclusion.getSalesManualTag() :
                customerConclusion.getModelRecord());
        featureVO.setCustomerConclusion(customerConclusion);
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

    private CustomerProcessSummary.ProcessInfoExplanationContent convertSummaryByOverwrite(CommunicationContent featureFromLLM) {
        CustomerProcessSummary.ProcessInfoExplanationContent explanationContent =
                new CustomerProcessSummary.ProcessInfoExplanationContent();
        explanationContent.setResult(Boolean.FALSE);
        // 多通电话覆盖+规则加工
        if (Objects.nonNull(featureFromLLM) &&
                !StringUtils.isEmpty(featureFromLLM.getQuestion()) &&
                !featureFromLLM.getQuestion().trim().equals("无") &&
                !featureFromLLM.getQuestion().trim().equals("null")) {
            explanationContent.setResult(Boolean.TRUE);
            explanationContent.setOriginChat(CommonUtils.getOriginChatFromChatText(
                    StringUtils.isEmpty(featureFromLLM.getQuestionCallId()) ? featureFromLLM.getCallId() : featureFromLLM.getQuestionCallId(),
                    featureFromLLM.getQuestion()));
        }
        return explanationContent;
    }

    private CustomerFeatureResponse.ProcessSummary getProcessSummary(CustomerFeatureResponse customerFeature, CustomerInfo customerInfo, CustomerStageStatus stageStatus, CustomerProcessSummary summaryResponse) {
        CustomerFeatureResponse.ProcessSummary processSummary = new CustomerFeatureResponse.ProcessSummary();
        List<String> advantage = new ArrayList<>();
        List<CustomerFeatureResponse.Question> questions = new ArrayList<>();
        try {
            // 客户客户匹配度判断
            // 优点：-完成客户匹配度判断：客户匹配度判断的值不为“未完成判断”
            // 缺点：-未完成客户匹配度判断：客户匹配度判断的值为“未完成判断”，并列出缺具体哪个字段的信息（可以用括号放在后面显示）（前提条件是通话次数大于等于1）
            String conversionRate = customerInfo.getConversionRate();
            if (!conversionRate.equals("incomplete")) {
                advantage.add("完成客户匹配度判断");
            } else if (Objects.nonNull(customerInfo.getCommunicationRounds()) &&
                    customerInfo.getCommunicationRounds() >= 2) {
                questions.add(new CustomerFeatureResponse.Question("尚未完成客户匹配度判断，需继续收集客户信息"));
            }
            // 客户交易风格了解
            // 优点：-完成客户交易风格了解：“客户交易风格了解”的值为“完成”（如果有了“提前完成客户交易风格了解”，则本条不用再判断）
            // 缺点：-未完成客户交易风格了解：“客户交易风格了解”的值为“未完成”，并列出缺具体哪个字段的信息（可以用括号放在后面显示）（前提条件是通话次数大于等于1）
            int tradingStyle = stageStatus.getTransactionStyle();
            if (tradingStyle == 1) {
                advantage.add("完成客户交易风格了解");
            } else if (Objects.nonNull(customerInfo.getCommunicationRounds()) &&
                    customerInfo.getCommunicationRounds() >= 2) {
                questions.add(new CustomerFeatureResponse.Question("尚未完成客户交易风格了解，需继续收集客户信息"));
            }
            // 跟进的客户
            // 优点：-跟进对的客户：销售跟进的是客户匹配度判断的值为“较高”或“中等”的客户
            // 缺点：-跟进错的客户：销售跟进的是客户匹配度判断的值为“较低”的客户
            if (conversionRate.equals("high") || conversionRate.equals("medium")) {
                advantage.add("跟进对的客户");
            } else if (conversionRate.equals("low")) {
                questions.add(new CustomerFeatureResponse.Question("跟进匹配度低的客户，需确认匹配度高和中的客户都已跟进完毕再跟进匹配度低的客户"));
            }

            //-SOP 执行顺序正确：阶段是逐个按顺序完成的,只在1——2——3点亮后才开始判定。也就是只有1不算，只有1——2也不算。
            //-SOP 执行顺序错误：阶段不是逐个按顺序完成的，并列出哪几个阶段未按顺序完成
            if (stageStatus.getMatchingJudgment() == 1 &&
                    stageStatus.getTransactionStyle() == 1 &&
                    stageStatus.getFunctionIntroduction() == 1) {
                if (((stageStatus.getConfirmValue() == 0 &&
                        stageStatus.getConfirmPurchase() == 0 &&
                        stageStatus.getCompletePurchase() == 0)) ||
                        ((stageStatus.getConfirmValue() == 1 &&
                                stageStatus.getConfirmPurchase() == 0 &&
                                stageStatus.getCompletePurchase() == 0)) ||
                        ((stageStatus.getConfirmValue() == 1 && stageStatus.getConfirmPurchase() == 1))) {
                    advantage.add("SOP执行顺序正确");
                }
            }
            Set<String> questionStatus = new TreeSet<>();
            if (stageStatus.getMatchingJudgment() == 0 &&
                    (stageStatus.getTransactionStyle() +
                            stageStatus.getFunctionIntroduction() +
                            stageStatus.getConfirmValue() +
                            stageStatus.getConfirmPurchase() +
                            stageStatus.getCompletePurchase()) > 0) {
                questionStatus.add("客户匹配度判断");
            }
            if (stageStatus.getTransactionStyle() == 0 &&
                    (stageStatus.getFunctionIntroduction() +
                            stageStatus.getConfirmValue() +
                            stageStatus.getConfirmPurchase() +
                            stageStatus.getCompletePurchase()) > 0) {
                questionStatus.add("客户交易风格了解");
            }
            if (stageStatus.getFunctionIntroduction() == 0 &&
                    (stageStatus.getConfirmValue() +
                            stageStatus.getConfirmPurchase() +
                            stageStatus.getCompletePurchase()) > 0) {
                questionStatus.add("针对性功能介绍");
            }
            if (stageStatus.getConfirmValue() == 0 &&
                    (stageStatus.getConfirmPurchase() + stageStatus.getCompletePurchase()) > 0) {
                questionStatus.add("客户确认价值");
            }
            if (stageStatus.getConfirmPurchase() == 0 && stageStatus.getCompletePurchase() > 0) {
                questionStatus.add("客户确认购买");
            }

            if (!CollectionUtils.isEmpty(questionStatus)) {
                StringBuilder ttt = new StringBuilder("SOP 执行顺序错误，需完成前序任务（缺失：");
                for (String status : questionStatus) {
                    ttt.append(status).append("，");
                }
                ttt.deleteCharAt(ttt.length() - 1);
                ttt.append("）");
                questions.add(new CustomerFeatureResponse.Question(ttt.toString()));
            }

            // 痛点和价值量化
            // 优点：-完成痛点和价值量化放大：字段“业务员有对客户的问题做量化放大”和“业务员有对软件的价值做量化放大”都为“是”
            // 缺点：-尚未完成痛点和价值量化放大，需后续完成：字段“业务员有对客户的问题做量化放大”和“业务员有对软件的价值做量化放大”不都为“是”（前提条件是通话次数大于等于3）
            CustomerProcessSummary.ProcessInfoExplanation infoExplanation = summaryResponse.getInfoExplanation();
            try {
                if (Objects.nonNull(infoExplanation.getCustomerIssuesQuantified()) &&
                        infoExplanation.getCustomerIssuesQuantified().getResult() &&
                        Objects.nonNull(infoExplanation.getSoftwareValueQuantified()) &&
                        infoExplanation.getSoftwareValueQuantified().getResult()) {
                    advantage.add("完成痛点和价值量化放大");
                } else if (Objects.nonNull(customerInfo.getCommunicationRounds()) &&
                        customerInfo.getCommunicationRounds() >= 3) {
                    questions.add(new CustomerFeatureResponse.Question("尚未完成痛点和价值量化放大，需后续完成"));
                }
            } catch (Exception e) {
                questions.add(new CustomerFeatureResponse.Question("尚未完成痛点和价值量化放大，需后续完成"));
            }

            // 功能讲解
            // 优点：-功能讲解让客户理解：“客户对软件功能的清晰度”的值为“是”
            // 缺点：-功能讲解未让客户理解：“客户对软件功能的清晰度”的值为“否”
            try {
                if ((Boolean) customerFeature.getBasic().getSoftwareFunctionClarity().getCustomerConclusion().getCompareValue()) {
                    advantage.add("客户对软件功能理解清晰");
                } else {
                    StringBuilder tempStr = new StringBuilder("客户对软件功能尚未理解清晰，");
                    CustomerFeatureResponse.Question question = new CustomerFeatureResponse.Question();
                    if (customerFeature.getBasic().getSoftwareFunctionClarity().getStandardProcess().equals(100)) {
                        tempStr.append("业务员已完成标准讲解，");
                        question.setComplete("业务员已完成标准讲解");
                    } else {
                        tempStr.append("业务员未完成标准讲解，");
                        question.setIncomplete("业务员未完成标准讲解");
                    }
                    question.setQuestion((String) customerFeature.getBasic().getSoftwareFunctionClarity().getCustomerQuestion().getModelRecord());
                    tempStr.append("客户问题是：").append(customerFeature.getBasic().getSoftwareFunctionClarity().getCustomerQuestion().getModelRecord()).append("，需根据客户学习能力更白话讲解");
                    question.setMessage(tempStr.toString());
                    questions.add(question);
                }
            } catch (Exception e) {
                // 有异常，说明有数据为空，不处理
            }

            // 选股方法
            // 优点：-客户认可选股方法：“客户对业务员讲的选股方法的认可度”的值为“是”
            // 缺点：-客户对选股方法尚未认可，需加强选股成功的真实案例证明：“客户对业务员讲的选股方法的认可度”的值为“否”
            try {
                if ((Boolean) customerFeature.getBasic().getStockSelectionMethod().getCustomerConclusion().getCompareValue()) {
                    advantage.add("客户认可选股方法");
                } else {
                    StringBuilder tempStr = new StringBuilder("客户对选股方法尚未认可，");
                    CustomerFeatureResponse.Question question = new CustomerFeatureResponse.Question();
                    if (customerFeature.getBasic().getStockSelectionMethod().getStandardProcess().equals(100)) {
                        tempStr.append("业务员已完成标准讲解，");
                        question.setComplete("业务员已完成标准讲解");
                    } else {
                        tempStr.append("业务员未完成标准讲解，");
                        question.setIncomplete("业务员未完成标准讲解");
                    }
                    question.setQuestion((String) customerFeature.getBasic().getStockSelectionMethod().getCustomerQuestion().getModelRecord());
                    tempStr.append("客户问题是：").append(customerFeature.getBasic().getStockSelectionMethod().getCustomerQuestion().getModelRecord()).append("，需加强选股成功的真实案例证明");
                    question.setMessage(tempStr.toString());
                    questions.add(question);
                }
            } catch (Exception e) {
                // 有异常，说明有数据为空，不处理
            }

            // 自身问题
            // 优点：-客户认可自身问题：“客户对自身问题及影响的认可度”的值为“是”
            // 缺点：-客户对自身问题尚未认可，需列举与客户相近的真实反面案例证明：“客户对自身问题及影响的认可度”的值为“否”
            try {
                if ((Boolean) customerFeature.getBasic().getSelfIssueRecognition().getCustomerConclusion().getCompareValue()) {
                    advantage.add("客户认可自身问题");
                } else {
                    StringBuilder tempStr = new StringBuilder("客户对自身问题尚未认可，");
                    CustomerFeatureResponse.Question question = new CustomerFeatureResponse.Question();
                    try {
                        if (customerFeature.getBasic().getQuantified().getCustomerIssuesQuantified().getResult()) {
                            tempStr.append("业务员已完成痛点量化放大，");
                            question.setQuantify("业务员已完成痛点量化放大");
                        } else {
                            tempStr.append("业务员未完成痛点量化放大，");
                            question.setInquantify("业务员未完成痛点量化放大");
                        }
                    } catch (Exception e) {
                        tempStr.append("业务员未完成痛点量化放大，");
                        question.setInquantify("业务员未完成痛点量化放大");
                    }
                    question.setQuestion((String) customerFeature.getBasic().getSelfIssueRecognition().getCustomerQuestion().getModelRecord());
                    tempStr.append("客户问题是：").append(customerFeature.getBasic().getSelfIssueRecognition().getCustomerQuestion().getModelRecord()).append("，需列举与客户相近的真实反面案例证明");
                    question.setMessage(tempStr.toString());
                    questions.add(question);
                }
            } catch (Exception e) {
                // 有异常，说明有数据为空，不处理
            }

            // 价值认可
            // 优点：-客户认可软件价值：字段（不是阶段）“客户对软件价值的认可度”的值为“是”
            // 缺点：-客户对软件价值尚未认可，需加强使用软件的真实成功案例证明：字段（不是阶段）“客户对软件价值的认可度”的值为“否”
            try {
                if ((Boolean) customerFeature.getBasic().getSoftwareValueApproval().getCustomerConclusion().getCompareValue()) {
                    advantage.add("客户认可软件价值");
                } else {
                    StringBuilder tempStr = new StringBuilder("客户对软件价值尚未认可，");
                    CustomerFeatureResponse.Question question = new CustomerFeatureResponse.Question();
                    try {
                        if (customerFeature.getBasic().getQuantified().getSoftwareValueQuantified().getResult()) {
                            tempStr.append("业务员已完成价值量化放大，");
                            question.setQuantify("业务员已完成价值量化放大");
                        } else {
                            tempStr.append("业务员未完成价值量化放大，");
                            question.setInquantify("业务员未完成价值量化放大");
                        }
                    } catch (Exception e) {
                        tempStr.append("业务员未完成价值量化放大，");
                        question.setInquantify("业务员未完成价值量化放大");
                    }
                    question.setQuestion((String) customerFeature.getBasic().getSoftwareValueApproval().getCustomerQuestion().getModelRecord());
                    tempStr.append("客户问题是：").append(customerFeature.getBasic().getSoftwareValueApproval().getCustomerQuestion().getModelRecord()).append("，需加强使用软件的真实成功案例证明");
                    question.setMessage(tempStr.toString());
                    questions.add(question);
                }
            } catch (Exception e) {
                // 有异常，说明有数据为空，不处理
            }

            // 优点：- 客户确认购买：字段“客户对购买软件的态度”的值为“是”
            // 缺点：- 客户拒绝购买，需暂停劝说客户购买，明确拒绝原因进行化解：字段“客户对购买软件的态度”的值为“否”
            // 优点：- 客户完成购买：阶段“客户完成购买”的值为“是”
            try {
                if ((Boolean) customerFeature.getBasic().getSoftwarePurchaseAttitude().getCustomerConclusion().getCompareValue()) {
                    advantage.add("客户确认购买");
                } else {
                    StringBuilder tempStr = new StringBuilder("客户拒绝购买，");
                    CustomerFeatureResponse.Question question = new CustomerFeatureResponse.Question();
                    question.setQuestion((String) customerFeature.getBasic().getSoftwarePurchaseAttitude().getCustomerQuestion().getModelRecord());
                    tempStr.append("客户问题是：").append(customerFeature.getBasic().getSoftwarePurchaseAttitude().getCustomerQuestion().getModelRecord()).append("，需暂停劝说客户购买，针对拒绝原因进行化解");
                    question.setMessage(tempStr.toString());
                    questions.add(question);
                }
            } catch (Exception e) {
                // 有异常，说明有数据为空，不处理
            }
            if (stageStatus.getCompletePurchase() == 1) {
                advantage.add("客户完成购买");
            }
        } catch (Exception e) {
            log.error("获取优缺点失败", e);
        }
        processSummary.setAdvantage(advantage);
        processSummary.setQuestions(questions);
        return processSummary;
    }

    private void getStandardExplanationCompletion(CustomerFeatureResponse customerFeature) {
        int completion = 0;
        if (determineTradingMethod(customerFeature.getTradingMethod().getCurrentStocks())) {
            completion += 25;
        }
        if (determineTradingMethod(customerFeature.getTradingMethod().getStockPurchaseReason())) {
            completion += 25;
        }
        if (determineTradingMethod(customerFeature.getTradingMethod().getTradeTimingDecision())) {
            completion += 25;
        }
        if (determineTradingMethod(customerFeature.getTradingMethod().getTradingStyle())) {
            completion += 25;
        }
        customerFeature.getBasic().getSoftwareFunctionClarity().setStandardProcess(completion);
        customerFeature.getBasic().getStockSelectionMethod().setStandardProcess(completion);
    }

    private Boolean determineTradingMethod(TradeMethodFeature tradeMethodFeature) {
        return tradeMethodFeature.getInquired().equals("yes")
                && Objects.nonNull(tradeMethodFeature.getCustomerConclusion())
                && !StringUtils.isEmpty(tradeMethodFeature.getCustomerConclusion().getCompareValue())
                && Objects.nonNull(tradeMethodFeature.getStandardAction())
                && Objects.nonNull(tradeMethodFeature.getStandardAction().getResult())
                && tradeMethodFeature.getStandardAction().getResult();
    }
}
