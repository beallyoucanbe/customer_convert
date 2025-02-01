package com.smart.sso.server.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smart.sso.server.enums.FundsVolumeEnum;
import com.smart.sso.server.enums.HasTimeEnum;
import com.smart.sso.server.primary.mapper.CharacterCostTimeMapper;
import com.smart.sso.server.primary.mapper.CustomerFeatureMapper;
import com.smart.sso.server.primary.mapper.CustomerBaseMapper;
import com.smart.sso.server.primary.mapper.TelephoneRecordMapper;
import com.smart.sso.server.model.*;
import com.smart.sso.server.model.VO.CustomerListVO;
import com.smart.sso.server.model.VO.CustomerProfile;
import com.smart.sso.server.model.dto.*;
import com.smart.sso.server.service.*;
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
    private EventService eventService;
    @Autowired
    @Lazy
    private MessageService messageService;

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
            customerFeature.setSummary(getProcessSummary(customerFeature, customerBase, stageStatus, summaryResponse));
        }
        setIntroduceService(featureFromLLM, customerFeature);
        setRemindLive(featureFromLLM, customerFeature, customerBase.getCreateTime());
        setRemindCommunity(featureFromLLM, customerFeature, customerBase.getCreateTime());
        customerFeature.getWarmth().setVisitLiveFreq(eventService.getVisitLiveFreqContent(customerId, customerBase.getCreateTime()));
        customerFeature.getWarmth().setVisitCommunityFreq(eventService.getVisitCommunityFreqContent(customerId, customerBase.getCreateTime()));
        customerFeature.getWarmth().setFunctionFreq(eventService.getFunctionFreqContent(customerId, customerBase.getCreateTime()));
        if (Objects.nonNull(customerFeature.getBasic().getFundsVolume()) &&
                Objects.nonNull(customerFeature.getBasic().getFundsVolume().getCustomerConclusion()) &&
                Objects.nonNull(customerFeature.getBasic().getFundsVolume().getCustomerConclusion().getModelRecord())) {
            customerFeature.getWarmth().setFundsVolume(customerFeature.getBasic().getFundsVolume().getCustomerConclusion());
        }
        if (Objects.nonNull(customerFeature.getBasic().getHasTime()) &&
                Objects.nonNull(customerFeature.getBasic().getHasTime().getCustomerConclusion()) &&
                Objects.nonNull(customerFeature.getBasic().getHasTime().getCustomerConclusion().getModelRecord())) {
            CustomerFeatureResponse.ChatContent hasTime = new CustomerFeatureResponse.ChatContent();
            hasTime.setValue(customerFeature.getBasic().getHasTime().getCustomerConclusion().getModelRecord().toString());
            hasTime.setOriginChat(customerFeature.getBasic().getHasTime().getCustomerConclusion().getOriginChat());
            customerFeature.getWarmth().setCustomerCourse(hasTime);
        }
        customerFeature.getHandoverPeriod().setCurrentStocks(customerFeature.getTradingMethod().getCurrentStocks());
        customerFeature.getHandoverPeriod().setTradingStyle(customerFeature.getTradingMethod().getTradingStyle());
        customerFeature.getHandoverPeriod().setStockMarketAge(customerFeature.getTradingMethod().getStockMarketAge());

        setDeliveryPeriod(customerBase, featureFromLLM, customerFeature, customerBase.getCreateTime());


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
                if (!info.getSalesId().toString().equals(customerBase.getOwnerId())) {
                    customerBaseMapper.updateSalesById(customerBase.getId(), info.getSalesId().toString(), info.getSalesName());
                }
                if (!Objects.equals(info.getCustomerRefundStatus(), customerBase.getCustomerRefundStatus())) {
                    customerBaseMapper.updateRefundStatusById(customerBase.getId(), info.getCustomerRefundStatus(), info.getRefundTime());
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
                customerBase.setRefundTime(info.getRefundTime());
                customerBase.setCustomerRefundStatus(info.getCustomerRefundStatus());
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
        basic.setHasTime(convertBaseFeatureByOverwrite(featureFromLLM.getHasTime(), Objects.isNull(featureFromSale) ? null : featureFromSale.getHasTimeSales(), HasTimeEnum.class, String.class));
        customerFeatureResponse.setBasic(basic);
        return customerFeatureResponse;
    }

    public CustomerProcessSummary convert2CustomerProcessSummaryResponse(CustomerFeatureFromLLM featureFromLLM, CustomerFeature featureFromSale) {
        if (Objects.isNull(featureFromLLM)) {
            return null;
        }
        CustomerProcessSummary customerSummaryResponse = new CustomerProcessSummary();
        CustomerProcessSummary.ProcessInfoExplanation infoExplanation = new CustomerProcessSummary.ProcessInfoExplanation();
        customerSummaryResponse.setInfoExplanation(infoExplanation);

        CustomerProcessSummary.TradingMethod tradingMethod = new CustomerProcessSummary.TradingMethod();
        tradingMethod.setCurrentStocks(convertTradeMethodFeatureByOverwrite(featureFromLLM.getCurrentStocks(), Objects.isNull(featureFromSale) ? null : featureFromSale.getCurrentStocksSales(), null, String.class));
        tradingMethod.setTradingStyle(convertTradeMethodFeatureByOverwrite(featureFromLLM.getTradingStyle(), Objects.isNull(featureFromSale) ? null : featureFromSale.getTradingStyleSales(), null, String.class));
        tradingMethod.setStockMarketAge(convertTradeMethodFeatureByOverwrite(featureFromLLM.getStockMarketAge(), Objects.isNull(featureFromSale) ? null : featureFromSale.getStockMarketAgeSales(), null, String.class));

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
            } else {
                questions.add(new CustomerFeatureResponse.Question("【交接期】未完成客户交易风格了解"));
            }
            // 【交接期】完整介绍服务内容
            // 优点：-【交接期】完整介绍服务内容:字段“是否完整介绍服务内容”的值为“是’
            // 缺点：-【交接期】未完整介绍服务内容:字段“是否完整介绍服务内容”的值为“否’
            if (Objects.nonNull(customerFeature.getHandoverPeriod().getBasic().getCompleteIntro().getValue()) &&
                    (Boolean) customerFeature.getHandoverPeriod().getBasic().getCompleteIntro().getValue()) {
                advantage.add("【交接期】完整介绍服务内容");
            } else {
                questions.add(new CustomerFeatureResponse.Question("【交接期】未完整介绍服务内容"));
            }
            // 【交接期】提醒查看盘中直播频次
            // 优点：-【交接期】提醒查看盘中直播频次较高:字段“提醒查看盘中直播频次”的值为“高”或“中’
            // 缺点：-【交接期】提醒查看盘中直播频次较低:字段“提醒查看盘中直播频次”的值为“低
            if (Objects.nonNull(customerFeature.getHandoverPeriod().getBasic().getRemindLiveFreq().getValue())) {
                Double fre = (Double) customerFeature.getHandoverPeriod().getBasic().getRemindLiveFreq().getValue();
                if (fre < 2) {
                    advantage.add("【交接期】提醒查看盘中直播频次较高");
                } else {
                    questions.add(new CustomerFeatureResponse.Question("【交接期】提醒查看盘中直播频次较低"));
                }
            }
            // 【交接期】圈子内容传递频次
            // 优点：-【交接期】提醒查看圈子内容频次较高:字段“提醒查看圈子内容频次”的值为“高”或“中"
            // 缺点：-【交接期】提醒查看圈子内容频次较低:字段“提醒查看圈子内容频次”的值为“低’
            if (Objects.nonNull(customerFeature.getHandoverPeriod().getBasic().getRemindCommunityFreq().getValue())) {
                Double fre = (Double) customerFeature.getHandoverPeriod().getBasic().getRemindCommunityFreq().getValue();
                if (fre < 2) {
                    advantage.add("【交接期】直播/圈子内容传递频次较高");
                } else {
                    questions.add(new CustomerFeatureResponse.Question("【交接期】直播/圈子内容传递频次较低"));
                }
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

    private Boolean determineTradingMethod(TradeMethodFeature tradeMethodFeature) {
        return tradeMethodFeature.getInquired().equals("yes")
                && Objects.nonNull(tradeMethodFeature.getCustomerConclusion())
                && !StringUtils.isEmpty(tradeMethodFeature.getCustomerConclusion().getCompareValue())
                && Objects.nonNull(tradeMethodFeature.getStandardAction())
                && Objects.nonNull(tradeMethodFeature.getStandardAction().getResult())
                && tradeMethodFeature.getStandardAction().getResult();
    }

    private void setIntroduceService(CustomerFeatureFromLLM featureFromLLM, CustomerFeatureResponse customerFeature) {
        // 5个维度：
        int completeInfo = 0;
        if (Objects.nonNull(featureFromLLM.getIntroduceService_1()) &&
                !StringUtils.isEmpty(featureFromLLM.getIntroduceService_1().getAnswerText())) {
            completeInfo += 20;
        }
        if (Objects.nonNull(featureFromLLM.getIntroduceService_2()) &&
                !StringUtils.isEmpty(featureFromLLM.getIntroduceService_2().getAnswerText())) {
            completeInfo += 20;
        }
        if (Objects.nonNull(featureFromLLM.getIntroduceService_3()) &&
                !StringUtils.isEmpty(featureFromLLM.getIntroduceService_3().getAnswerText())) {
            completeInfo += 20;
        }
        if (Objects.nonNull(featureFromLLM.getIntroduceService_4()) &&
                !StringUtils.isEmpty(featureFromLLM.getIntroduceService_4().getAnswerText())) {
            completeInfo += 20;
        }
        if (Objects.nonNull(featureFromLLM.getIntroduceService_5()) &&
                !StringUtils.isEmpty(featureFromLLM.getIntroduceService_5().getAnswerText())) {
            completeInfo += 20;
        }
        customerFeature.getHandoverPeriod().getBasic().getCompleteIntro().setValue(completeInfo);
        CustomerFeatureResponse.RecordContent recordContent = new CustomerFeatureResponse.RecordContent();
        List<CustomerFeatureResponse.RecordTitle> columns = new ArrayList<>();
        columns.add(new CustomerFeatureResponse.RecordTitle("event_type", "维度名"));
        columns.add(new CustomerFeatureResponse.RecordTitle("event_content", "原文摘要"));
        recordContent.setColumns(columns);

        List<Map<String, Object>> data = new ArrayList<>();
        //1、盘中直播"沙场点兵"、回放位置
        if (Objects.nonNull(featureFromLLM.getIntroduceService_1()) && !StringUtils.isEmpty(featureFromLLM.getIntroduceService_1().getAnswerText())) {
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
        if (Objects.nonNull(featureFromLLM.getIntroduceService_2()) && !StringUtils.isEmpty(featureFromLLM.getIntroduceService_2().getAnswerText())) {
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
        if (Objects.nonNull(featureFromLLM.getIntroduceService_3()) && !StringUtils.isEmpty(featureFromLLM.getIntroduceService_3().getAnswerText())) {
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
        if (Objects.nonNull(featureFromLLM.getIntroduceService_4()) && !StringUtils.isEmpty(featureFromLLM.getIntroduceService_4().getAnswerText())) {
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
        if (Objects.nonNull(featureFromLLM.getIntroduceService_5()) && !StringUtils.isEmpty(featureFromLLM.getIntroduceService_5().getAnswerText())) {
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

    private void setRemindLive(CustomerFeatureFromLLM featureFromLLM,
                               CustomerFeatureResponse customerFeature,
                               LocalDateTime customerCreateTime) {
        // 提醒查看盘中直播：
        CustomerFeatureResponse.RecordContent recordContent = new CustomerFeatureResponse.RecordContent();
        List<CustomerFeatureResponse.RecordTitle> columns = new ArrayList<>();
        columns.add(new CustomerFeatureResponse.RecordTitle("event_time", "会话时间"));
        columns.add(new CustomerFeatureResponse.RecordTitle("event_content", "原文摘要"));
        recordContent.setColumns(columns);

        List<Map<String, Object>> data = new ArrayList<>();
        List<String> allTimeStr = new ArrayList<>();
        int count = 0;
        //1、销售提醒客户查看"盘中直播"的语句
        if (!CollectionUtils.isEmpty(featureFromLLM.getRemindService_1())) {
            for (CommunicationContent one : featureFromLLM.getRemindService_1()) {
                Map<String, Object> item = new HashMap<>();
                item.put("event_time", one.getTs());
                item.put("event_content", CommonUtils.getOriginChatFromChatText(one.getCallId(), one.getAnswerText()));
                data.add(item);
                allTimeStr.add(one.getTs());
                count++;
            }
        }
        Collections.sort(data, new Comparator<Map<String, Object>>() {
            @Override
            public int compare(Map<String, Object> o1, Map<String, Object> o2) {
                String eventType1 = (String) o1.get("event_time");
                String eventType2 = (String) o2.get("event_time");
                return eventType2.compareTo(eventType1); // 字符串按字典序比较
            }
        });
        recordContent.setData(data);

        // 计算频次
        if (!CollectionUtils.isEmpty(allTimeStr)) {
            int days = CommonUtils.calculateDaysDifference(customerCreateTime);
            // 这里计算平均多少天一次
            double fre = (double) days / count;
            String formattedResult = String.format("%.1f", fre);
            customerFeature.getHandoverPeriod().getBasic().getRemindLiveFreq().setValue(Double.parseDouble(formattedResult));
        }

        customerFeature.getHandoverPeriod().getBasic().getRemindLiveFreq().setRecords(recordContent);
    }


    private void setRemindCommunity(CustomerFeatureFromLLM featureFromLLM,
                                    CustomerFeatureResponse customerFeature,
                                    LocalDateTime customerCreateTime) {
        // 直播/圈子内容传递频次（区分三个老师的姓名）
        CustomerFeatureResponse.RecordContent recordContent = new CustomerFeatureResponse.RecordContent();
        List<CustomerFeatureResponse.RecordTitle> columns = new ArrayList<>();
        columns.add(new CustomerFeatureResponse.RecordTitle("event_time", "会话时间"));
        columns.add(new CustomerFeatureResponse.RecordTitle("event_content", "原文摘要"));
        recordContent.setColumns(columns);

        List<Map<String, Object>> data = new ArrayList<>();
        List<String> allTimeStr = new ArrayList<>();
        int count = 0;

        //1，销售提醒客户查看"圈子内容"的语句
        if (!CollectionUtils.isEmpty(featureFromLLM.getRemindService_2())) {
            for (CommunicationContent one : featureFromLLM.getRemindService_2()) {
                Map<String, Object> item = new HashMap<>();
                item.put("event_time", one.getTs());
                item.put("event_content", CommonUtils.getOriginChatFromChatText(one.getCallId(), one.getAnswerText()));
                data.add(item);
                allTimeStr.add(one.getTs());
                count++;
            }
        }
        Collections.sort(data, new Comparator<Map<String, Object>>() {
            @Override
            public int compare(Map<String, Object> o1, Map<String, Object> o2) {
                String eventType1 = (String) o1.get("event_time");
                String eventType2 = (String) o2.get("event_time");
                return eventType2.compareTo(eventType1); // 字符串按字典序比较
            }
        });
        recordContent.setData(data);
        // 计算频次
        if (!CollectionUtils.isEmpty(allTimeStr)) {
            int days = CommonUtils.calculateDaysDifference(customerCreateTime);
            // 这里计算平均多少天一次
            double fre = (double) days / count;
            String formattedResult = String.format("%.1f", fre);
            customerFeature.getHandoverPeriod().getBasic().getRemindCommunityFreq().setValue(Double.parseDouble(formattedResult));
        }
        customerFeature.getHandoverPeriod().getBasic().getRemindCommunityFreq().setRecords(recordContent);
    }

    private void setDeliveryRemindLive(CustomerFeatureFromLLM featureFromLLM,
                                       CustomerFeatureResponse customerFeature,
                                       LocalDateTime customerCreateTime) {
        // 提醒查看交付课直播：
        CustomerFeatureResponse.RecordContent recordContent = new CustomerFeatureResponse.RecordContent();
        List<CustomerFeatureResponse.RecordTitle> columns = new ArrayList<>();
        columns.add(new CustomerFeatureResponse.RecordTitle("event_time", "会话时间"));
        columns.add(new CustomerFeatureResponse.RecordTitle("event_content", "原文摘要"));
        recordContent.setColumns(columns);

        List<Map<String, Object>> data = new ArrayList<>();
        List<String> allTimeStr = new ArrayList<>();
        int count = 0;
        //1、提醒查看交付课直播
        if (!CollectionUtils.isEmpty(featureFromLLM.getDeliveryRemindLive())) {
            for (CommunicationContent one : featureFromLLM.getDeliveryRemindLive()) {
                Map<String, Object> item = new HashMap<>();
                item.put("event_time", one.getTs());
                item.put("event_content", CommonUtils.getOriginChatFromChatText(one.getCallId(), one.getAnswerText()));
                data.add(item);
                allTimeStr.add(one.getTs());
                count++;
            }
        }
        Collections.sort(data, new Comparator<Map<String, Object>>() {
            @Override
            public int compare(Map<String, Object> o1, Map<String, Object> o2) {
                String eventType1 = (String) o1.get("event_time");
                String eventType2 = (String) o2.get("event_time");
                return eventType2.compareTo(eventType1); // 字符串按字典序比较
            }
        });
        recordContent.setData(data);

        // 计算频次
        if (!CollectionUtils.isEmpty(allTimeStr)) {
            int days = CommonUtils.calculateDaysDifference(customerCreateTime);
            // 这里计算平均多少天一次
            double fre = (double) days / count;
            String formattedResult = String.format("%.1f", fre);
            customerFeature.getHandoverPeriod().getBasic().getRemindLiveFreq().setValue(Double.parseDouble(formattedResult));
        }

        customerFeature.getHandoverPeriod().getBasic().getRemindLiveFreq().setRecords(recordContent);
    }

    private void setDeliveryRemindplayback(CustomerFeatureFromLLM featureFromLLM,
                                       CustomerFeatureResponse customerFeature,
                                       LocalDateTime customerCreateTime) {
        // 提醒查看交付课直播：
        CustomerFeatureResponse.RecordContent recordContent = new CustomerFeatureResponse.RecordContent();
        List<CustomerFeatureResponse.RecordTitle> columns = new ArrayList<>();
        columns.add(new CustomerFeatureResponse.RecordTitle("event_time", "会话时间"));
        columns.add(new CustomerFeatureResponse.RecordTitle("event_content", "原文摘要"));
        recordContent.setColumns(columns);

        List<Map<String, Object>> data = new ArrayList<>();
        List<String> allTimeStr = new ArrayList<>();
        int count = 0;
        //1、提醒查看交付课直播
        if (!CollectionUtils.isEmpty(featureFromLLM.getDeliveryRemindLive())) {
            for (CommunicationContent one : featureFromLLM.getDeliveryRemindLive()) {
                Map<String, Object> item = new HashMap<>();
                item.put("event_time", one.getTs());
                item.put("event_content", CommonUtils.getOriginChatFromChatText(one.getCallId(), one.getAnswerText()));
                data.add(item);
                allTimeStr.add(one.getTs());
                count++;
            }
        }
        Collections.sort(data, new Comparator<Map<String, Object>>() {
            @Override
            public int compare(Map<String, Object> o1, Map<String, Object> o2) {
                String eventType1 = (String) o1.get("event_time");
                String eventType2 = (String) o2.get("event_time");
                return eventType2.compareTo(eventType1); // 字符串按字典序比较
            }
        });
        recordContent.setData(data);

        // 计算频次
        if (!CollectionUtils.isEmpty(allTimeStr)) {
            int days = CommonUtils.calculateDaysDifference(customerCreateTime);
            // 这里计算平均多少天一次
            double fre = (double) days / count;
            String formattedResult = String.format("%.1f", fre);
            customerFeature.getHandoverPeriod().getBasic().getRemindLiveFreq().setValue(Double.parseDouble(formattedResult));
        }

        customerFeature.getHandoverPeriod().getBasic().getRemindLiveFreq().setRecords(recordContent);
    }

    private void setDeliveryPeriod(CustomerBase customerBase,
                                   CustomerFeatureFromLLM featureFromLLM,
                                   CustomerFeatureResponse customerFeature,
                                   LocalDateTime customerCreateTime) {
        // 设置沟通频次
        customerFeature.getDeliveryPeriod().getBasic().getCommunicationFreq().setValue(customerBase.getCommunicationRounds());
        // 交付课直播
        setDeliveryRemindLive(featureFromLLM, customerFeature, customerBase.getCreateTime());
        // 交付课回放
        setDeliveryRemindplayback(featureFromLLM, customerFeature, customerBase.getCreateTime());

        if (Objects.nonNull(customerFeature.getBasic().getHasTime()) &&
                Objects.nonNull(customerFeature.getBasic().getHasTime().getCustomerConclusion()) &&
                Objects.nonNull(customerFeature.getBasic().getHasTime().getCustomerConclusion().getModelRecord())) {
            CustomerFeatureResponse.ChatContent hasTime = new CustomerFeatureResponse.ChatContent();
            hasTime.setValue(customerFeature.getBasic().getHasTime().getCustomerConclusion().getModelRecord().toString());
            hasTime.setOriginChat(customerFeature.getBasic().getHasTime().getCustomerConclusion().getOriginChat());
            customerFeature.getWarmth().setCustomerCourse(hasTime);
        }


    }
}
