package com.smart.sso.server.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.smart.sso.server.enums.ConfigTypeEnum;
import com.smart.sso.server.enums.CustomerRecognition;
import com.smart.sso.server.enums.EarningDesireEnum;
import com.smart.sso.server.enums.FundsVolumeEnum;
import com.smart.sso.server.enums.ProfitLossEnum;
import com.smart.sso.server.mapper.ConfigMapper;
import com.smart.sso.server.mapper.CustomerFeatureMapper;
import com.smart.sso.server.mapper.CustomerInfoMapper;
import com.smart.sso.server.mapper.CustomerSummaryMapper;
import com.smart.sso.server.model.*;
import com.smart.sso.server.model.VO.CustomerListVO;
import com.smart.sso.server.model.VO.CustomerProfile;
import com.smart.sso.server.model.dto.CallBackRequest;
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
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

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
    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");


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
        customerProfile.setCustomerStage(getCustomerStageStatus(customerFeature, customerSummary));
        if (Objects.isNull(customerProfile.getCommunicationRounds())) {
            customerProfile.setCommunicationRounds(0);
        }
        // 重新判断一下匹配度，防止更新不及时的情况
        String conversionRate = getConversionRate(customerFeature);
        if (!customerInfo.getConversionRate().equals(conversionRate)) {
            customerInfoMapper.updateConversionRateById(id, conversionRate);
            customerProfile.setConversionRate(conversionRate);
        }
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
        CustomerStageStatus stageStatus = getCustomerStageStatus(customerFeature, customerSummary);
        summaryResponse.setSummary(getProcessSummary(customerFeature, customerInfo, stageStatus));
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
                if (!fundsVolume.equals("无") && !fundsVolume.equals("null")) {
                    break;
                }
            }
        }
        for (int i = earningDesireModel.size() - 1; i >= 0; i--) {
            if (!StringUtils.isEmpty(earningDesireModel.get(i).getAnswer())) {
                earningDesire = earningDesireModel.get(i).getAnswer();
                if (!earningDesire.equals("无") && !earningDesire.equals("null")) {
                    break;
                }
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
        if (Objects.nonNull(tradingMethod.getCurrentStocks().getModelRecord()) &&
                Objects.nonNull(tradingMethod.getStockPurchaseReason().getModelRecord()) &&
                Objects.nonNull(tradingMethod.getTradeTimingDecision().getModelRecord()) &&
                Objects.nonNull(tradingMethod.getTradingStyle().getModelRecord()) &&
                Objects.nonNull(tradingMethod.getStockMarketAge().getModelRecord())) {
            stageStatus.setTransactionStyle(1);
        }
        // 针对性功能介绍 相关字段的值全部为“是”——“销售有结合客户的股票举例”、“销售有基于客户交易风格做针对性的功能介绍”、“销售有点评客户的选股方法”、“销售有点评客户的选股时机”
        CustomerProcessSummaryResponse.ProcessInfoExplanation infoExplanation = summaryResponse.getInfoExplanation();
        if (Objects.nonNull(infoExplanation.getStock()) &&
                (Boolean) infoExplanation.getStock() &&
                Objects.nonNull(infoExplanation.getStockPickReview()) &&
                (Boolean) infoExplanation.getStockPickReview() &&
                Objects.nonNull(infoExplanation.getStockTimingReview()) &&
                (Boolean) infoExplanation.getStockTimingReview() &&
                Objects.nonNull(infoExplanation.getTradeBasedIntro()) &&
                (Boolean) infoExplanation.getTradeBasedIntro()) {
            stageStatus.setFunctionIntroduction(1);
        }
        // 客户确认价值 相关字段的值全部为“是”——“客户对软件功能的清晰度”、“客户对销售讲的选股方法的认可度”、“客户对自身问题及影响的认可度”、“客户对软件价值的认可度”
        CustomerFeatureResponse.Recognition recognition = customerFeatureResponse.getRecognition();
        if (Objects.nonNull(recognition.getSoftwareFunctionClarity().getModelRecord()) &&
                (Boolean) recognition.getSoftwareFunctionClarity().getModelRecord() &&
                Objects.nonNull(recognition.getStockSelectionMethod().getModelRecord()) &&
                (Boolean) recognition.getStockSelectionMethod().getModelRecord() &&
                Objects.nonNull(recognition.getSelfIssueRecognition().getModelRecord()) &&
                (Boolean) recognition.getSelfIssueRecognition().getModelRecord() &&
                Objects.nonNull(recognition.getSoftwareValueApproval().getModelRecord()) &&
                (Boolean) recognition.getSoftwareValueApproval().getModelRecord()) {
            stageStatus.setConfirmValue(1);
        }
        // 客户确认购买 客户对购买软件的态度”的值为“是”
        if (Objects.nonNull(recognition.getSoftwarePurchaseAttitude().getModelRecord()) &&
                (Boolean) recognition.getSoftwarePurchaseAttitude().getModelRecord()) {
            stageStatus.setConfirmPurchase(1);
        }
        // 客户完成购买”，规则是看客户提供的字段“成交状态”来直接判定，这个数值从数据库中提取
        // TODO
        if (Objects.nonNull(summaryResponse.getApprovalAnalysis().getPurchase()) &&
                !StringUtils.isEmpty(summaryResponse.getApprovalAnalysis().getPurchase().getRecognition()) &&
                "approved".equals(summaryResponse.getApprovalAnalysis().getPurchase().getRecognition())) {
            stageStatus.setConfirmPurchase(1);
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
                    Objects.nonNull(customerFeatureRequest.getBasic().getFundsVolume().getSalesRecord())) {
                customerFeature.setFundsVolumeSales(new FeatureContentSales(customerFeatureRequest.getBasic().getFundsVolume().getSalesRecord()));
            }
            if (Objects.nonNull(customerFeatureRequest.getBasic().getProfitLossSituation()) &&
                    Objects.nonNull(customerFeatureRequest.getBasic().getProfitLossSituation().getSalesRecord())) {
                customerFeature.setProfitLossSituationSales(new FeatureContentSales(customerFeatureRequest.getBasic().getProfitLossSituation().getSalesRecord()));
            }
            if (Objects.nonNull(customerFeatureRequest.getBasic().getEarningDesire()) &&
                    Objects.nonNull(customerFeatureRequest.getBasic().getEarningDesire().getSalesRecord())) {
                customerFeature.setEarningDesireSales(new FeatureContentSales(customerFeatureRequest.getBasic().getEarningDesire().getSalesRecord()));
            }
        }

        if (Objects.nonNull(customerFeatureRequest.getTradingMethod())) {
            if (Objects.nonNull(customerFeatureRequest.getTradingMethod().getCurrentStocks()) &&
                    Objects.nonNull(customerFeatureRequest.getTradingMethod().getCurrentStocks().getSalesRecord())) {
                customerFeature.setFundsVolumeSales(new FeatureContentSales(customerFeatureRequest.getTradingMethod().getCurrentStocks().getSalesRecord()));
            }
            if (Objects.nonNull(customerFeatureRequest.getTradingMethod().getStockPurchaseReason()) &&
                    Objects.nonNull(customerFeatureRequest.getTradingMethod().getStockPurchaseReason().getSalesRecord())) {
                customerFeature.setProfitLossSituationSales(new FeatureContentSales(customerFeatureRequest.getTradingMethod().getStockPurchaseReason().getSalesRecord()));
            }
            if (Objects.nonNull(customerFeatureRequest.getTradingMethod().getTradeTimingDecision()) &&
                    Objects.nonNull(customerFeatureRequest.getTradingMethod().getTradeTimingDecision().getSalesRecord())) {
                customerFeature.setEarningDesireSales(new FeatureContentSales(customerFeatureRequest.getTradingMethod().getTradeTimingDecision().getSalesRecord()));
            }
            if (Objects.nonNull(customerFeatureRequest.getTradingMethod().getLearningAbility()) &&
                    Objects.nonNull(customerFeatureRequest.getTradingMethod().getLearningAbility().getSalesRecord())) {
                customerFeature.setLearningAbilitySales(new FeatureContentSales(customerFeatureRequest.getTradingMethod().getLearningAbility().getSalesRecord()));
            }
            if (Objects.nonNull(customerFeatureRequest.getTradingMethod().getStockMarketAge()) &&
                    Objects.nonNull(customerFeatureRequest.getTradingMethod().getStockMarketAge().getSalesRecord())) {
                customerFeature.setStockMarketAgeSales(new FeatureContentSales(customerFeatureRequest.getTradingMethod().getStockMarketAge().getSalesRecord()));
            }
            if (Objects.nonNull(customerFeatureRequest.getTradingMethod().getTradingStyle()) &&
                    Objects.nonNull(customerFeatureRequest.getTradingMethod().getTradingStyle().getSalesRecord())) {
                customerFeature.setTradingStyleSales(new FeatureContentSales(customerFeatureRequest.getTradingMethod().getTradingStyle().getSalesRecord()));
            }
        }

        if (Objects.nonNull(customerFeatureRequest.getRecognition())) {
            if (Objects.nonNull(customerFeatureRequest.getRecognition().getCourseTeacherApproval()) &&
                    Objects.nonNull(customerFeatureRequest.getRecognition().getCourseTeacherApproval().getSalesRecord())) {
                customerFeature.setCourseTeacherApprovalSales(new FeatureContentSales(customerFeatureRequest.getRecognition().getCourseTeacherApproval().getSalesRecord()));
            }
            if (Objects.nonNull(customerFeatureRequest.getRecognition().getSelfIssueRecognition()) &&
                    Objects.nonNull(customerFeatureRequest.getRecognition().getSelfIssueRecognition().getSalesRecord())) {
                customerFeature.setCourseTeacherApprovalSales(new FeatureContentSales(customerFeatureRequest.getRecognition().getSelfIssueRecognition().getSalesRecord()));
            }
            if (Objects.nonNull(customerFeatureRequest.getRecognition().getSoftwareFunctionClarity()) &&
                    Objects.nonNull(customerFeatureRequest.getRecognition().getSoftwareFunctionClarity().getSalesRecord())) {
                customerFeature.setCourseTeacherApprovalSales(new FeatureContentSales(customerFeatureRequest.getRecognition().getSoftwareFunctionClarity().getSalesRecord()));
            }
            if (Objects.nonNull(customerFeatureRequest.getRecognition().getSoftwarePurchaseAttitude()) &&
                    Objects.nonNull(customerFeatureRequest.getRecognition().getSoftwarePurchaseAttitude().getSalesRecord())) {
                customerFeature.setCourseTeacherApprovalSales(new FeatureContentSales(customerFeatureRequest.getRecognition().getSoftwarePurchaseAttitude().getSalesRecord()));
            }
            if (Objects.nonNull(customerFeatureRequest.getRecognition().getSoftwareValueApproval()) &&
                    Objects.nonNull(customerFeatureRequest.getRecognition().getSoftwareValueApproval().getSalesRecord())) {
                customerFeature.setCourseTeacherApprovalSales(new FeatureContentSales(customerFeatureRequest.getRecognition().getSoftwareValueApproval().getSalesRecord()));
            }
            if (Objects.nonNull(customerFeatureRequest.getRecognition().getStockSelectionMethod()) &&
                    Objects.nonNull(customerFeatureRequest.getRecognition().getStockSelectionMethod().getSalesRecord())) {
                customerFeature.setCourseTeacherApprovalSales(new FeatureContentSales(customerFeatureRequest.getRecognition().getStockSelectionMethod().getSalesRecord()));
            }
        }
        if (Objects.nonNull(customerFeatureRequest.getNote())) {
            customerFeature.setNote(customerFeatureRequest.getNote());
        }
        customerFeatureMapper.updateById(customerFeature);
    }

    @Override
    public void callback(CallBackRequest callBackRequest) {
        String sourceId = callBackRequest.getSourceId();
        try {
            // 将sourceId 写入文件
            String filePath = "/opt/customer-convert/callback/sourceid.txt";
            CommonUtils.appendTextToFile(filePath, sourceId);
//            ShellUtils.bashRun("", new HashMap<>());
            ShellUtils.saPythonRun("/home/opsuser/hsw/chat_insight-main/process_text.py", 2, sourceId);
        } catch (Exception e) {
            // 这里只负责调用对用的脚本
            log.error("执行脚本报错");
        }
    }

    @Override
    public String getRedirectUrl(String customerId, String activeId) {
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


    private CustomerFeatureResponse.Feature convertFeatureByOverwrite(List<FeatureContent> featureContentByModel, FeatureContentSales featureContentBySales, Class<? extends Enum<?>> enumClass, Class type) {
        CustomerFeatureResponse.Feature featureVO = new CustomerFeatureResponse.Feature();
        // 多通电话覆盖+规则加工
        String resultAnswer = null;
        String resultAnswerLatest = null;
        // 获取
        if (!CollectionUtils.isEmpty(featureContentByModel)) {
            for (int i = featureContentByModel.size() - 1; i >= 0; i--) {
                if (!StringUtils.isEmpty(featureContentByModel.get(i).getAnswer()) &&
                        !featureContentByModel.get(i).getAnswer().equals("无") &&
                        !featureContentByModel.get(i).getAnswer().equals("null")) {
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
        //“已询问”有三个值：“是”、“否”、“不需要”。
        if (!CollectionUtils.isEmpty(featureContentByModel)) {
            //如果 funds_volume_model json list 中有一个 question 有值，就是 ‘是’;
            for (int i = featureContentByModel.size() - 1; i >= 0; i--) {
                if (!StringUtils.isEmpty(featureContentByModel.get(i).getQuestion()) &&
                        !featureContentByModel.get(i).getQuestion().equals("无") &&
                        !featureContentByModel.get(i).getQuestion().equals("null")) {
                    featureVO.setInquired("yes");
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

    private CustomerFeatureResponse.Feature converFeaturetByAppend(List<FeatureContent> featureContentByModel, FeatureContentSales featureContentBySales) {
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

        featureVO.setModelRecord(CollectionUtils.isEmpty(modelRecord) ? null : JsonUtil.serialize(modelRecord));
        featureVO.setSalesRecord(Objects.isNull(featureContentBySales) ? null : featureContentBySales.getContent());
        //“已询问”有三个值：“是”、“否”、“不需要”。
        if (!CollectionUtils.isEmpty(featureContentByModel)) {
            //如果 funds_volume_model json list 中有一个 question 有值，就是 ‘是’;
            for (int i = featureContentByModel.size() - 1; i >= 0; i--) {
                if (!StringUtils.isEmpty(featureContentByModel.get(i).getQuestion()) &&
                        !featureContentByModel.get(i).getQuestion().equals("无") &&
                        !featureContentByModel.get(i).getQuestion().equals("null")) {
                    featureVO.setInquired("yes");
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
                        break;
                    }
                }
            }
        }
        //否则就是 ‘否’
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
                            message.setTime(dateFormat.parse(dateString));
                            messageList.add(message);
                        }
                        chat.setMessages(messageList);
                        chat.setTime(dateFormat.parse(dateString));
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

    private CustomerProcessSummaryResponse.ProcessSummary getProcessSummary(CustomerFeature customerFeature, CustomerInfo customerInfo, CustomerStageStatus stageStatus) {
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
                            customerInfo.getCommunicationRounds() >= 1) {
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
                        questions.add(ttt.toString());
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
                            customerInfo.getCommunicationRounds() >= 1) {

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
                            questions.add(ttt.toString());
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
                questions.add("跟进错的客户");
            }

            // 功能讲解
            // 优点：-功能讲解让客户理解：“客户对软件功能的清晰度”的值为“是”
            // 缺点：-功能讲解未让客户理解：“客户对软件功能的清晰度”的值为“否”
            if (Objects.nonNull(customerFeatureResponse.getRecognition().getSoftwareFunctionClarity().getModelRecord()) &&
                    (Boolean) customerFeatureResponse.getRecognition().getSoftwareFunctionClarity().getModelRecord()) {
                advantage.add("功能讲解让客户理解");
            } else if (Objects.nonNull(customerFeatureResponse.getRecognition().getSoftwareFunctionClarity().getModelRecord()) &&
                    !(Boolean) customerFeatureResponse.getRecognition().getSoftwareFunctionClarity().getModelRecord()) {
                questions.add("功能讲解未让客户理解");
            }

            // 让客户认可价值
            // 优点：-成功让客户认可价值：相关字段全部为“是”——“客户对软件功能的清晰度”、“客户对销售讲的选股方法的认可度”、“客户对自身问题及影响的认可度”、“客户对软件价值的认可度”
            // 缺点：-未让客户认可价值：相关字段有一个以上为“否”——“客户对软件功能的清晰度”、“客户对销售讲的选股方法的认可度”、“客户对自身问题及影响的认可度”、“客户对软件价值的认可度”，并列出缺具体哪一项不为“是”（可以用括号放在后面显示）
            CustomerFeatureResponse.Recognition recognition = customerFeatureResponse.getRecognition();
            if (Objects.nonNull(recognition.getSoftwareFunctionClarity().getModelRecord()) &&
                    (Boolean) recognition.getSoftwareFunctionClarity().getModelRecord() &&
                    Objects.nonNull(recognition.getStockSelectionMethod().getModelRecord()) &&
                    (Boolean) recognition.getStockSelectionMethod().getModelRecord() &&
                    Objects.nonNull(recognition.getSelfIssueRecognition().getModelRecord()) &&
                    (Boolean) recognition.getSelfIssueRecognition().getModelRecord() &&
                    Objects.nonNull(recognition.getSoftwareValueApproval().getModelRecord()) &&
                    (Boolean) recognition.getSoftwareValueApproval().getModelRecord()) {
                advantage.add("成功让客户认可价值");
            } else {
                StringBuilder ttt = new StringBuilder("未让客户认可价值（");
                if (Objects.nonNull(recognition.getSoftwareFunctionClarity().getModelRecord()) &&
                        !(Boolean) recognition.getSoftwareFunctionClarity().getModelRecord()) {
                    ttt.append("客户对软件功能的清晰度，");
                }
                if (Objects.nonNull(recognition.getStockSelectionMethod().getModelRecord()) &&
                        !(Boolean) recognition.getStockSelectionMethod().getModelRecord()) {
                    ttt.append("客户对销售讲的选股方法的认可度，");
                }
                if (Objects.nonNull(recognition.getSelfIssueRecognition().getModelRecord()) &&
                        !(Boolean) recognition.getSelfIssueRecognition().getModelRecord()) {
                    ttt.append("客户对自身问题及影响的认可度，");
                }
                if (Objects.nonNull(recognition.getSoftwareValueApproval().getModelRecord()) &&
                        !(Boolean) recognition.getSoftwareValueApproval().getModelRecord()) {
                    ttt.append("客户对软件价值的认可度，");
                }
                ttt.deleteCharAt(ttt.length() - 1);
                ttt.append("）");
                if (ttt.length() > 15) {
                    questions.add(ttt.toString());
                }
            }
        } catch (Exception e) {
            log.error("获取优缺点失败");
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
            StringBuilder ttt = new StringBuilder("SOP执行顺序错误（缺失：");
            for (String status : questionStatus) {
                ttt.append(status).append("，");
            }
            ttt.deleteCharAt(ttt.length() - 1);
            ttt.append("）");
            questions.add(ttt.toString());
        }

        // 优点：-收集信息快（涉及时间戳，可考虑先去掉）
        // 缺点：-收集信息慢（涉及时间戳，可考虑先去掉）
        //-邀约听课成功：“客户回答自己是否会参加课程”的值为“是”（或者用听课次数和听课时长来判断？）
        //-邀约听课失败：“客户回答自己是否会参加课程”的值为“否”或空（或者用听课次数和听课时长来判断？）（前提条件是通话次数大于等于1 and 通话总时长大于等于2分钟）

        //-质疑应对失败：单个类别的质疑不认可的对话组数大于等于5，并列出是哪几类的质疑应对失败
        processSummary.setAdvantage(advantage);
        processSummary.setQuestions(questions);
        return processSummary;
    }

}
