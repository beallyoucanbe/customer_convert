package com.smart.sso.server.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.smart.sso.server.enums.ConfigTypeEnum;
import com.smart.sso.server.enums.CustomerRecognition;
import com.smart.sso.server.enums.EarningDesireEnum;
import com.smart.sso.server.enums.FundsVolumeEnum;
import com.smart.sso.server.enums.LearningAbilityEnum;
import com.smart.sso.server.enums.ProfitLossEnum;
import com.smart.sso.server.mapper.ConfigMapper;
import com.smart.sso.server.mapper.CustomerFeatureMapper;
import com.smart.sso.server.mapper.CustomerInfoMapper;
import com.smart.sso.server.mapper.CustomerRelationMapper;
import com.smart.sso.server.mapper.CustomerSummaryMapper;
import com.smart.sso.server.model.*;
import com.smart.sso.server.model.VO.CustomerListVO;
import com.smart.sso.server.model.VO.CustomerProfile;
import com.smart.sso.server.model.dto.CustomerFeatureResponse;
import com.smart.sso.server.model.dto.CustomerInfoListRequest;
import com.smart.sso.server.model.dto.CustomerInfoListResponse;
import com.smart.sso.server.model.dto.CustomerProcessSummaryResponse;
import com.smart.sso.server.model.dto.LeadMemberRequest;
import com.smart.sso.server.service.CustomerInfoService;
import com.smart.sso.server.util.CommonUtils;
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

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
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
    private CustomerSummaryMapper customerSummaryMapper;
    @Autowired
    private ConfigMapper configMapper;
    @Autowired
    private CustomerRelationMapper customerRelationMapper;
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    private SimpleDateFormat dateFormat1 = new SimpleDateFormat("yyyy-MM-dd HH:mm");
    private SimpleDateFormat dateFormat2 = new SimpleDateFormat("yyyy-MM-dd HH");
    private SimpleDateFormat dateFormat3 = new SimpleDateFormat("yyyy-MM-dd");


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
    public CustomerProfile queryCustomerById(String id) {
        CustomerInfo customerInfo = customerInfoMapper.selectById(id);
        CustomerFeature customerFeature = customerFeatureMapper.selectById(id);
        CustomerSummary customerSummary = customerSummaryMapper.selectById(id);
        CustomerProfile customerProfile = convert2CustomerProfile(customerInfo);
        customerProfile.setCustomerStage(getCustomerStageStatus(customerInfo, customerFeature, customerSummary));
        if (Objects.isNull(customerProfile.getCommunicationRounds())) {
            customerProfile.setCommunicationRounds(0);
        }
        // 重新判断一下匹配度，防止更新不及时的情况
        String conversionRate = getConversionRate(customerFeature);
        if (!customerInfo.getConversionRate().equals(conversionRate)) {
            customerInfoMapper.updateConversionRateById(id, conversionRate);
            customerProfile.setConversionRate(conversionRate);
        }
        customerProfile.setLastCommunicationDate(new Date(customerInfo.getUpdateTime().atZone(ZoneId.of("Asia/Shanghai")).toInstant().toEpochMilli()));
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
        CustomerStageStatus stageStatus = getCustomerStageStatus(customerInfo, customerFeature, customerSummary);
        if (Objects.nonNull(summaryResponse)) {
            summaryResponse.setSummary(getProcessSummary(customerFeature, customerInfo, stageStatus, summaryResponse));
        }
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
        if (Objects.isNull(customerFeature)) {
            return result;
        }
        List<FeatureContent> fundsVolumeModel = customerFeature.getFundsVolumeModel();
        String fundsVolumeSales = Objects.nonNull(customerFeature.getFundsVolumeSales()) &&
                Objects.nonNull(customerFeature.getFundsVolumeSales().getTag())
                ? customerFeature.getFundsVolumeSales().getTag().toString() : null;
        List<FeatureContent> earningDesireModel = customerFeature.getEarningDesireModel();
        String earningDesireSales = Objects.nonNull(customerFeature.getEarningDesireSales()) &&
                Objects.nonNull(customerFeature.getEarningDesireSales().getTag())
                ? customerFeature.getEarningDesireSales().getTag().toString() : null;
        String fundsVolume = null;
        String earningDesire = null;
        if (!CollectionUtils.isEmpty(fundsVolumeModel)) {
            // 找到最后一个非null的值
            for (int i = fundsVolumeModel.size() - 1; i >= 0; i--) {
                if (!StringUtils.isEmpty(fundsVolumeModel.get(i).getAnswer())) {
                    fundsVolume = fundsVolumeModel.get(i).getAnswer();
                    if (!fundsVolume.equals("无") && !fundsVolume.equals("null")) {
                        break;
                    }
                }
            }
        }
        if (!CollectionUtils.isEmpty(earningDesireModel)) {
            for (int i = earningDesireModel.size() - 1; i >= 0; i--) {
                if (!StringUtils.isEmpty(earningDesireModel.get(i).getAnswer())) {
                    earningDesire = earningDesireModel.get(i).getAnswer();
                    if (!earningDesire.equals("无") && !earningDesire.equals("null")) {
                        break;
                    }
                }
            }
        }
        if ((StringUtils.isEmpty(fundsVolume) && StringUtils.isEmpty(fundsVolumeSales)) ||
                (StringUtils.isEmpty(earningDesire) && StringUtils.isEmpty(earningDesireSales))) {
            return result;
        }
        String fundsVolumeStatus = null;
        String earningDesireStatus = null;

        if (StringUtils.isEmpty(fundsVolumeSales)) {
            if (fundsVolume.equals("充裕") || fundsVolume.equals("大于等于5万")) {
                fundsVolumeStatus = "high";
            } else if (fundsVolume.equals("匮乏") || fundsVolume.equals("小于5万")) {
                fundsVolumeStatus = "low";
            }
        } else {
            if (fundsVolumeSales.equals("abundant") || fundsVolumeSales.equals("great_equal_five_w")) {
                fundsVolumeStatus = "high";
            } else if (fundsVolumeSales.equals("deficient") || fundsVolumeSales.equals("less_five_w")) {
                fundsVolumeStatus = "low";
            }
        }

        if (StringUtils.isEmpty(earningDesireSales)) {
            if (earningDesire.equals("强")) {
                earningDesireStatus = "high";
            } else if (earningDesire.equals("弱")) {
                earningDesireStatus = "low";
            }
        } else {
            if (earningDesireSales.equals("high")) {
                earningDesireStatus = "high";
            } else if (earningDesireSales.equals("low")) {
                earningDesireStatus = "low";
            }
        }

        if (StringUtils.isEmpty(fundsVolumeStatus) || StringUtils.isEmpty(earningDesireStatus)) {
            return result;
        }
        if (fundsVolumeStatus.equals("high") && earningDesireStatus.equals("high")) {
            return "high";
        }
        if (fundsVolumeStatus.equals("low") && earningDesireStatus.equals("high")) {
            return "medium";
        }
        if (fundsVolumeStatus.equals("high") && earningDesireStatus.equals("low")) {
            return "medium";
        }
        if (fundsVolumeStatus.equals("low") && earningDesireStatus.equals("low")) {
            return "low";
        }
        return result;
    }

    @Override
    public CustomerStageStatus getCustomerStageStatus(CustomerInfo customerInfo, CustomerFeature customerFeature, CustomerSummary customerSummary) {
        CustomerFeatureResponse customerFeatureResponse = convert2CustomerFeatureResponse(customerFeature);
        CustomerProcessSummaryResponse summaryResponse = convert2CustomerProcessSummaryResponse(customerSummary);
        CustomerStageStatus stageStatus = new CustomerStageStatus();
        // 客户匹配度判断 值不为“未完成判断”
        if (!"incomplete".equals(getConversionRate(customerFeature))) {
            stageStatus.setMatchingJudgment(1);
        }

        if (Objects.nonNull(customerFeatureResponse)) {
            // 客户交易风格了解 相关字段全部有值——“客户当前持仓或关注的股票”、“客户为什么买这些股票”、“客户怎么决定的买卖这些股票的时机”、“客户的交易风格”、“客户的股龄”
            CustomerFeatureResponse.TradingMethod tradingMethod = customerFeatureResponse.getTradingMethod();
            if (Objects.nonNull(tradingMethod.getCurrentStocks().getCompareValue()) &&
                    Objects.nonNull(tradingMethod.getStockPurchaseReason().getCompareValue()) &&
                    Objects.nonNull(tradingMethod.getTradeTimingDecision().getCompareValue()) &&
                    Objects.nonNull(tradingMethod.getTradingStyle().getCompareValue()) &&
                    Objects.nonNull(tradingMethod.getStockMarketAge().getCompareValue())) {
                stageStatus.setTransactionStyle(1);
            }
            // 客户确认价值 相关字段的值全部为“是”——“客户对软件功能的清晰度”、“客户对销售讲的选股方法的认可度”、“客户对自身问题及影响的认可度”、“客户对软件价值的认可度”
            CustomerFeatureResponse.Recognition recognition = customerFeatureResponse.getRecognition();
            if (Objects.nonNull(recognition.getSoftwareFunctionClarity().getCompareValue()) &&
                    (Boolean) recognition.getSoftwareFunctionClarity().getCompareValue() &&
                    Objects.nonNull(recognition.getStockSelectionMethod().getCompareValue()) &&
                    (Boolean) recognition.getStockSelectionMethod().getCompareValue() &&
                    Objects.nonNull(recognition.getSelfIssueRecognition().getCompareValue()) &&
                    (Boolean) recognition.getSelfIssueRecognition().getCompareValue() &&
                    Objects.nonNull(recognition.getLearnNewMethodApproval().getCompareValue()) &&
                    (Boolean) recognition.getLearnNewMethodApproval().getCompareValue() &&
                    Objects.nonNull(recognition.getContinuousLearnApproval().getCompareValue()) &&
                    (Boolean) recognition.getContinuousLearnApproval().getCompareValue() &&
                    Objects.nonNull(recognition.getSoftwareValueApproval().getCompareValue()) &&
                    (Boolean) recognition.getSoftwareValueApproval().getCompareValue()) {
                stageStatus.setConfirmValue(1);
            }
            // 客户确认购买 客户对购买软件的态度”的值为“是”
            if (Objects.nonNull(recognition.getSoftwarePurchaseAttitude().getCompareValue()) &&
                    (Boolean) recognition.getSoftwarePurchaseAttitude().getCompareValue()) {
                stageStatus.setConfirmPurchase(1);
            }
        }

        if (Objects.nonNull(summaryResponse)) {
            // 针对性功能介绍 相关字段的值全部为“是”——“销售有结合客户的股票举例”、“销售有基于客户交易风格做针对性的功能介绍”、“销售有点评客户的选股方法”、“销售有点评客户的选股时机”
            CustomerProcessSummaryResponse.ProcessInfoExplanation infoExplanation = summaryResponse.getInfoExplanation();
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
    public void modifyCustomerFeatureById(String id, CustomerFeatureResponse customerFeatureRequest) {
        CustomerFeature customerFeature = customerFeatureMapper.selectById(id);
        if (Objects.isNull(customerFeature)) {
            throw new RuntimeException("客户不存在");
        }
        if (Objects.nonNull(customerFeatureRequest.getBasic())) {
            if (Objects.nonNull(customerFeatureRequest.getBasic().getFundsVolume()) &&
                    (Objects.nonNull(customerFeatureRequest.getBasic().getFundsVolume().getSalesRecord()) ||
                            Objects.nonNull(customerFeatureRequest.getBasic().getFundsVolume().getSalesManualTag()))) {
                customerFeature.setFundsVolumeSales(new FeatureContentSales(customerFeatureRequest.getBasic().getFundsVolume().getSalesRecord(),
                        customerFeatureRequest.getBasic().getFundsVolume().getSalesManualTag()));
            }
            if (Objects.nonNull(customerFeatureRequest.getBasic().getProfitLossSituation()) &&
                    (Objects.nonNull(customerFeatureRequest.getBasic().getProfitLossSituation().getSalesRecord()) ||
                            Objects.nonNull(customerFeatureRequest.getBasic().getProfitLossSituation().getSalesManualTag()))) {
                customerFeature.setProfitLossSituationSales(new FeatureContentSales(customerFeatureRequest.getBasic().getProfitLossSituation().getSalesRecord(),
                        customerFeatureRequest.getBasic().getProfitLossSituation().getSalesManualTag()));
            }
            if (Objects.nonNull(customerFeatureRequest.getBasic().getEarningDesire()) &&
                    (Objects.nonNull(customerFeatureRequest.getBasic().getEarningDesire().getSalesRecord()) ||
                            Objects.nonNull(customerFeatureRequest.getBasic().getEarningDesire().getSalesManualTag()))) {
                customerFeature.setEarningDesireSales(new FeatureContentSales(customerFeatureRequest.getBasic().getEarningDesire().getSalesRecord(),
                        customerFeatureRequest.getBasic().getEarningDesire().getSalesManualTag()));
            }
        }

        if (Objects.nonNull(customerFeatureRequest.getTradingMethod())) {
            if (Objects.nonNull(customerFeatureRequest.getTradingMethod().getCurrentStocks()) &&
                    (Objects.nonNull(customerFeatureRequest.getTradingMethod().getCurrentStocks().getSalesRecord()) ||
                            Objects.nonNull(customerFeatureRequest.getTradingMethod().getCurrentStocks().getSalesManualTag()))) {
                customerFeature.setCurrentStocksSales(new FeatureContentSales(customerFeatureRequest.getTradingMethod().getCurrentStocks().getSalesRecord(),
                        customerFeatureRequest.getTradingMethod().getCurrentStocks().getSalesManualTag()));
            }
            if (Objects.nonNull(customerFeatureRequest.getTradingMethod().getStockPurchaseReason()) &&
                    (Objects.nonNull(customerFeatureRequest.getTradingMethod().getStockPurchaseReason().getSalesRecord()) ||
                            Objects.nonNull(customerFeatureRequest.getTradingMethod().getStockPurchaseReason().getSalesManualTag()))) {
                customerFeature.setStockPurchaseReasonSales(new FeatureContentSales(customerFeatureRequest.getTradingMethod().getStockPurchaseReason().getSalesRecord(),
                        customerFeatureRequest.getTradingMethod().getStockPurchaseReason().getSalesManualTag()));
            }
            if (Objects.nonNull(customerFeatureRequest.getTradingMethod().getTradeTimingDecision()) &&
                    (Objects.nonNull(customerFeatureRequest.getTradingMethod().getTradeTimingDecision().getSalesRecord()) ||
                            Objects.nonNull(customerFeatureRequest.getTradingMethod().getTradeTimingDecision().getSalesManualTag()))) {
                customerFeature.setTradeTimingDecisionSales(new FeatureContentSales(customerFeatureRequest.getTradingMethod().getTradeTimingDecision().getSalesRecord(),
                        customerFeatureRequest.getTradingMethod().getTradeTimingDecision().getSalesManualTag()));
            }
            if (Objects.nonNull(customerFeatureRequest.getTradingMethod().getLearningAbility()) &&
                    (Objects.nonNull(customerFeatureRequest.getTradingMethod().getLearningAbility().getSalesRecord()) ||
                            Objects.nonNull(customerFeatureRequest.getTradingMethod().getLearningAbility().getSalesManualTag()))) {
                customerFeature.setLearningAbilitySales(new FeatureContentSales(customerFeatureRequest.getTradingMethod().getLearningAbility().getSalesRecord(),
                        customerFeatureRequest.getTradingMethod().getLearningAbility().getSalesManualTag()));
            }
            if (Objects.nonNull(customerFeatureRequest.getTradingMethod().getStockMarketAge()) &&
                    (Objects.nonNull(customerFeatureRequest.getTradingMethod().getStockMarketAge().getSalesRecord()) ||
                            Objects.nonNull(customerFeatureRequest.getTradingMethod().getStockMarketAge().getSalesManualTag()))) {
                customerFeature.setStockMarketAgeSales(new FeatureContentSales(customerFeatureRequest.getTradingMethod().getStockMarketAge().getSalesRecord(),
                        customerFeatureRequest.getTradingMethod().getStockMarketAge().getSalesManualTag()));
            }
            if (Objects.nonNull(customerFeatureRequest.getTradingMethod().getTradingStyle()) &&
                    (Objects.nonNull(customerFeatureRequest.getTradingMethod().getTradingStyle().getSalesRecord()) ||
                            Objects.nonNull(customerFeatureRequest.getTradingMethod().getTradingStyle().getSalesManualTag()))) {
                customerFeature.setTradingStyleSales(new FeatureContentSales(customerFeatureRequest.getTradingMethod().getTradingStyle().getSalesRecord(),
                        customerFeatureRequest.getTradingMethod().getTradingStyle().getSalesManualTag()));
            }
        }

        if (Objects.nonNull(customerFeatureRequest.getRecognition())) {
            if (Objects.nonNull(customerFeatureRequest.getRecognition().getCourseTeacherApproval()) &&
                    (Objects.nonNull(customerFeatureRequest.getRecognition().getCourseTeacherApproval().getSalesRecord()) ||
                            Objects.nonNull(customerFeatureRequest.getRecognition().getCourseTeacherApproval().getSalesManualTag()))) {
                customerFeature.setCourseTeacherApprovalSales(new FeatureContentSales(customerFeatureRequest.getRecognition().getCourseTeacherApproval().getSalesRecord(),
                        customerFeatureRequest.getRecognition().getCourseTeacherApproval().getSalesManualTag()));
            }
            if (Objects.nonNull(customerFeatureRequest.getRecognition().getSelfIssueRecognition()) &&
                    (Objects.nonNull(customerFeatureRequest.getRecognition().getSelfIssueRecognition().getSalesRecord()) ||
                            Objects.nonNull(customerFeatureRequest.getRecognition().getSelfIssueRecognition().getSalesManualTag()))) {
                customerFeature.setSelfIssueRecognitionSales(new FeatureContentSales(customerFeatureRequest.getRecognition().getSelfIssueRecognition().getSalesRecord(),
                        customerFeatureRequest.getRecognition().getSelfIssueRecognition().getSalesManualTag()));
            }
            if (Objects.nonNull(customerFeatureRequest.getRecognition().getSoftwareFunctionClarity()) &&
                    (Objects.nonNull(customerFeatureRequest.getRecognition().getSoftwareFunctionClarity().getSalesRecord()) ||
                            Objects.nonNull(customerFeatureRequest.getRecognition().getSoftwareFunctionClarity().getSalesManualTag()))) {
                customerFeature.setSoftwareFunctionClaritySales(new FeatureContentSales(customerFeatureRequest.getRecognition().getSoftwareFunctionClarity().getSalesRecord(),
                        customerFeatureRequest.getRecognition().getSoftwareFunctionClarity().getSalesManualTag()));
            }
            if (Objects.nonNull(customerFeatureRequest.getRecognition().getSoftwarePurchaseAttitude()) &&
                    (Objects.nonNull(customerFeatureRequest.getRecognition().getSoftwarePurchaseAttitude().getSalesRecord()) ||
                            Objects.nonNull(customerFeatureRequest.getRecognition().getSoftwarePurchaseAttitude().getSalesManualTag()))) {
                customerFeature.setSoftwarePurchaseAttitudeSales(new FeatureContentSales(customerFeatureRequest.getRecognition().getSoftwarePurchaseAttitude().getSalesRecord(),
                        customerFeatureRequest.getRecognition().getSoftwarePurchaseAttitude().getSalesManualTag()));
            }
            if (Objects.nonNull(customerFeatureRequest.getRecognition().getContinuousLearnApproval()) &&
                    (Objects.nonNull(customerFeatureRequest.getRecognition().getContinuousLearnApproval().getSalesRecord()) ||
                            Objects.nonNull(customerFeatureRequest.getRecognition().getContinuousLearnApproval().getSalesManualTag()))) {
                customerFeature.setContinuousLearnApprovalSales(new FeatureContentSales(customerFeatureRequest.getRecognition().getContinuousLearnApproval().getSalesRecord(),
                        customerFeatureRequest.getRecognition().getContinuousLearnApproval().getSalesManualTag()));
            }
            if (Objects.nonNull(customerFeatureRequest.getRecognition().getLearnNewMethodApproval()) &&
                    (Objects.nonNull(customerFeatureRequest.getRecognition().getLearnNewMethodApproval().getSalesRecord()) ||
                            Objects.nonNull(customerFeatureRequest.getRecognition().getLearnNewMethodApproval().getSalesManualTag()))) {
                customerFeature.setLearnNewMethodApprovalSales(new FeatureContentSales(customerFeatureRequest.getRecognition().getLearnNewMethodApproval().getSalesRecord(),
                        customerFeatureRequest.getRecognition().getLearnNewMethodApproval().getSalesManualTag()));
            }
            if (Objects.nonNull(customerFeatureRequest.getRecognition().getSoftwareValueApproval()) &&
                    (Objects.nonNull(customerFeatureRequest.getRecognition().getSoftwareValueApproval().getSalesRecord()) ||
                            Objects.nonNull(customerFeatureRequest.getRecognition().getSoftwareValueApproval().getSalesManualTag()))) {
                customerFeature.setSoftwareValueApprovalSales(new FeatureContentSales(customerFeatureRequest.getRecognition().getSoftwareValueApproval().getSalesRecord(),
                        customerFeatureRequest.getRecognition().getSoftwareValueApproval().getSalesManualTag()));
            }
            if (Objects.nonNull(customerFeatureRequest.getRecognition().getStockSelectionMethod()) &&
                    (Objects.nonNull(customerFeatureRequest.getRecognition().getStockSelectionMethod().getSalesRecord()) ||
                            Objects.nonNull(customerFeatureRequest.getRecognition().getStockSelectionMethod().getSalesManualTag()))) {
                customerFeature.setStockSelectionMethodSales(new FeatureContentSales(customerFeatureRequest.getRecognition().getStockSelectionMethod().getSalesRecord(),
                        customerFeatureRequest.getRecognition().getStockSelectionMethod().getSalesManualTag()));
            }
        }
        if (Objects.nonNull(customerFeatureRequest.getNote())) {
            customerFeature.setNote(customerFeatureRequest.getNote());
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
//        else {
//            // 增量写入
//            List<LeadMemberRequest> listMap = JsonUtil.readValue(config.getValue(), new TypeReference<List<LeadMemberRequest>>() {
//            });
//            HashMap<String, LeadMemberRequest> areaSet = listMap.stream().map(LeadMemberRequest::getArea).collect(Collectors.toSet());
//            for (LeadMemberRequest item : members) {
//                if (areaSet.contains(item.getArea())) {
//
//                    List<String> membersOld = listMap.get(entry.getKey());
//                    for (String member : entry.getValue()) {
//                        if (!membersOld.contains(member)) {
//                            membersOld.add(member);
//                        }
//                    }
//                } else {
//                    listMap.add(item);
//                }
//            }
//            config.setValue(JsonUtil.serialize(listMap));
//            configMapper.updateById(config);
//        }
        return JsonUtil.readValue(configMapper.selectOne(queryWrapper).getValue(), new TypeReference<List<LeadMemberRequest>>() {
        });
    }

    @Override
    public String getChatContent(String path) {
        String filePath = "/opt/customer-convert/callback/files/" + path; // 文件路径
        StringBuilder content = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = br.readLine()) != null) {
                content.append(line).append(System.lineSeparator());
            }
        } catch (IOException e) {
            log.error("读取文件失败：", e);
        }
        return content.toString();
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
        tradingMethod.setLearningAbility(convertFeatureByOverwrite(customerFeature.getLearningAbilityModel(), customerFeature.getLearningAbilitySales(), LearningAbilityEnum.class, String.class));
        customerFeatureResponse.setTradingMethod(tradingMethod);

        // Recognition 客户认可度
        CustomerFeatureResponse.Recognition recognition = new CustomerFeatureResponse.Recognition();
        recognition.setCourseTeacherApproval(convertFeatureByOverwrite(customerFeature.getCourseTeacherApprovalModel(), customerFeature.getCourseTeacherApprovalSales(), null, Boolean.class));
        recognition.setSoftwareFunctionClarity(convertFeatureByOverwrite(customerFeature.getSoftwareFunctionClarityModel(), customerFeature.getSoftwareFunctionClaritySales(), null, Boolean.class));
        recognition.setStockSelectionMethod(convertFeatureByOverwrite(customerFeature.getStockSelectionMethodModel(), customerFeature.getStockSelectionMethodSales(), null, Boolean.class));
        recognition.setSelfIssueRecognition(convertFeatureByOverwrite(customerFeature.getSelfIssueRecognitionModel(), customerFeature.getSelfIssueRecognitionSales(), null, Boolean.class));
        recognition.setContinuousLearnApproval(convertFeatureByOverwrite(customerFeature.getContinuousLearnApprovalModel(), customerFeature.getContinuousLearnApprovalSales(), null, Boolean.class));
        recognition.setLearnNewMethodApproval(convertFeatureByOverwrite(customerFeature.getLearnNewMethodApprovalModel(), customerFeature.getLearnNewMethodApprovalSales(), null, Boolean.class));
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

        CustomerProcessSummaryResponse.ProcessInfoExplanation infoExplanation = new CustomerProcessSummaryResponse.ProcessInfoExplanation();
        infoExplanation.setStock(convertSummaryByOverwrite(customerSummary.getIllustrateBasedStock()));
        infoExplanation.setTradeBasedIntro(convertSummaryByOverwrite(customerSummary.getTradeStyleIntroduce()));
        infoExplanation.setStockPickReview(convertSummaryByOverwrite(customerSummary.getStockPickMethodReview()));
        infoExplanation.setStockTimingReview(convertSummaryByOverwrite(customerSummary.getStockPickTimingReview()));
        infoExplanation.setSoftwareValueQuantified(convertSummaryByOverwrite(customerSummary.getSoftwareValueQuantified()));
        infoExplanation.setCustomerIssuesQuantified(convertSummaryByOverwrite(customerSummary.getCustomerIssuesQuantified()));

        customerSummaryResponse.setInfoExplanation(infoExplanation);

        CustomerProcessSummaryResponse.ProcessApprovalAnalysis approvalAnalysis = new CustomerProcessSummaryResponse.ProcessApprovalAnalysis();
        approvalAnalysis.setMethod(convertProcessContent(customerSummary.getApprovalAnalysisMethod()));
        approvalAnalysis.setIssue(convertProcessContent(customerSummary.getApprovalAnalysisIssue()));
        approvalAnalysis.setValue(convertProcessContent(customerSummary.getApprovalAnalysisValue()));
        approvalAnalysis.setPurchase(convertProcessContent(customerSummary.getApprovalAnalysisPurchase()));
        approvalAnalysis.setPrice(convertProcessContent(customerSummary.getApprovalAnalysisPrice()));
        approvalAnalysis.setSoftwareOperation(convertProcessContent(customerSummary.getApprovalAnalysisSoftwareOperation()));
        approvalAnalysis.setCourse(convertProcessContent(customerSummary.getApprovalAnalysisCourse()));
        approvalAnalysis.setNoMoney(convertProcessContent(customerSummary.getApprovalAnalysisNoMoney()));
        approvalAnalysis.setOthers(convertProcessContent(customerSummary.getApprovalAnalysisOthers()));

        customerSummaryResponse.setApprovalAnalysis(approvalAnalysis);

        return customerSummaryResponse;
    }


    private CustomerFeatureResponse.Feature convertFeatureByOverwrite(List<FeatureContent> featureContentByModel, FeatureContentSales featureContentBySales, Class<? extends Enum<?>> enumClass, Class type) {
        CustomerFeatureResponse.Feature featureVO = new CustomerFeatureResponse.Feature();
        // 多通电话覆盖+规则加工
        String resultAnswer = null;
        String resultAnswerLatest = null;
        String original = null;
        String callId = null;
        // 获取
        if (!CollectionUtils.isEmpty(featureContentByModel)) {
            for (int i = featureContentByModel.size() - 1; i >= 0; i--) {
                if (!StringUtils.isEmpty(featureContentByModel.get(i).getAnswer()) &&
                        !featureContentByModel.get(i).getAnswer().equals("无") &&
                        !featureContentByModel.get(i).getAnswer().equals("null")) {
                    resultAnswerLatest = featureContentByModel.get(i).getAnswer();
                    original = Objects.nonNull(featureContentByModel.get(i).getOriginal()) ? featureContentByModel.get(i).getOriginal() : featureContentByModel.get(i).getAnswer();
                    callId = featureContentByModel.get(i).getCallId();
                    break;
                }
            }
        }
        // 如果最后一个非空值为null，结果就是null
        if (!StringUtils.isEmpty(resultAnswerLatest)) {
            // 没有候选值枚举，直接返回最后一个非空（如果存在）记录值
            if (Objects.isNull(enumClass)) {
                resultAnswer = resultAnswerLatest;
                CustomerFeatureResponse.OriginChat originChat = new CustomerFeatureResponse.OriginChat();
                originChat.setContent(original);
                originChat.setId(callId);
                featureVO.setOriginChat(originChat);
            } else {
                // 有候选值枚举，需要比较最后一个非空记录值是否跟候选值相同，不同则返回为空
                for (Enum<?> enumConstant : enumClass.getEnumConstants()) {
                    // 获取枚举对象的 `value` 和 `text` 字段值
                    String value = getFieldValue(enumConstant, "value");
                    String enumText = getFieldValue(enumConstant, "text");
                    // 判断文本是否匹配`text`
                    if (resultAnswerLatest.trim().equals(enumText)) {
                        resultAnswer = value;
                        CustomerFeatureResponse.OriginChat originChat = new CustomerFeatureResponse.OriginChat();
                        originChat.setContent(original);
                        originChat.setId(callId);
                        featureVO.setOriginChat(originChat);
                        break;
                    }
                }
            }
            // 返回值类型是boolen
            if (type == Boolean.class) {
                resultAnswer = deletePunctuation(resultAnswer);
                if ("是".equals(resultAnswer)) {
                    featureVO.setModelRecord(Boolean.TRUE);
                } else {
                    if ("否".equals(resultAnswer)) {
                        featureVO.setModelRecord(Boolean.FALSE);
                    }
                }
            } else {
                featureVO.setModelRecord(resultAnswer);
            }
        }
        featureVO.setSalesRecord(Objects.isNull(featureContentBySales) ? null : featureContentBySales.getContent());
        featureVO.setSalesManualTag(Objects.isNull(featureContentBySales) ? null : featureContentBySales.getTag());

        //“已询问”有三个值：“是”、“否”、“不需要”。
        if (!CollectionUtils.isEmpty(featureContentByModel)) {
            //如果 funds_volume_model json list 中有一个 question 有值，就是 ‘是’;
            for (int i = featureContentByModel.size() - 1; i >= 0; i--) {
                if (!StringUtils.isEmpty(featureContentByModel.get(i).getQuestion()) &&
                        !featureContentByModel.get(i).getQuestion().equals("无") &&
                        !featureContentByModel.get(i).getQuestion().equals("null")) {
                    featureVO.setInquired("yes");
                    CustomerFeatureResponse.OriginChat originChat = new CustomerFeatureResponse.OriginChat();
                    originChat.setContent(featureContentByModel.get(i).getQuestion());
                    originChat.setId(featureContentByModel.get(i).getCallId());
                    featureVO.setInquiredOriginChat(originChat);
                    break;
                }
            }
            //如果都没有 question 或者 question 都没值，但是有 answer 有值，就是‘不需要’；
            if (featureVO.getInquired().equals("no")) {
                for (int i = featureContentByModel.size() - 1; i >= 0; i--) {
                    if (!StringUtils.isEmpty(featureContentByModel.get(i).getAnswer()) &&
                            !featureContentByModel.get(i).getAnswer().equals("无") &&
                            !featureContentByModel.get(i).getAnswer().equals("null")) {
                        featureVO.setInquired("no-need");
                        CustomerFeatureResponse.OriginChat originChat = new CustomerFeatureResponse.OriginChat();
                        originChat.setContent(featureContentByModel.get(i).getQuestion());
                        originChat.setId(featureContentByModel.get(i).getCallId());
                        featureVO.setInquiredOriginChat(originChat);
                        break;
                    }
                }
            }
        }
        featureVO.setCompareValue(Objects.nonNull(featureVO.getSalesManualTag()) ? featureVO.getSalesManualTag() :
                featureVO.getModelRecord());
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

    private CustomerFeatureResponse.FeatureSpecial converFeaturetByAppend(List<FeatureContent> featureContentByModel, FeatureContentSales featureContentBySales) {
        CustomerFeatureResponse.FeatureSpecial featureVO = new CustomerFeatureResponse.FeatureSpecial();
        List<CustomerFeatureResponse.OriginChat> originChats = new ArrayList<>();
        // 多通电话追加+规则加工，跳过null值
        List<String> modelRecord = new ArrayList<>();
        if (!CollectionUtils.isEmpty(featureContentByModel)) {
            ListIterator<FeatureContent> iterator = featureContentByModel.listIterator(featureContentByModel.size());
            while (iterator.hasPrevious()) {
                FeatureContent item = iterator.previous();
                if (!StringUtils.isEmpty(item.getAnswer())) {
                    CustomerFeatureResponse.OriginChat originChat = new CustomerFeatureResponse.OriginChat();
                    originChat.setContent(item.getAnswer());
                    originChat.setId(item.getCallId());
                    originChats.add(originChat);
                    modelRecord.add(item.getAnswer());
                }
            }
        }
        featureVO.setOriginChats(originChats);
        featureVO.setModelRecord(CollectionUtils.isEmpty(modelRecord) ? null : JsonUtil.serialize(modelRecord));
        featureVO.setSalesRecord(Objects.isNull(featureContentBySales) ? null : featureContentBySales.getContent());
        featureVO.setSalesManualTag(Objects.isNull(featureContentBySales) ? null : featureContentBySales.getTag());
        //“已询问”有三个值：“是”、“否”、“不需要”。
        if (!CollectionUtils.isEmpty(featureContentByModel)) {
            //如果 funds_volume_model json list 中有一个 question 有值，就是 ‘是’;
            for (int i = featureContentByModel.size() - 1; i >= 0; i--) {
                if (!StringUtils.isEmpty(featureContentByModel.get(i).getQuestion()) &&
                        !featureContentByModel.get(i).getQuestion().equals("无") &&
                        !featureContentByModel.get(i).getQuestion().equals("null")) {
                    featureVO.setInquired("yes");
                    CustomerFeatureResponse.OriginChat originChat = new CustomerFeatureResponse.OriginChat();
                    originChat.setContent(featureContentByModel.get(i).getQuestion());
                    originChat.setId(featureContentByModel.get(i).getCallId());
                    featureVO.setInquiredOriginChat(originChat);
                    break;
                }
            }
            //如果都没有 question 或者 question 都没值，但是有 answer 有值，就是‘不需要’；
            if (featureVO.getInquired().equals("no")) {
                for (int i = featureContentByModel.size() - 1; i >= 0; i--) {
                    if (!StringUtils.isEmpty(featureContentByModel.get(i).getAnswer()) &&
                            !featureContentByModel.get(i).getAnswer().equals("无") &&
                            !featureContentByModel.get(i).getAnswer().equals("null")) {
                        featureVO.setInquired("no-need");
                        CustomerFeatureResponse.OriginChat originChat = new CustomerFeatureResponse.OriginChat();
                        originChat.setContent(featureContentByModel.get(i).getQuestion());
                        originChat.setId(featureContentByModel.get(i).getCallId());
                        featureVO.setInquiredOriginChat(originChat);
                        break;
                    }
                }
            }
        }
        //否则就是 ‘否’
        featureVO.setCompareValue(Objects.nonNull(featureVO.getSalesManualTag()) ? featureVO.getSalesManualTag() :
                featureVO.getModelRecord());
        return featureVO;
    }

    private CustomerProcessSummaryResponse.ProcessInfoExplanationContent convertSummaryByOverwrite(List<SummaryContent> summaryContentList) {

        CustomerProcessSummaryResponse.ProcessInfoExplanationContent explanationContent =
                new CustomerProcessSummaryResponse.ProcessInfoExplanationContent();

        if (CollectionUtils.isEmpty(summaryContentList)) {
            explanationContent.setResult(Boolean.FALSE);
            return explanationContent;
        }
        // 多通电话覆盖+规则加工
        for (int i = summaryContentList.size() - 1; i >= 0; i--) {
            SummaryContent item = summaryContentList.get(i);
            if (!StringUtils.isEmpty(item.getContent().trim())) {
                explanationContent.setResult(Boolean.TRUE);
                CustomerProcessSummaryResponse.OriginChat originChat =
                        new CustomerProcessSummaryResponse.OriginChat();
                originChat.setId(item.getCallId());
                originChat.setContent(item.getContent());
                explanationContent.setOriginChat(originChat);
                return explanationContent;
            }
        }
        return explanationContent;
    }

    private CustomerProcessSummaryResponse.ProcessContent convertProcessContent(List<SummaryContent> summaryContentList) {
        summaryContentList = dataPreprocess(summaryContentList);
        CustomerProcessSummaryResponse.ProcessContent processContent = new CustomerProcessSummaryResponse.ProcessContent();
        String recognitionOverall = null;
        List<CustomerProcessSummaryResponse.Chat> chatList = new ArrayList<>();
        if (!CollectionUtils.isEmpty(summaryContentList)) {
            for (SummaryContent item : summaryContentList) {
                try {
                    SummaryContentChats contentChats = null;
                    contentChats = JsonUtil.readValue(item.getContent().replace("\n", ""), new TypeReference<SummaryContentChats>() {
                    });
                    String dateString = contentChats.getTime();
                    for (SummaryContentChats.Chat chatFromDb : contentChats.getChats()) {
                        List<SummaryContentChats.Message> dbMessages = chatFromDb.getMessages();
                        String chatRecognition = null;
                        if (!StringUtils.isEmpty(chatFromDb.getRecognition())) {
                            if ("是".equals(chatFromDb.getRecognition())) {
                                chatRecognition = CustomerRecognition.APPROVED.getText();
                            } else if ("否".equals(chatFromDb.getRecognition())) {
                                chatRecognition = CustomerRecognition.NOT_APPROVED.getText();
                            } else {
                                chatRecognition = CustomerRecognition.UNKNOWN.getText();
                            }
                        }
                        if (!StringUtils.isEmpty(chatRecognition)) {
                            recognitionOverall = chatRecognition;
                        }
                        CustomerProcessSummaryResponse.Chat chat = new CustomerProcessSummaryResponse.Chat();
                        chat.setRecognition(chatRecognition);
                        List<CustomerProcessSummaryResponse.Message> messageList = new ArrayList<>();
                        for (SummaryContentChats.Message dbmessage : dbMessages) {
                            CustomerProcessSummaryResponse.Message message = new CustomerProcessSummaryResponse.Message();
                            message.setRole(dbmessage.getRole());
                            message.setContent(dbmessage.getContent());
                            try {
                                message.setTime(dateFormat1.parse(dateString));
                            } catch (Exception e) {
                                try {
                                    message.setTime(dateFormat2.parse(dateString));
                                } catch (Exception ex) {
                                    message.setTime(dateFormat3.parse(dateString));
                                }
                            }
                            messageList.add(message);
                        }
                        chat.setMessages(messageList);
                        chat.setSourceId(item.getCallId());
                        try {
                            chat.setTime(dateFormat1.parse(dateString));
                        } catch (Exception e) {
                            try {
                                chat.setTime(dateFormat2.parse(dateString));
                            } catch (Exception ex) {
                                chat.setTime(dateFormat3.parse(dateString));
                            }
                        }
                        chatList.add(chat);
                    }
                } catch (Exception e) {
                    log.error("格式转化失败：{}", JsonUtil.serialize(item));
                }
            }
        }
        processContent.setChats(chatList);
        processContent.setRecognition(recognitionOverall);
        return processContent;
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

    private CustomerProcessSummaryResponse.ProcessSummary getProcessSummary(CustomerFeature customerFeature, CustomerInfo customerInfo, CustomerStageStatus stageStatus, CustomerProcessSummaryResponse summaryResponse) {
        CustomerProcessSummaryResponse.ProcessSummary processSummary = new CustomerProcessSummaryResponse.ProcessSummary();

        CustomerFeatureResponse customerFeatureResponse = convert2CustomerFeatureResponse(customerFeature);
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
                        StringBuilder ttt = new StringBuilder("未完成客户匹配度判断（");
                        List<FeatureContent> fundsVolumeModel = customerFeature.getFundsVolumeModel();
                        List<FeatureContent> earningDesireModel = customerFeature.getEarningDesireModel();

                        if (CollectionUtils.isEmpty(fundsVolumeModel)) {
                            ttt.append("资金体量，");
                        } else {
                            String fundsVolume = null;
                            // 找到最后一个非null的值
                            for (int i = fundsVolumeModel.size() - 1; i >= 0; i--) {
                                if (!StringUtils.isEmpty(fundsVolumeModel.get(i).getAnswer())) {
                                    fundsVolume = fundsVolumeModel.get(i).getAnswer();
                                    if (!fundsVolume.equals("无") && !fundsVolume.equals("null")) {
                                        break;
                                    }
                                }
                            }
                            if (StringUtils.isEmpty(fundsVolume)) {
                                ttt.append("资金体量，");
                            }
                        }
                        if (CollectionUtils.isEmpty(earningDesireModel)) {
                            ttt.append("赚钱欲望，");
                        } else {
                            String earningDesire = null;
                            for (int i = earningDesireModel.size() - 1; i >= 0; i--) {
                                if (!StringUtils.isEmpty(earningDesireModel.get(i).getAnswer())) {
                                    earningDesire = earningDesireModel.get(i).getAnswer();
                                    if (!earningDesire.equals("无") && !earningDesire.equals("null")) {
                                        break;
                                    }
                                }
                            }
                            if (StringUtils.isEmpty(earningDesire)) {
                                ttt.append("赚钱欲望，");
                            }
                        }
                        ttt.deleteCharAt(ttt.length() - 1);
                        ttt.append("）");
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

                        StringBuilder ttt = new StringBuilder("未完成客户交易风格了解（");
                        // 客户交易风格了解 相关字段全部有值——“客户当前持仓或关注的股票”、“客户为什么买这些股票”、“客户怎么决定的买卖这些股票的时机”、“客户的交易风格”、“客户的股龄”
                        CustomerFeatureResponse.TradingMethod tradingMethod = customerFeatureResponse.getTradingMethod();
                        if (Objects.isNull(tradingMethod.getCurrentStocks().getModelRecord())) {
                            ttt.append("客户当前持仓或关注的股票，");
                        }
                        if (Objects.isNull(tradingMethod.getStockPurchaseReason().getModelRecord())) {
                            ttt.append("客户为什么买这些股票，");
                        }
                        if (Objects.isNull(tradingMethod.getStockPurchaseReason().getModelRecord())) {
                            ttt.append("客户怎么决定的买卖这些股票的时机，");
                        }
                        if (Objects.isNull(tradingMethod.getStockPurchaseReason().getModelRecord())) {
                            ttt.append("客户的交易风格，");
                        }
                        if (Objects.isNull(tradingMethod.getStockPurchaseReason().getModelRecord())) {
                            ttt.append("客户的股龄，");
                        }
                        ttt.deleteCharAt(ttt.length() - 1);
                        ttt.append("）");
                        if (ttt.length() > 15) {
                            questions.add("尚未完成客户交易风格了解，需继续收集客户信息");
                        }
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
            if (stageStatus.getMatchingJudgment() == 1 && stageStatus.getTransactionStyle() == 1 && stageStatus.getFunctionIntroduction() == 1) {
                if (((stageStatus.getConfirmValue() == 0 && stageStatus.getConfirmPurchase() == 0 && stageStatus.getCompletePurchase() == 0)) ||
                        ((stageStatus.getConfirmValue() == 1 && stageStatus.getConfirmPurchase() == 0 && stageStatus.getCompletePurchase() == 0)) ||
                        ((stageStatus.getConfirmValue() == 1 && stageStatus.getConfirmPurchase() == 1))){
                    advantage.add("SOP执行顺序正确");
                }
            }
            Set<String> questionStatus = new TreeSet<>();
            if (stageStatus.getMatchingJudgment() == 0 &&
                    (stageStatus.getTransactionStyle() + stageStatus.getFunctionIntroduction() + stageStatus.getConfirmValue() + stageStatus.getConfirmPurchase() + stageStatus.getCompletePurchase()) > 0){
                questionStatus.add("客户判断");
            }
            if (stageStatus.getTransactionStyle() == 0 &&
                    (stageStatus.getFunctionIntroduction() + stageStatus.getConfirmValue() + stageStatus.getConfirmPurchase() + stageStatus.getCompletePurchase()) > 0){
                questionStatus.add("交易风格了解");
            }
            if (stageStatus.getFunctionIntroduction() == 0 &&
                    (stageStatus.getConfirmValue() + stageStatus.getConfirmPurchase() + stageStatus.getCompletePurchase()) > 0){
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
            CustomerProcessSummaryResponse.ProcessInfoExplanation infoExplanation = summaryResponse.getInfoExplanation();
            if (Objects.nonNull(infoExplanation.getCustomerIssuesQuantified()) && infoExplanation.getCustomerIssuesQuantified().getResult() &&
                    Objects.nonNull(infoExplanation.getSoftwareValueQuantified()) && infoExplanation.getSoftwareValueQuantified().getResult()) {
                advantage.add("完成痛点和价值量化放大");
            } else if (Objects.nonNull(customerInfo.getCommunicationRounds()) && customerInfo.getCommunicationRounds() >= 3){
                questions.add("尚未完成痛点和价值量化放大，需后续完成");
            }

            CustomerFeatureResponse.Recognition recognition = customerFeatureResponse.getRecognition();
            // 功能讲解
            // 优点：-功能讲解让客户理解：“客户对软件功能的清晰度”的值为“是”
            // 缺点：-功能讲解未让客户理解：“客户对软件功能的清晰度”的值为“否”
            if (Objects.nonNull(recognition.getSoftwareFunctionClarity().getCompareValue()) &&
                    (Boolean) recognition.getSoftwareFunctionClarity().getCompareValue()) {
                advantage.add("客户对软件功能理解清晰");
            } else if (Objects.nonNull(recognition.getSoftwareFunctionClarity().getCompareValue()) &&
                    !(Boolean) recognition.getSoftwareFunctionClarity().getCompareValue()) {
                questions.add("客户对软件功能尚未理解清晰，需根据客户学习能力更白话讲解");
            }

            // 选股方法
            // 优点：-客户认可选股方法：“客户对业务员讲的选股方法的认可度”的值为“是”
            // 缺点：-客户对选股方法尚未认可，需加强选股成功的真实案例证明：“客户对业务员讲的选股方法的认可度”的值为“否”
            if (Objects.nonNull(recognition.getStockSelectionMethod().getCompareValue()) &&
                    (Boolean) recognition.getStockSelectionMethod().getCompareValue()) {
                advantage.add("客户认可选股方法");
            } else if (Objects.nonNull(recognition.getStockSelectionMethod().getCompareValue()) &&
                    !(Boolean) recognition.getStockSelectionMethod().getCompareValue()) {
                questions.add("客户对选股方法尚未认可，需加强选股成功的真实案例证明");
            }

            // 自身问题
            // 优点：-客户认可自身问题：“客户对自身问题及影响的认可度”的值为“是”
            // 缺点：-客户对自身问题尚未认可，需列举与客户相近的真实反面案例证明：“客户对自身问题及影响的认可度”的值为“否”
            if (Objects.nonNull(recognition.getSelfIssueRecognition().getCompareValue()) &&
                    (Boolean) recognition.getSelfIssueRecognition().getCompareValue()) {
                advantage.add("客户认可自身问题");
            } else if (Objects.nonNull(recognition.getSelfIssueRecognition().getCompareValue()) &&
                    !(Boolean) recognition.getSelfIssueRecognition().getCompareValue()) {
                questions.add("客户对自身问题尚未认可，需列举与客户相近的真实反面案例证明");
            }

            // 价值认可
            // 优点：-客户认可软件价值：字段（不是阶段）“客户对软件价值的认可度”的值为“是”
            // 缺点：-客户对软件价值尚未认可，需加强使用软件的真实成功案例证明：字段（不是阶段）“客户对软件价值的认可度”的值为“否”
            if (Objects.nonNull(recognition.getSoftwareValueApproval().getCompareValue()) &&
                    (Boolean) recognition.getSoftwareValueApproval().getCompareValue()) {
                advantage.add("客户认可软件价值");
            } else if (Objects.nonNull(recognition.getSoftwareValueApproval().getCompareValue()) &&
                    !(Boolean) recognition.getSoftwareValueApproval().getCompareValue()) {
                questions.add("客户对软件价值尚未认可，需加强使用软件的真实成功案例证明");
            }

            // 缺点：- 质疑应对失败次数多，需参考调整应对话术：单个类别的质疑不认可的对话组数大于等于5
            int questionCount = 0;
            CustomerProcessSummaryResponse.ProcessApprovalAnalysis approvalAnalysis = summaryResponse.getApprovalAnalysis();
            if (Objects.nonNull(approvalAnalysis)) {
                if (Objects.nonNull(approvalAnalysis.getMethod()) && !CollectionUtils.isEmpty(approvalAnalysis.getMethod().getChats())){
                    for (CustomerProcessSummaryResponse.Chat item : approvalAnalysis.getMethod().getChats()) {
                        if (item.getRecognition().equals(CustomerRecognition.NOT_APPROVED.getText())) {
                            questionCount++;
                        }
                    }
                }
                if (Objects.nonNull(approvalAnalysis.getIssue()) && !CollectionUtils.isEmpty(approvalAnalysis.getIssue().getChats())){
                    for (CustomerProcessSummaryResponse.Chat item : approvalAnalysis.getIssue().getChats()) {
                        if (item.getRecognition().equals(CustomerRecognition.NOT_APPROVED.getText())) {
                            questionCount++;
                        }
                    }
                }
                if (Objects.nonNull(approvalAnalysis.getValue()) && !CollectionUtils.isEmpty(approvalAnalysis.getValue().getChats())){
                    for (CustomerProcessSummaryResponse.Chat item : approvalAnalysis.getValue().getChats()) {
                        if (item.getRecognition().equals(CustomerRecognition.NOT_APPROVED.getText())) {
                            questionCount++;
                        }
                    }
                }
                if (Objects.nonNull(approvalAnalysis.getPrice()) && !CollectionUtils.isEmpty(approvalAnalysis.getPrice().getChats())){
                    for (CustomerProcessSummaryResponse.Chat item : approvalAnalysis.getPrice().getChats()) {
                        if (item.getRecognition().equals(CustomerRecognition.NOT_APPROVED.getText())) {
                            questionCount++;
                        }
                    }
                }
                if (Objects.nonNull(approvalAnalysis.getPurchase()) && !CollectionUtils.isEmpty(approvalAnalysis.getPurchase().getChats())){
                    for (CustomerProcessSummaryResponse.Chat item : approvalAnalysis.getPurchase().getChats()) {
                        if (item.getRecognition().equals(CustomerRecognition.NOT_APPROVED.getText())) {
                            questionCount++;
                        }
                    }
                }
                if (Objects.nonNull(approvalAnalysis.getSoftwareOperation()) && !CollectionUtils.isEmpty(approvalAnalysis.getSoftwareOperation().getChats())){
                    for (CustomerProcessSummaryResponse.Chat item : approvalAnalysis.getSoftwareOperation().getChats()) {
                        if (item.getRecognition().equals(CustomerRecognition.NOT_APPROVED.getText())) {
                            questionCount++;
                        }
                    }
                }
                if (Objects.nonNull(approvalAnalysis.getCourse()) && !CollectionUtils.isEmpty(approvalAnalysis.getCourse().getChats())){
                    for (CustomerProcessSummaryResponse.Chat item : approvalAnalysis.getCourse().getChats()) {
                        if (item.getRecognition().equals(CustomerRecognition.NOT_APPROVED.getText())) {
                            questionCount++;
                        }
                    }
                }
                if (Objects.nonNull(approvalAnalysis.getNoMoney()) && !CollectionUtils.isEmpty(approvalAnalysis.getNoMoney().getChats())){
                    for (CustomerProcessSummaryResponse.Chat item : approvalAnalysis.getNoMoney().getChats()) {
                        if (item.getRecognition().equals(CustomerRecognition.NOT_APPROVED.getText())) {
                            questionCount++;
                        }
                    }
                }
                if (Objects.nonNull(approvalAnalysis.getOthers()) && !CollectionUtils.isEmpty(approvalAnalysis.getOthers().getChats())){
                    for (CustomerProcessSummaryResponse.Chat item : approvalAnalysis.getOthers().getChats()) {
                        if (item.getRecognition().equals(CustomerRecognition.NOT_APPROVED.getText())) {
                            questionCount++;
                        }
                    }
                }
            }
            if (questionCount >= 5) {
                questions.add("质疑应对失败次数多，需参考调整应对话术");
            }

            // 优点：- 客户确认购买：字段“客户对购买软件的态度”的值为“是”
            // 缺点：- 客户拒绝购买，需暂停劝说客户购买，明确拒绝原因进行化解：字段“客户对软件价值的认可度”的值为“否”
            // 优点：- 客户完成购买：阶段“客户完成购买”的值为“是”
            if (Objects.nonNull(recognition.getSoftwarePurchaseAttitude().getCompareValue()) &&
                    (Boolean) recognition.getSoftwarePurchaseAttitude().getCompareValue()) {
                advantage.add("客户确认购买");
            } else if (Objects.nonNull(recognition.getSoftwareValueApproval().getCompareValue()) &&
                    !(Boolean) recognition.getSoftwareValueApproval().getCompareValue()) {
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
