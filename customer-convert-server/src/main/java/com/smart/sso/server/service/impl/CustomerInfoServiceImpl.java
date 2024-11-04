package com.smart.sso.server.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.smart.sso.server.enums.ConfigTypeEnum;
import com.smart.sso.server.enums.EarningDesireEnum;
import com.smart.sso.server.enums.FundsVolumeEnum;
import com.smart.sso.server.enums.LearningAbilityEnum;
import com.smart.sso.server.mapper.CharacterCostTimeMapper;
import com.smart.sso.server.mapper.ConfigMapper;
import com.smart.sso.server.mapper.CustomerFeatureMapper;
import com.smart.sso.server.mapper.CustomerInfoMapper;
import com.smart.sso.server.mapper.CustomerRelationMapper;
import com.smart.sso.server.mapper.TelephoneRecordMapper;
import com.smart.sso.server.model.*;
import com.smart.sso.server.model.VO.CustomerListVO;
import com.smart.sso.server.model.VO.CustomerProfile;
import com.smart.sso.server.model.dto.*;
import com.smart.sso.server.service.CustomerInfoService;
import com.smart.sso.server.service.TelephoneRecordService;
import com.smart.sso.server.util.CommonUtils;
import com.smart.sso.server.util.DateUtil;
import com.smart.sso.server.util.JsonUtil;
import com.smart.sso.server.util.ShellUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.lang.reflect.Field;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

import static com.smart.sso.server.constant.AppConstant.SOURCEID_KEY_PREFIX;
import static com.smart.sso.server.util.CommonUtils.deletePunctuation;

@Service
@Slf4j
public class CustomerInfoServiceImpl implements CustomerInfoService {

    @Autowired
    private CustomerInfoMapper customerInfoMapper;
    @Autowired
    private CustomerFeatureMapper customerFeatureMapper;
    @Autowired
    private ConfigMapper configMapper;
    @Autowired
    private CustomerRelationMapper customerRelationMapper;
    @Autowired
    private TelephoneRecordMapper telephoneRecordMapper;
    @Autowired
    private CharacterCostTimeMapper characterCostTimeMapper;
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    @Autowired
    private TelephoneRecordService recordService;

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
        CustomerFeature featureFromSale = customerFeatureMapper.selectById(customerInfo.getId());
        CustomerFeatureFromLLM featureFromLLM = recordService.getCustomerFeatureFromLLM(customerId, activityId);

        CustomerFeatureResponse customerFeature = convert2CustomerFeatureResponse(featureFromSale, featureFromLLM);

        CustomerProfile customerProfile = convert2CustomerProfile(customerInfo);
        customerProfile.setCustomerStage(getCustomerStageStatus(customerInfo, featureFromSale, featureFromLLM));
        if (Objects.isNull(customerProfile.getCommunicationRounds())) {
            customerProfile.setCommunicationRounds(0);
        }
        // 重新判断一下匹配度，防止更新不及时的情况
        String conversionRate = getConversionRate(customerFeature);
        if (!customerInfo.getConversionRate().equals(conversionRate)) {
            customerInfoMapper.updateConversionRateById(customerInfo.getId(), conversionRate);
            customerProfile.setConversionRate(conversionRate);
        }
        customerProfile.setLastCommunicationDate(new Date(customerInfo.getUpdateTime().atZone(ZoneId.of("Asia/Shanghai")).toInstant().toEpochMilli()));
        return customerProfile;
    }

    @Override
    public CustomerFeatureResponse queryCustomerFeatureById(String customerId, String activityId) {
        CustomerInfo customerInfo = customerInfoMapper.selectByCustomerIdAndCampaignId(customerId, activityId);
        CustomerFeature featureFromSale = customerFeatureMapper.selectById(customerInfo.getId());
        CustomerFeatureFromLLM featureFromLLM = recordService.getCustomerFeatureFromLLM(customerId, activityId);
        CustomerProcessSummary summaryResponse = convert2CustomerProcessSummaryResponse(featureFromLLM, featureFromSale);
        CustomerStageStatus stageStatus = getCustomerStageStatus(customerInfo, featureFromSale, featureFromLLM);
        CustomerFeatureResponse customerFeature = convert2CustomerFeatureResponse(featureFromSale, featureFromLLM);
        customerFeature.setSummary(getProcessSummary(customerFeature, customerInfo, stageStatus, summaryResponse));
        customerFeature.setTradingMethod(summaryResponse.getTradingMethod());
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
        // -较高：资金体量=“充裕”或“大于等于10万” and 赚钱欲望=“高”
        // -中等：(资金体量=“匮乏”或“小于10万” and 赚钱欲望=“高”) or (资金体量=“充裕”或“大于等于10万” and 赚钱欲望=“低”)
        // -较低：资金体量=“匮乏”或“小于10万” and 赚钱欲望=“低”
        // -未完成判断：资金体量=空 or 赚钱欲望=空
        String result = "incomplete";
        if (Objects.isNull(customerFeature) || Objects.isNull(customerFeature.getBasic())) {
            return result;
        }
        CustomerFeatureResponse.Basic basic = customerFeature.getBasic();
        if ((Objects.isNull(basic.getFundsVolume()) ||
                Objects.isNull(basic.getFundsVolume().getCustomerConclusion())) &&
                (Objects.isNull(basic.getEarningDesire()) ||
                        Objects.isNull(basic.getEarningDesire().getCustomerConclusion()))) {
            return result;
        }
        String fundsVolume = null;
        String earningDesire = null;

        if (Objects.nonNull(basic.getFundsVolume()) &&
                Objects.nonNull(basic.getFundsVolume().getCustomerConclusion())) {
            fundsVolume = Objects.isNull(basic.getFundsVolume().getCustomerConclusion().getCompareValue()) ? null : (String) basic.getFundsVolume().getCustomerConclusion().getCompareValue();
        }
        if (Objects.nonNull(basic.getEarningDesire()) &&
                Objects.nonNull(basic.getEarningDesire().getCustomerConclusion())) {
            earningDesire = Objects.isNull(basic.getEarningDesire().getCustomerConclusion().getCompareValue()) ? null : (String) basic.getEarningDesire().getCustomerConclusion().getCompareValue();
        }

        if (StringUtils.isEmpty(fundsVolume) || StringUtils.isEmpty(earningDesire)) {
            return result;
        }
        if (fundsVolume.equals("high") && earningDesire.equals("high")) {
            return "high";
        }
        if (fundsVolume.equals("low") && earningDesire.equals("high")) {
            return "medium";
        }
        if (fundsVolume.equals("high") && earningDesire.equals("low")) {
            return "medium";
        }
        if (fundsVolume.equals("low") && earningDesire.equals("low")) {
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
            // 客户交易风格了解 相关字段全部有值——“客户当前持仓或关注的股票”、“客户为什么买这些股票”、“客户怎么决定的买卖这些股票的时机”、“客户的交易风格”、“客户的股龄”
            CustomerProcessSummary.TradingMethod tradingMethod = summaryResponse.getTradingMethod();
            try {
                if (Objects.nonNull(tradingMethod.getCurrentStocks().getCustomerConclusion().getCompareValue()) &&
                        Objects.nonNull(tradingMethod.getStockPurchaseReason().getCustomerConclusion().getCompareValue()) &&
                        Objects.nonNull(tradingMethod.getTradeTimingDecision().getCustomerConclusion().getCompareValue()) &&
                        Objects.nonNull(tradingMethod.getTradingStyle().getCustomerConclusion().getCompareValue()) &&
                        Objects.nonNull(tradingMethod.getStockMarketAge().getCustomerConclusion().getCompareValue())) {
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

        if (Objects.nonNull(summaryResponse)) {
            // 针对性功能介绍 相关字段的值全部为“是”——“销售有结合客户的股票举例”、“销售有基于客户交易风格做针对性的功能介绍”、“销售有点评客户的选股方法”、“销售有点评客户的选股时机”
            CustomerProcessSummary.ProcessInfoExplanation infoExplanation = summaryResponse.getInfoExplanation();
            if (Objects.nonNull(infoExplanation.getStock()) &&
                    infoExplanation.getStock().getResult() &&
                    Objects.nonNull(infoExplanation.getStockPickReview()) &&
                    infoExplanation.getStockPickReview().getResult() &&
                    Objects.nonNull(infoExplanation.getStockTimingReview()) &&
                    infoExplanation.getStockTimingReview().getResult() &&
                    Objects.nonNull(infoExplanation.getCustomerIssuesQuantified()) &&
                    infoExplanation.getCustomerIssuesQuantified().getResult() &&
                    Objects.nonNull(infoExplanation.getSoftwareValueQuantified()) &&
                    infoExplanation.getSoftwareValueQuantified().getResult() &&
                    Objects.nonNull(infoExplanation.getTradeBasedIntro()) &&
                    infoExplanation.getTradeBasedIntro().getResult()) {
                stageStatus.setFunctionIntroduction(1);
            }
        }
        // 客户完成购买”，规则是看客户提供的字段“成交状态”来直接判定，这个数值从数据库中提取
        try {
            QueryWrapper<CustomerRelation> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("owner_id", customerInfo.getOwnerId());
            queryWrapper.eq("customer_id", Long.parseLong(customerInfo.getCustomerId()));
            CustomerRelation customerRelation = customerRelationMapper.selectOne(queryWrapper);
            if (Objects.nonNull(customerRelation) && Objects.nonNull(customerRelation.getCustomerSigned())
                    && customerRelation.getCustomerSigned()) {
                stageStatus.setCompletePurchase(1);
            }
        } catch (Exception e) {
            log.error("判断确认购买状态失败", e);
        }
        return stageStatus;
    }

    @Override
    public List<ActivityInfo> getActivityInfoByCustomerId(String customerId) {
        return customerInfoMapper.selectActivityInfoByCustomerId(customerId);
    }

    @Override
    public void modifyCustomerFeatureById(String customerId, String activityId, CustomerFeatureResponse customerFeatureRequest) {
        CustomerInfo customerInfo = customerInfoMapper.selectByCustomerIdAndCampaignId(customerId, activityId);
        CustomerFeature customerFeature = customerFeatureMapper.selectById(customerInfo.getId());
        if (Objects.isNull(customerFeature)) {
            customerFeature = new CustomerFeature();
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
    }

    @Override
    @Async
    public void callback(String sourceId) {
        try {
            // 将sourceId 写入文件
            String filePath = "/opt/customer-convert/callback/sourceid.txt";
            CommonUtils.appendTextToFile(filePath, sourceId);
            String[] params = {sourceId};
            Process process = ShellUtils.saPythonRun("/home/opsuser/hsw/chat_insight-main/process_text.py", params.length, params);
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
        QueryWrapper<CustomerInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("customer_id", customerId);
        queryWrapper.eq("current_campaign", activeId);

        CustomerInfo customerInfo = customerInfoMapper.selectOne(queryWrapper);
        String id = "";
        if (Objects.isNull(customerInfo)) {
            log.error("获取客户失败");
        } else {
            id = customerInfo.getId();
        }
//        String urlFormatter = "http://101.42.51.62:3100/customer?id=%s";
        String urlFormatter = "https://newcmp.emoney.cn/chat/customer?id=%s&embed=true";
        if (!StringUtils.isEmpty(from)) {
            urlFormatter = urlFormatter + "&from=" + from;
        }
        if (!StringUtils.isEmpty(manager)) {
            urlFormatter = urlFormatter + "&manager=" + manager;
        }
        return String.format(urlFormatter, id);
    }

    @Override
    public List<LeadMemberRequest> addLeaderMember(List<LeadMemberRequest> members, boolean overwrite) {
        QueryWrapper<Config> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("type", ConfigTypeEnum.COMMON.getValue());
        queryWrapper.eq("name", ConfigTypeEnum.LEADER.getValue());
        Config config = configMapper.selectOne(queryWrapper);
        if (Objects.isNull(config)) {
            // 覆盖写入 或者 第一次配置，直接写入
            Config newConfig = new Config();
            newConfig.setType(ConfigTypeEnum.COMMON.getValue());
            newConfig.setName(ConfigTypeEnum.LEADER.getValue());
            newConfig.setValue(JsonUtil.serialize(members));
            configMapper.insert(newConfig);
        } else if (overwrite) {
            config.setValue(JsonUtil.serialize(members));
            configMapper.updateById(config);
        }
        return JsonUtil.readValue(configMapper.selectOne(queryWrapper).getValue(), new TypeReference<List<LeadMemberRequest>>() {
        });
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
        queryWrapperInfo.eq("current_campaign", "361");
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
        quantified.setCustomerIssuesQuantified(convertSummaryByOverwrite(featureFromLLM.getFundsVolume()));
        quantified.setSoftwareValueQuantified(convertSummaryByOverwrite(featureFromLLM.getFundsVolume()));
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
        BaseFeature baseFeature = new BaseFeature(convertFeatureByOverwrite(featureContentByModel, featureContentBySales, enumClass, type));
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
        return new TradeMethodFeature(convertFeatureByOverwrite(featureContentByModel, featureContentBySales, enumClass, type));
    }

    private Feature convertFeatureByOverwrite(CommunicationContent featureContentByModel, FeatureContentSales featureContentBySales, Class<? extends Enum<?>> enumClass, Class type) {
        Feature featureVO = new Feature();
        String resultAnswer = null;
        //“已询问”有三个值：“是”、“否”、“不需要”。
        if (Objects.nonNull(featureContentByModel)) {
            //如果question 有值，就是 ‘是’;
            if (!StringUtils.isEmpty(featureContentByModel.getQuestion()) &&
                    !featureContentByModel.getQuestion().equals("无") &&
                    !featureContentByModel.getQuestion().equals("null")) {
                featureVO.setInquired("yes");
                OriginChat originChat = new OriginChat();
                originChat.setContents(CommonUtils.getMessageListFromOriginChat(featureContentByModel.getQuestion()));
                originChat.setId(featureContentByModel.getCallId());
                featureVO.setInquiredOriginChat(originChat);
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
        if (Objects.nonNull(featureContentByModel) && !StringUtils.isEmpty(featureContentByModel.getAnswerTag())) {
            // 没有候选值枚举，直接返回最后一个非空（如果存在）记录值
            if (Objects.isNull(enumClass)) {
                customerConclusion.setModelRecord(featureContentByModel.getAnswerTag());
                customerConclusion.setOriginChat(CommonUtils.getOriginChatFromChatText(featureContentByModel.getCallId(), featureContentByModel.getAnswerText()));
            } else {
                // 有候选值枚举，需要比较最后一个非空记录值是否跟候选值相同，不同则返回为空
                for (Enum<?> enumConstant : enumClass.getEnumConstants()) {
                    // 获取枚举对象的 `value` 和 `text` 字段值
                    String value = getFieldValue(enumConstant, "value");
                    String enumText = getFieldValue(enumConstant, "text");
                    // 判断文本是否匹配`text`
                    if (featureContentByModel.getAnswerTag().trim().equals(enumText)) {
                        resultAnswer = value;
                        customerConclusion.setOriginChat(CommonUtils.getOriginChatFromChatText(featureContentByModel.getCallId(), featureContentByModel.getAnswerText()));
                    }
                }
            }
            // 返回值类型是boolen
            if (type == Boolean.class) {
                resultAnswer = deletePunctuation(resultAnswer);
                if ("是".equals(resultAnswer) || "有购买意向".equals(resultAnswer)) {
                    customerConclusion.setModelRecord(Boolean.TRUE);
                } else {
                    if ("否".equals(resultAnswer) || "无购买意向".equals(resultAnswer)) {
                        customerConclusion.setModelRecord(Boolean.FALSE);
                    }
                }
            } else {
                customerConclusion.setModelRecord(resultAnswer);
            }
        }
        customerConclusion.setSalesRecord(Objects.isNull(featureContentBySales) ? null : featureContentBySales.getContent());
        customerConclusion.setSalesManualTag(Objects.isNull(featureContentBySales) ? null : featureContentBySales.getTag());
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
        if (Objects.isNull(featureFromLLM)) {
            explanationContent.setResult(Boolean.FALSE);
            return explanationContent;
        }
        // 多通电话覆盖+规则加工
        if (!StringUtils.isEmpty(featureFromLLM.getQuestion().trim())) {
            explanationContent.setResult(Boolean.TRUE);
            explanationContent.setOriginChat(CommonUtils.getOriginChatFromChatText(featureFromLLM.getCallId(), featureFromLLM.getQuestion()));
            return explanationContent;
        }
        return explanationContent;
    }

    private List<SummaryContent> dataPreprocess(List<SummaryContent> summaryContentList) {
        if (CollectionUtils.isEmpty(summaryContentList)) {
            return summaryContentList;
        }
        Map<String, SummaryContent> keySummaryContent = new TreeMap<>();
        for (SummaryContent item : summaryContentList) {
            if (!keySummaryContent.containsKey(item.getCallId())) {
                keySummaryContent.put(item.getCallId(), item);
            }
        }
        return new ArrayList<>(keySummaryContent.values());
    }

    private CustomerFeatureResponse.ProcessSummary getProcessSummary(CustomerFeatureResponse customerFeature, CustomerInfo customerInfo, CustomerStageStatus stageStatus, CustomerProcessSummary summaryResponse) {
        CustomerFeatureResponse.ProcessSummary processSummary = new CustomerFeatureResponse.ProcessSummary();
        List<String> advantage = new ArrayList<>();
        List<String> questions = new ArrayList<>();

        // 客户客户匹配度判断
        try {
            String conversionRate = customerInfo.getConversionRate();
            // 优点：-提前完成客户匹配度判断：通话次数等于0 and 客户匹配度判断的值不为“未完成判断”
            // 优点：-完成客户匹配度判断：客户匹配度判断的值不为“未完成判断”（如果有了“提前完成客户匹配度判断”，则本条不用再判断）
            // - 未完成客户匹配度判断：客户匹配度判断的值为“未完成判断”，并列出缺具体哪个字段的信息（可以用括号放在后面显示）（前提条件是通话次数大于等于1）
            if ((Objects.isNull(customerInfo.getCommunicationRounds()) ||
                    customerInfo.getCommunicationRounds().equals(0))
                    && !conversionRate.equals("incomplete")) {
                advantage.add("提前完成客户匹配度判断");
            } else {
                if (!conversionRate.equals("incomplete")) {
                    advantage.add("完成客户匹配度判断");
                } else {
                    if (conversionRate.equals("incomplete") &&
                            Objects.nonNull(customerInfo.getCommunicationRounds()) &&
                            customerInfo.getCommunicationRounds() >= 2) {
                        questions.add("尚未完成客户匹配度判断，需继续收集客户信息");
                    }
                }
            }

            // 客户交易风格了解
            // 优点：-提前完成客户交易风格了解：通话次数等于0 and “客户交易风格了解”的值为“完成”
            // 优点：-完成客户交易风格了解：“客户交易风格了解”的值为“完成”（如果有了“提前完成客户交易风格了解”，则本条不用再判断）
            // 缺点：-未完成客户交易风格了解：“客户交易风格了解”的值为“未完成”，并列出缺具体哪个字段的信息（可以用括号放在后面显示）（前提条件是通话次数大于等于1）
            int tradingStyle = stageStatus.getTransactionStyle();
            if ((Objects.isNull(customerInfo.getCommunicationRounds()) ||
                    customerInfo.getCommunicationRounds().equals(0))
                    && tradingStyle == 1) {
                advantage.add("提前完成客户交易风格了解");
            } else {
                if (tradingStyle == 1) {
                    advantage.add("完成客户交易风格了解");
                } else {
                    if (tradingStyle == 0 &&
                            Objects.nonNull(customerInfo.getCommunicationRounds()) &&
                            customerInfo.getCommunicationRounds() >= 2) {
                        questions.add("尚未完成客户交易风格了解，需继续收集客户信息");
                    }
                }
            }

            // 跟进的客户
            // 优点：-跟进对的客户：销售跟进的是客户匹配度判断的值为“较高”或“中等”的客户
            // 缺点：-跟进错的客户：销售跟进的是客户匹配度判断的值为“较低”的客户
            if (conversionRate.equals("high") || conversionRate.equals("medium")) {
                advantage.add("跟进对的客户");
            } else if (conversionRate.equals("low")) {
                questions.add("跟进匹配度低的客户，需确认匹配度高和中的客户都已跟进完毕再跟进匹配度低的客户");
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
                questionStatus.add("客户判断");
            }
            if (stageStatus.getTransactionStyle() == 0 &&
                    (stageStatus.getFunctionIntroduction() +
                            stageStatus.getConfirmValue() +
                            stageStatus.getConfirmPurchase() +
                            stageStatus.getCompletePurchase()) > 0) {
                questionStatus.add("交易风格了解");
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
                questions.add(ttt.toString());
            }

            // 痛点和价值量化
            // 优点：-完成痛点和价值量化放大：字段“业务员有对客户的问题做量化放大”和“业务员有对软件的价值做量化放大”都为“是”
            // 缺点：-尚未完成痛点和价值量化放大，需后续完成：字段“业务员有对客户的问题做量化放大”和“业务员有对软件的价值做量化放大”不都为“是”（前提条件是通话次数大于等于3）
            CustomerProcessSummary.ProcessInfoExplanation infoExplanation = summaryResponse.getInfoExplanation();
            if (Objects.nonNull(infoExplanation.getCustomerIssuesQuantified()) &&
                    infoExplanation.getCustomerIssuesQuantified().getResult() &&
                    Objects.nonNull(infoExplanation.getSoftwareValueQuantified()) &&
                    infoExplanation.getSoftwareValueQuantified().getResult()) {
                advantage.add("完成痛点和价值量化放大");
            } else if (Objects.nonNull(customerInfo.getCommunicationRounds()) &&
                    customerInfo.getCommunicationRounds() >= 3) {
                questions.add("尚未完成痛点和价值量化放大，需后续完成");
            }

            // 功能讲解
            // 优点：-功能讲解让客户理解：“客户对软件功能的清晰度”的值为“是”
            // 缺点：-功能讲解未让客户理解：“客户对软件功能的清晰度”的值为“否”
            try {
                if ((Boolean) customerFeature.getBasic().getSoftwareFunctionClarity().getCustomerConclusion().getCompareValue()) {
                    advantage.add("客户对软件功能理解清晰");
                } else {
                    questions.add("客户对软件功能尚未理解清晰，需根据客户学习能力更白话讲解");
                }
            } catch (Exception e) {
                // 有异常，说明有数据为空，不处理
            }

            // 选股方法
            // 优点：-客户认可选股方法：“客户对业务员讲的选股方法的认可度”的值为“是”
            // 缺点：-客户对选股方法尚未认可，需加强选股成功的真实案例证明：“客户对业务员讲的选股方法的认可度”的值为“否”
            try {
                if (Objects.nonNull(customerFeature.getBasic().getStockSelectionMethod().getCustomerConclusion().getCompareValue())) {
                    advantage.add("客户认可选股方法");
                } else {
                    questions.add("客户对选股方法尚未认可，需加强选股成功的真实案例证明");
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
                    questions.add("客户对自身问题尚未认可，需列举与客户相近的真实反面案例证明");
                }
            } catch (Exception e) {
                // 有异常，说明有数据为空，不处理
            }

            // 价值认可
            // 优点：-客户认可软件价值：字段（不是阶段）“客户对软件价值的认可度”的值为“是”
            // 缺点：-客户对软件价值尚未认可，需加强使用软件的真实成功案例证明：字段（不是阶段）“客户对软件价值的认可度”的值为“否”
            if ((Boolean) customerFeature.getBasic().getSoftwareValueApproval().getCustomerConclusion().getCompareValue()) {
                advantage.add("客户认可软件价值");
            } else {
                questions.add("客户对软件价值尚未认可，需加强使用软件的真实成功案例证明");
            }


            // 优点：- 客户确认购买：字段“客户对购买软件的态度”的值为“是”
            // 缺点：- 客户拒绝购买，需暂停劝说客户购买，明确拒绝原因进行化解：字段“客户对购买软件的态度”的值为“否”
            // 优点：- 客户完成购买：阶段“客户完成购买”的值为“是”
            if ((Boolean) customerFeature.getBasic().getSoftwarePurchaseAttitude().getCustomerConclusion().getCompareValue()) {
                advantage.add("客户确认购买");
            } else {
                questions.add("客户拒绝购买，需暂停劝说客户购买，明确拒绝原因进行化解");
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

}
