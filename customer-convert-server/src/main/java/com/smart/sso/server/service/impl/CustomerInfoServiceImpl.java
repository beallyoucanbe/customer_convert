package com.smart.sso.server.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smart.sso.server.enums.FundsVolumeEnum;
import com.smart.sso.server.enums.HasTimeEnum;
import com.smart.sso.server.enums.LearningAbilityEnum;
import com.smart.sso.server.primary.mapper.CharacterCostTimeMapper;
import com.smart.sso.server.primary.mapper.CustomerFeatureMapper;
import com.smart.sso.server.primary.mapper.CustomerBaseMapper;
import com.smart.sso.server.primary.mapper.TelephoneRecordMapper;
import com.smart.sso.server.model.*;
import com.smart.sso.server.model.VO.CustomerListVO;
import com.smart.sso.server.model.VO.CustomerProfile;
import com.smart.sso.server.model.dto.*;
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
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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
    private CustomerBaseMapper customerBaseMapper;
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

    private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");


    @Override
    public CustomerBaseListResponse queryCustomerInfoList(CustomerInfoListRequest params) {
        Page<CustomerBase> selectPage = new Page<>(params.getPage(), params.getLimit());
        QueryWrapper<CustomerBase> queryWrapper = new QueryWrapper<>();

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
        Page<CustomerBase> resultPage = customerBaseMapper.selectPage(selectPage, queryWrapper);
        CustomerBaseListResponse result = new CustomerBaseListResponse();
        result.setTotal(resultPage.getTotal());
        result.setLimit(params.getLimit());
        result.setOffset(params.getPage());
        result.setCustomers(convert(resultPage.getRecords()));
        return result;
    }

    @Override
    public CustomerProfile queryCustomerById(String customerId, String activityId) {
        CustomerBase customerBase = customerBaseMapper.selectByCustomerIdAndCampaignId(customerId, activityId);
        if (Objects.isNull(customerBase)) {
            customerBase = recordService.syncCustomerInfoFromRecord(customerId, customerId);
            if (Objects.isNull(customerBase)) {
                return null;
            }
        }
        CustomerFeature featureFromSale = customerFeatureMapper.selectById(customerBase.getId());
        CustomerFeatureFromLLM featureFromLLM = recordService.getCustomerFeatureFromLLM(customerId, activityId);

        CustomerFeatureResponse customerFeature = convert2CustomerFeatureResponse(featureFromSale, featureFromLLM);

        CustomerProfile customerProfile = convert2CustomerProfile(customerBase);
        customerProfile.setCustomerStage(getCustomerStageStatus(customerBase, featureFromSale, featureFromLLM));
        customerProfile.setIsSend188(customerBase.getIsSend188());
        if (Objects.isNull(customerProfile.getCommunicationRounds())) {
            customerProfile.setCommunicationRounds(0);
        }
        // 这里重新判断下打电话的次数
        TelephoneRecordStatics round = recordService.getCommunicationRound(customerId, activityId);
        if (customerProfile.getCommunicationRounds() != round.getTotalCalls()) {
            customerBaseMapper.updateCommunicationRounds(customerId, activityId, round.getTotalCalls(), round.getLatestCommunicationTime());
            customerProfile.setCommunicationRounds(round.getTotalCalls());
        }
        // 重新判断一下匹配度，防止更新不及时的情况
        String conversionRate = getConversionRate(customerFeature);
        if (!customerBase.getConversionRate().equals(conversionRate)) {
            customerBaseMapper.updateConversionRateById(customerBase.getId(), conversionRate);
            customerProfile.setConversionRate(conversionRate);
        }
        customerProfile.setLastCommunicationDate(Objects.isNull(featureFromLLM) ? null : featureFromLLM.getCommunicationTime());
        return customerProfile;
    }

    @Override
    public CustomerFeatureResponse queryCustomerFeatureById(String customerId, String activityId) {
        CustomerBase customerBase = customerBaseMapper.selectByCustomerIdAndCampaignId(customerId, activityId);
        if (Objects.isNull(customerBase)) {
            customerBase = recordService.syncCustomerInfoFromRecord(customerId, customerId);
            if (Objects.isNull(customerBase)) {
                return null;
            }
        }
        CustomerFeature featureFromSale = customerFeatureMapper.selectById(customerBase.getId());
        CustomerFeatureFromLLM featureFromLLM = recordService.getCustomerFeatureFromLLM(customerId, activityId);
        // 没有通话记录，直接返回
        if (Objects.isNull(featureFromLLM)) {
            featureFromLLM = new CustomerFeatureFromLLM();
        }
        CustomerProcessSummary summaryResponse = convert2CustomerProcessSummaryResponse(featureFromLLM, featureFromSale);
        CustomerStageStatus stageStatus = getCustomerStageStatus(customerBase, featureFromSale, featureFromLLM);
        CustomerFeatureResponse customerFeature = convert2CustomerFeatureResponse(featureFromSale, featureFromLLM);
        if (Objects.nonNull(customerFeature)) {
            customerFeature.setTradingMethod(Objects.isNull(summaryResponse) ? null : summaryResponse.getTradingMethod());
            getStandardExplanationCompletion(customerFeature);
            customerFeature.setSummary(getProcessSummary(customerFeature, customerBase, stageStatus, summaryResponse));
        }
        setIntroduceService(featureFromLLM, customerFeature);
        setRemindService(featureFromLLM, customerFeature);
        setTeacherRemind(featureFromLLM, customerFeature);
        if (Objects.nonNull(customerFeature.getBasic().getFundsVolume()) &&
                Objects.nonNull(customerFeature.getBasic().getFundsVolume().getCustomerConclusion()) &&
                Objects.nonNull(customerFeature.getBasic().getFundsVolume().getCustomerConclusion().getModelRecord())){
            customerFeature.getWarmth().setFundsVolume(customerFeature.getBasic().getFundsVolume().getCustomerConclusion());
        }
        if (Objects.nonNull(customerFeature.getBasic().getHasTime()) &&
                Objects.nonNull(customerFeature.getBasic().getHasTime().getCustomerConclusion()) &&
                Objects.nonNull(customerFeature.getBasic().getHasTime().getCustomerConclusion().getModelRecord())){
            CustomerFeatureResponse.ChatContent hasTime = new CustomerFeatureResponse.ChatContent();
            hasTime.setValue(customerFeature.getBasic().getHasTime().getCustomerConclusion().getModelRecord().toString());
            hasTime.setOriginChat(customerFeature.getBasic().getHasTime().getCustomerConclusion().getOriginChat());
            customerFeature.getWarmth().setCustomerCourse(hasTime);
        }
        customerFeature.getHandoverPeriod().setCurrentStocks(customerFeature.getTradingMethod().getCurrentStocks());
        customerFeature.getHandoverPeriod().setTradingStyle(customerFeature.getTradingMethod().getTradingStyle());
        customerFeature.getHandoverPeriod().setStockMarketAge(customerFeature.getTradingMethod().getStockMarketAge());
        return customerFeature;
    }

    @Override
    public String getConversionRate(CustomerFeatureResponse customerFeature) {
        // "high", "medium", "low", "incomplete"
        // -较高：资金体量=“大于10万”
        // -中等：资金体量=“5到10万”
        // -较低：资金体量=“小于5万”
        // -未完成判断：资金体量=空
        String result = "incomplete";
        if (true) {
            return result;
        }
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
    public CustomerStageStatus getCustomerStageStatus(CustomerBase customerBase, CustomerFeature featureFromSale, CustomerFeatureFromLLM featureFromLLM) {
        CustomerFeatureResponse customerFeature = convert2CustomerFeatureResponse(featureFromSale, featureFromLLM);
        CustomerProcessSummary summaryResponse = convert2CustomerProcessSummaryResponse(featureFromLLM, featureFromSale);
        CustomerStageStatus stageStatus = new CustomerStageStatus();

        if (Objects.nonNull(customerFeature)) {
            // 客户信息收集:“客户的资金体量”有值 and“客户是否有时间听课”有值
            CustomerFeatureResponse.Basic basic = customerFeature.getBasic();
            try {
                if (Objects.nonNull(basic.getFundsVolume().getCustomerConclusion().getCompareValue()) &&
                        !basic.getFundsVolume().getCustomerConclusion().getCompareValue().equals("无") &&
                        !basic.getFundsVolume().getCustomerConclusion().getCompareValue().equals("null") &&
                        Objects.nonNull(basic.getHasTime().getCustomerConclusion().getCompareValue()) &&
                        !basic.getHasTime().getCustomerConclusion().getCompareValue().equals("无") &&
                        !basic.getHasTime().getCustomerConclusion().getCompareValue().equals("null")) {
                    stageStatus.setMatchingJudgment(1);
                }
            } catch (Exception e) {
                // 有异常就不变
            }
            // 客户交易风格了解-“客户自己的股票”、“客户的炒股风格”、“客户的股龄”，这3项的“客户结论”都有值
            CustomerProcessSummary.TradingMethod tradingMethod = summaryResponse.getTradingMethod();
            try {
                if (Objects.nonNull(tradingMethod.getCurrentStocks().getCustomerConclusion().getCompareValue()) &&
                        !tradingMethod.getCurrentStocks().getCustomerConclusion().getCompareValue().equals("无") &&
                        !tradingMethod.getCurrentStocks().getCustomerConclusion().getCompareValue().equals("null") &&
                        Objects.nonNull(tradingMethod.getTradingStyle().getCustomerConclusion().getCompareValue()) &&
                        !tradingMethod.getTradingStyle().getCustomerConclusion().getCompareValue().equals("无") &&
                        !tradingMethod.getTradingStyle().getCustomerConclusion().getCompareValue().equals("null") &&
                        Objects.nonNull(tradingMethod.getStockMarketAge().getCustomerConclusion().getCompareValue()) &&
                        !tradingMethod.getStockMarketAge().getCustomerConclusion().getCompareValue().equals("无") &&
                        !tradingMethod.getStockMarketAge().getCustomerConclusion().getCompareValue().equals("null")) {
                    stageStatus.setTransactionStyle(1);
                }
            } catch (Exception e) {
                // 有异常就不变
            }
            // 客户认可老师:“客户对老师的认可度”的值为“是”
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
            // 客户认可投入和价值:相关字段的值全部为'是”-“客户认可投入时间”“客户认可自己跟得上”、“客户认可价值”
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
        // 客户完成购买”，:CRM取回客户的购买状态值为“是”
        try {
            CustomerInfo customerInfo = customerRelationService.getByActivityAndCustomer(customerBase.getCustomerId(),
                    customerBase.getOwnerId(), customerBase.getActivityId());
            if (Objects.nonNull(customerInfo) && Objects.nonNull(customerInfo.getCustomerPurchaseStatus())
                    && customerInfo.getCustomerPurchaseStatus() == 1) {
                stageStatus.setCompletePurchase(1);
            }
        } catch (Exception e) {
            log.error("判断确认购买状态失败, ID={}", customerBase.getCustomerId());
        }
        return stageStatus;
    }

    @Override
    public List<ActivityInfoWithVersion> getActivityInfoByCustomerId(String customerId) {
        List<ActivityInfoWithVersion> result = new ArrayList<>();
        List<ActivityInfo> newActivity = customerBaseMapper.selectActivityInfoByCustomerId(customerId);
        for (ActivityInfo activityInfo : newActivity) {
            ActivityInfoWithVersion activityInfoWithVersion = new ActivityInfoWithVersion(activityInfo);
            result.add(activityInfoWithVersion);
        }
        return result;
    }

    @Override
    public void modifyCustomerFeatureById(String customerId, String activityId, CustomerFeatureResponse customerFeatureRequest) {
        CustomerBase customerBase = customerBaseMapper.selectByCustomerIdAndCampaignId(customerId, activityId);
        CustomerFeature customerFeature = customerFeatureMapper.selectById(customerBase.getId());
        if (Objects.isNull(customerFeature)) {
            customerFeature = new CustomerFeature();
            customerFeature.setId(customerBase.getId());
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

        if (Objects.nonNull(customerFeatureRequest.getWarmth())) {
            if (Objects.nonNull(customerFeatureRequest.getWarmth().getFundsVolume()) &&
                    (Objects.nonNull(customerFeatureRequest.getWarmth().getFundsVolume().getSalesRecord()) ||
                            Objects.nonNull(customerFeatureRequest.getWarmth().getFundsVolume().getSalesManualTag()))) {
                customerFeature.setFundsVolumeSales(new FeatureContentSales(customerFeatureRequest.getWarmth().getFundsVolume().getSalesRecord(),
                        customerFeatureRequest.getWarmth().getFundsVolume().getSalesManualTag(), DateUtil.getCurrentDateTime()));
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
    public void updateCharacterCostTime(String id) {
        // 总结各个特征的花费的时间
        CustomerBase customerBase = customerBaseMapper.selectById(id);
        QueryWrapper<TelephoneRecord> queryWrapperInfo = new QueryWrapper<>();
        queryWrapperInfo.eq("customer_id", customerBase.getCustomerId());
        queryWrapperInfo.orderByAsc("communication_time");
        // 查看该客户的所有通话记录，并且按照顺序排列
        List<TelephoneRecord> customerFeatureList = telephoneRecordMapper.selectList(queryWrapperInfo);
        CharacterCostTime characterCostTime = new CharacterCostTime();
        characterCostTime.setId(customerBase.getId());
        characterCostTime.setCustomerId(customerBase.getCustomerId());
        characterCostTime.setCustomerName(customerBase.getCustomerName());
        characterCostTime.setOwnerId(customerBase.getOwnerId());
        characterCostTime.setOwnerName(customerBase.getOwnerName());
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
        QueryWrapper<CustomerBase> queryWrapperInfo = new QueryWrapper<>();
        // 筛选时间
        queryWrapperInfo.eq("current_campaign", "361");
        List<CustomerBase> customerFeatureList = customerBaseMapper.selectList(queryWrapperInfo);
        System.out.println("总客户数：" + customerFeatureList.size());
        int customerNum = 0;
        int featureNum = 0;
        List<String> result = new ArrayList<>();
        for (CustomerBase customerBase : customerFeatureList) {
            CustomerFeatureResponse featureProfile = queryCustomerFeatureById(customerBase.getCustomerId(), customerBase.getActivityId());
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
                result.add(customerBase.getId());
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
        List<CustomerInfo> characterList = customerRelationService.getByActivity(activityId);
        for (CustomerInfo info : characterList) {
            CustomerBase customerBase = customerBaseMapper.selectByCustomerIdAndCampaignId(Long.toString(info.getCustomerId()), activityId);
            if (Objects.nonNull(customerBase)) {
                // 判断销售是否发生变更
                if(!info.getSalesId().toString().equals(customerBase.getOwnerId())){
                    customerBaseMapper.updateSalesById(customerBase.getId(), info.getSalesId().toString(), info.getSalesName());
                }
            } else {
                customerBase = new CustomerBase();
                customerBase.setId(CommonUtils.generatePrimaryKey());
                customerBase.setCustomerId(Long.toString(info.getCustomerId()));
                customerBase.setOwnerId(Long.toString(info.getSalesId()));
                customerBase.setCustomerName(info.getCustomerName());
                customerBase.setOwnerName(info.getSalesName());
                customerBase.setActivityId(activityId);
                customerBase.setActivityName(info.getActivityName());
                customerBase.setUpdateTimeTelephone(LocalDateTime.now());
                customerBase.setPurchaseTime(info.getPurchaseTime());
                customerBaseMapper.insert(customerBase);
            }
        }
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


    public List<CustomerListVO> convert(List<CustomerBase> customerBaseList) {
        return customerBaseList.stream().map(item -> {
            CustomerListVO customerListVO = new CustomerListVO();
            BeanUtils.copyProperties(item, customerListVO);
            return customerListVO;
        }).collect(Collectors.toList());
    }

    public CustomerProfile convert2CustomerProfile(CustomerBase customerBase) {
        if (Objects.isNull(customerBase)) {
            return null;
        }
        CustomerProfile customerProfile = new CustomerProfile();
        BeanUtils.copyProperties(customerBase, customerProfile);
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
        basic.setFundsVolume(convertBaseFeatureByOverwrite(featureFromLLM.getFundsVolume(), Objects.isNull(featureFromSale) ? null : featureFromSale.getFundsVolumeSales(), FundsVolumeEnum.class, String.class));
        basic.setHasTime(convertBaseFeatureByOverwrite(featureFromLLM.getHasTime(), Objects.isNull(featureFromSale) ? null : featureFromSale.getHasTimeSales(), HasTimeEnum.class, String.class));

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
        TradeMethodFeature tradeMethodFeature = new TradeMethodFeature(convertFeatureByOverwrite(featureContentByModel, featureContentBySales, enumClass, type, false));
        CustomerProcessSummary.ProcessInfoExplanationContent explanationContent =
                new CustomerProcessSummary.ProcessInfoExplanationContent();
        explanationContent.setResult(Boolean.FALSE);
        if (Objects.nonNull(featureContentByModel) && !StringUtils.isEmpty(featureContentByModel.getExplanation()) &&
                !featureContentByModel.getExplanation().trim().equals("无") &&
                !featureContentByModel.getExplanation().trim().equals("null")) {
            explanationContent.setResult(Boolean.TRUE);
            explanationContent.setOriginChat(CommonUtils.getOriginChatFromChatText(
                    StringUtils.isEmpty(featureContentByModel.getQuestionCallId()) ? featureContentByModel.getCallId() : featureContentByModel.getQuestionCallId(),
                    featureContentByModel.getExplanation()));
        }
        tradeMethodFeature.setStandardAction(explanationContent);
        return tradeMethodFeature;
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

    private CustomerFeatureResponse.ProcessSummary getProcessSummary(CustomerFeatureResponse customerFeature, CustomerBase customerBase, CustomerStageStatus stageStatus, CustomerProcessSummary summaryResponse) {
        CustomerFeatureResponse.ProcessSummary processSummary = new CustomerFeatureResponse.ProcessSummary();
        List<String> advantage = new ArrayList<>();
        List<CustomerFeatureResponse.Question> questions = new ArrayList<>();
        try {
            // 【交接期】客户信息收集
            // 优点：-【交接期】完成客户信息收集:里程碑“客户信息收集”的值为“完成”
            // 缺点：-【交接期】未完成客户信息收集:里程碑“客户信息收集”的值为“未完成”
            int matchingJudgment = stageStatus.getMatchingJudgment();
            if (matchingJudgment == 1) {
                advantage.add("【交接期】完成客户信息收集");
            } else {
                questions.add(new CustomerFeatureResponse.Question("【交接期】未完成客户信息收集"));
            }
            // 【交接期】客户交易风格了解
            // 优点：-【交接期】完成客户交易风格了解:里程碑“客户交易风格了解”的值为“完成
            // 缺点：-【交接期】未完成客户交易风格了解:里程碑“客户交易风格了解”的值为“未完成’
            int tradingStyle = stageStatus.getTransactionStyle();
            if (tradingStyle == 1) {
                advantage.add("【交接期】完成客户交易风格了解");
            } else if (Objects.nonNull(customerBase.getCommunicationRounds()) &&
                    customerBase.getCommunicationRounds() >= 2) {
                questions.add(new CustomerFeatureResponse.Question("【交接期】未完成客户交易风格了解"));
            }
            // 【交接期】完整介绍服务内容
            // 优点：-【交接期】完整介绍服务内容:字段“是否完整介绍服务内容”的值为“是’
            // 缺点：-【交接期】未完整介绍服务内容:字段“是否完整介绍服务内容”的值为“否’
            if ((Boolean) customerFeature.getHandoverPeriod().getBasic().getCompleteIntro().getValue()) {
                advantage.add("【交接期】完整介绍服务内容");
            } else {
                questions.add(new CustomerFeatureResponse.Question("【交接期】未完整介绍服务内容"));
            }
            // 【交接期】提醒查看盘中直播频次
            // 优点：-【交接期】提醒查看盘中直播频次较高:字段“提醒查看盘中直播频次”的值为“高”或“中’
            // 缺点：-【交接期】提醒查看盘中直播频次较低:字段“提醒查看盘中直播频次”的值为“低
            if (Objects.nonNull(customerFeature.getHandoverPeriod().getBasic().getRemindFreq().getValue())) {
                Double fre = (Double) customerFeature.getHandoverPeriod().getBasic().getRemindFreq().getValue();
                if (fre < 2) {
                    advantage.add("【交接期】提醒查看盘中直播频次较高");
                } else {
                    questions.add(new CustomerFeatureResponse.Question("【交接期】提醒查看盘中直播频次较低"));
                }
            }
            // 【交接期】直播/圈子内容传递频次
            // 优点：-【交接期】直播/圈子内容传递频次较高:字段“直播/圈子内容传递频次”的值为“高”或“中
            // 缺点：-【交接期】直播/圈子内容传递频次较低:字段“直播/圈子内容传递频次”的值为“低”
            if (Objects.nonNull(customerFeature.getHandoverPeriod().getBasic().getTransFreq().getValue())) {
                Double fre = (Double) customerFeature.getHandoverPeriod().getBasic().getTransFreq().getValue();
                if (fre < 2) {
                    advantage.add("【交接期】直播/圈子内容传递频次较高");
                } else {
                    questions.add(new CustomerFeatureResponse.Question("【交接期】直播/圈子内容传递频次较低"));
                }
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
                } else if (Objects.nonNull(customerBase.getCommunicationRounds()) &&
                        customerBase.getCommunicationRounds() >= 3) {
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

    private void setIntroduceService(CustomerFeatureFromLLM featureFromLLM, CustomerFeatureResponse customerFeature){
        // 5个维度：
        if(Objects.nonNull(featureFromLLM.getIntroduceService_1()) &&
                Objects.nonNull(featureFromLLM.getIntroduceService_2()) &&
                Objects.nonNull(featureFromLLM.getIntroduceService_3()) &&
                Objects.nonNull(featureFromLLM.getIntroduceService_4()) &&
                Objects.nonNull(featureFromLLM.getIntroduceService_5()) &&
                !StringUtils.isEmpty(featureFromLLM.getIntroduceService_1().getAnswerText()) &&
                !StringUtils.isEmpty(featureFromLLM.getIntroduceService_2().getAnswerText()) &&
                !StringUtils.isEmpty(featureFromLLM.getIntroduceService_3().getAnswerText()) &&
                !StringUtils.isEmpty(featureFromLLM.getIntroduceService_4().getAnswerText()) &&
                !StringUtils.isEmpty(featureFromLLM.getIntroduceService_5().getAnswerText())){
            customerFeature.getHandoverPeriod().getBasic().getCompleteIntro().setValue(Boolean.TRUE);
        } else {
            customerFeature.getHandoverPeriod().getBasic().getCompleteIntro().setValue(Boolean.FALSE);
        }
        CustomerFeatureResponse.RecordContent recordContent = new CustomerFeatureResponse.RecordContent();
        List<CustomerFeatureResponse.RecordTitle> columns = new ArrayList<>();
        columns.add(new CustomerFeatureResponse.RecordTitle("event_type", "维度名"));
        columns.add(new CustomerFeatureResponse.RecordTitle("event_content", "原文摘要"));
        recordContent.setColumns(columns);

        List<Map<String, Object>> data = new ArrayList<>();
        //1、盘中直播"沙场点兵"、回放位置
        if(Objects.nonNull(featureFromLLM.getIntroduceService_1()) && !StringUtils.isEmpty(featureFromLLM.getIntroduceService_1().getAnswerText())){
            Map<String, Object> item = new HashMap<>();
            item.put("event_type", "盘中直播\"沙场点兵\"、回放位置");
            item.put("event_content", CommonUtils.getOriginChatFromChatText(featureFromLLM.getIntroduceService_1().getCallId(), featureFromLLM.getIntroduceService_1().getAnswerText()));
            data.add(item);
        } else {
            Map<String, Object> item = new HashMap<>();
            item.put("event_type", "盘中直播\"沙场点兵\"、回放位置");
            item.put("event_content", null);
            data.add(item);
        }
        //2、"智能投教圈"、提醒客户查收老师信息
        if(Objects.nonNull(featureFromLLM.getIntroduceService_2()) && !StringUtils.isEmpty(featureFromLLM.getIntroduceService_2().getAnswerText())){
            Map<String, Object> item = new HashMap<>();
            item.put("event_type", "\"智能投教圈\"、提醒客户查收老师信息");
            item.put("event_content", CommonUtils.getOriginChatFromChatText(featureFromLLM.getIntroduceService_2().getCallId(), featureFromLLM.getIntroduceService_2().getAnswerText()));
            data.add(item);
        } else {
            Map<String, Object> item = new HashMap<>();
            item.put("event_type", "\"智能投教圈\"、提醒客户查收老师信息");
            item.put("event_content", null);
            data.add(item);
        }
        //3、老师相关课程位置
        if(Objects.nonNull(featureFromLLM.getIntroduceService_3()) && !StringUtils.isEmpty(featureFromLLM.getIntroduceService_3().getAnswerText())){
            Map<String, Object> item = new HashMap<>();
            item.put("event_type", "老师相关课程位置");
            item.put("event_content", CommonUtils.getOriginChatFromChatText(featureFromLLM.getIntroduceService_3().getCallId(), featureFromLLM.getIntroduceService_3().getAnswerText()));
            data.add(item);
        } else {
            Map<String, Object> item = new HashMap<>();
            item.put("event_type", "老师相关课程位置");
            item.put("event_content", null);
            data.add(item);
        }
        //4、16节交付大课都包含什么内容
        if(Objects.nonNull(featureFromLLM.getIntroduceService_4()) && !StringUtils.isEmpty(featureFromLLM.getIntroduceService_4().getAnswerText())){
            Map<String, Object> item = new HashMap<>();
            item.put("event_type", "16节交付大课都包含什么内容");
            item.put("event_content", CommonUtils.getOriginChatFromChatText(featureFromLLM.getIntroduceService_4().getCallId(), featureFromLLM.getIntroduceService_4().getAnswerText()));
            data.add(item);
        } else {
            Map<String, Object> item = new HashMap<>();
            item.put("event_type", "16节交付大课都包含什么内容");
            item.put("event_content", null);
            data.add(item);
        }
        //5、软件功能指标位置
        if(Objects.nonNull(featureFromLLM.getIntroduceService_5()) && !StringUtils.isEmpty(featureFromLLM.getIntroduceService_5().getAnswerText())){
            Map<String, Object> item = new HashMap<>();
            item.put("event_type", "软件功能指标位置");
            item.put("event_content", CommonUtils.getOriginChatFromChatText(featureFromLLM.getIntroduceService_5().getCallId(), featureFromLLM.getIntroduceService_5().getAnswerText()));
            data.add(item);
        } else {
            Map<String, Object> item = new HashMap<>();
            item.put("event_type", "软件功能指标位置");
            item.put("event_content", null);
            data.add(item);
        }
        recordContent.setData(data);
        customerFeature.getHandoverPeriod().getBasic().getCompleteIntro().setRecords(recordContent);
    }

    private void setRemindService(CustomerFeatureFromLLM featureFromLLM, CustomerFeatureResponse customerFeature){
        // 提醒查看盘中直播：
        CustomerFeatureResponse.RecordContent recordContent = new CustomerFeatureResponse.RecordContent();
        List<CustomerFeatureResponse.RecordTitle> columns = new ArrayList<>();
        columns.add(new CustomerFeatureResponse.RecordTitle("event_type", "会话时间"));
        columns.add(new CustomerFeatureResponse.RecordTitle("event_content", "原文摘要"));
        recordContent.setColumns(columns);

        List<Map<String, Object>> data = new ArrayList<>();
        List<String> allTimeStr = new ArrayList<>();
        int count = 0;
        //1、销售提醒客户查看"盘中直播"的语句
        if(!CollectionUtils.isEmpty(featureFromLLM.getRemindService_1())){
            for (CommunicationContent one : featureFromLLM.getRemindService_1()){
                Map<String, Object> item = new HashMap<>();
                item.put("event_type", one.getTs());
                item.put("event_content", CommonUtils.getOriginChatFromChatText(one.getCallId(), one.getAnswerText()));
                data.add(item);
                allTimeStr.add(one.getTs());
                count++;
            }
        }
        Collections.sort(data, new Comparator<Map<String, Object>>() {
            @Override
            public int compare(Map<String, Object> o1, Map<String, Object> o2) {
                String eventType1 = (String) o1.get("event_type");
                String eventType2 = (String) o2.get("event_type");
                return eventType2.compareTo(eventType1); // 字符串按字典序比较
            }
        });
        recordContent.setData(data);

        // 计算频次
        if (!CollectionUtils.isEmpty(allTimeStr)) {
            List<String> sortedTime = allTimeStr.stream().sorted().collect(Collectors.toList());
            int days = calculateDaysDifference(sortedTime.get(0));
            // 这里计算平均多少天一次
            double fre = (double) days/count;
            String formattedResult = String.format("%.1f", fre);
            customerFeature.getHandoverPeriod().getBasic().getRemindFreq().setValue(Double.parseDouble(formattedResult));
        }

        customerFeature.getHandoverPeriod().getBasic().getRemindFreq().setRecords(recordContent);
    }

    public int calculateDaysDifference(String inputTime) {
        // 定义时间格式
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        // 解析输入的时间字符串
        LocalDateTime startDateTime = LocalDateTime.parse(inputTime, formatter);
        LocalDateTime now = LocalDateTime.now();

        // 如果输入时间晚于当前时间，抛出异常
        if (startDateTime.isAfter(now)) {
            throw new IllegalArgumentException("Input time must be earlier than the current time.");
        }

        // 转换为LocalDate，忽略时间部分
        LocalDate startDate = startDateTime.toLocalDate();
        LocalDate currentDate = now.toLocalDate();
        int workdays = 0;
        // 从起始日期开始逐天迭代，计算工作日
        while (!startDate.isAfter(currentDate)) {
            if (isWorkday(startDate)) {
                workdays++;
            }
            startDate = startDate.plusDays(1);
        }
        return workdays;
    }

    // 判断是否为工作日（周一到周五）
    private boolean isWorkday(LocalDate date) {
        switch (date.getDayOfWeek()) {
            case MONDAY:
            case TUESDAY:
            case WEDNESDAY:
            case THURSDAY:
            case FRIDAY:
                return true;
            default:
                return false;
        }
    }

    private void setTeacherRemind(CustomerFeatureFromLLM featureFromLLM, CustomerFeatureResponse customerFeature){
        // 直播/圈子内容传递频次（区分三个老师的姓名）
        CustomerFeatureResponse.RecordContent recordContent = new CustomerFeatureResponse.RecordContent();
        List<CustomerFeatureResponse.RecordTitle> columns = new ArrayList<>();
        columns.add(new CustomerFeatureResponse.RecordTitle("event_type", "会话时间"));
        columns.add(new CustomerFeatureResponse.RecordTitle("event_content", "原文摘要"));
        recordContent.setColumns(columns);

        List<Map<String, Object>> data = new ArrayList<>();
        List<String> allTimeStr = new ArrayList<>();
        int count = 0;

        //1，销售提醒客户查看"圈子内容"的语句
        if(!CollectionUtils.isEmpty(featureFromLLM.getRemindService_2())){
            for (CommunicationContent one : featureFromLLM.getRemindService_2()){
                Map<String, Object> item = new HashMap<>();
                item.put("event_type", one.getTs());
                item.put("event_content", CommonUtils.getOriginChatFromChatText(one.getCallId(), one.getAnswerText()));
                data.add(item);
                allTimeStr.add(one.getTs());
                count++;
            }
        }
        Collections.sort(data, new Comparator<Map<String, Object>>() {
            @Override
            public int compare(Map<String, Object> o1, Map<String, Object> o2) {
                String eventType1 = (String) o1.get("event_type");
                String eventType2 = (String) o2.get("event_type");
                return eventType2.compareTo(eventType1); // 字符串按字典序比较
            }
        });
        recordContent.setData(data);
        // 计算频次
        if (!CollectionUtils.isEmpty(allTimeStr)) {
            List<String> sortedTime = allTimeStr.stream().sorted().collect(Collectors.toList());
            int days = calculateDaysDifference(sortedTime.get(0));
            // 这里计算平均多少天一次
            double fre = (double) days/count;
            String formattedResult = String.format("%.1f", fre);
            customerFeature.getHandoverPeriod().getBasic().getTransFreq().setValue(Double.parseDouble(formattedResult));
        }
        customerFeature.getHandoverPeriod().getBasic().getTransFreq().setRecords(recordContent);
    }
}
