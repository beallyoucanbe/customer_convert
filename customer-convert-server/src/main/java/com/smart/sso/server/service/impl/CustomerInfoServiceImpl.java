package com.smart.sso.server.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smart.sso.server.enums.*;
import com.smart.sso.server.primary.mapper.*;
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
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import static com.smart.sso.server.constant.AppConstant.SOURCEID_KEY_PREFIX;
import static com.smart.sso.server.enums.FundsVolumeEnum.*;
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

    private DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

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
        CustomerBase customerBase = customerBaseMapper.selectByCustomerId(customerId);
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
        CustomerBase customerBase = customerBaseMapper.selectByCustomerId(customerId);
        CustomerInfo customerInfo = customerRelationService.getByCustomer(customerBase.getCustomerId(),
                customerBase.getOwnerId());
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
        // 设置回复频率
        if (!CollectionUtils.isEmpty(featureFromLLM.getCustomerResponse())){
            int daysDifference = CommonUtils.calculateDaysDifference(customerBase.getCreateTime());
            customerFeature.getWarmth().setCustomerResponse((float) (featureFromLLM.getCustomerResponse().size()/daysDifference));
            customerFeature.getWarmth().setLatestTimeCustomerResponse( LocalDateTime.parse(featureFromLLM.getCustomerResponse().get(0).getTs(), formatter));
        }
        if (Objects.nonNull(customerFeature)) {
            if (Objects.nonNull(customerInfo)) {
                // 这里设置听课数据
                customerFeature.getWarmth().setClassAttendTimes_2(customerInfo.getTotalCourses_2_0());
                customerFeature.getWarmth().setClassAttendDuration_2(customerInfo.getTotalDuration_2_0() / 60);
                customerFeature.getWarmth().setClassAttendTimes_3(customerInfo.getTotalCourses_3_0());
                customerFeature.getWarmth().setClassAttendDuration_3(customerInfo.getTotalDuration_3_0() / 60);
            }
            customerFeature.setTradingMethod(Objects.isNull(summaryResponse) ? null : summaryResponse.getTradingMethod());
            customerFeature.setSummary(getProcessSummary(customerFeature, stageStatus));
        }
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
        if (Objects.isNull(customerFeature)
                || Objects.isNull(customerFeature.getWarmth().getFundsVolume().getCompareValue())) {
            return result;
        }
        Feature.CustomerConclusion conclusion = customerFeature.getWarmth().getFundsVolume();
        if (StringUtils.isEmpty(conclusion.getCompareValue())) {
            return result;
        }
        if (conclusion.getCompareValue().equals(GREAT_THIRTY_W.getValue())) {
            return "high";
        }
        if (conclusion.getCompareValue().equals(TWENTY_TO_THIRTY_W.getValue())) {
            return "medium";
        }
        if (conclusion.getCompareValue().equals(LESS_FIFTEEN_W.getValue())) {
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
            try {
                if (Objects.nonNull(customerFeature.getWarmth().getFundsVolume().getCompareValue()) &&
                        !customerFeature.getWarmth().getFundsVolume().getCompareValue().equals("无") &&
                        !customerFeature.getWarmth().getFundsVolume().getCompareValue().equals("null")) {
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

            // 客户认可老师
            try {
                if ((Boolean) customerFeature.getBasic().getTeacherApproval().getCustomerConclusion().getCompareValue()) {
                    stageStatus.setFunctionIntroduction(1);
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

        // 客户完成购买”，:CRM取回客户的购买状态值为“是”
        try {
            CustomerInfo customerInfo = customerRelationService.getByCustomer(customerBase.getCustomerId(),
                    customerBase.getOwnerId());
            if (Objects.nonNull(customerInfo) && Objects.nonNull(customerInfo.getIsPurchased_2_0())
                    && customerInfo.getIsPurchased_2_0() == 1) {
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
        CustomerBase customerBase = customerBaseMapper.selectByCustomerId(customerId);
        CustomerFeature customerFeature = customerFeatureMapper.selectById(customerBase.getId());
        if (Objects.isNull(customerFeature)) {
            customerFeature = new CustomerFeature();
            customerFeature.setId(customerBase.getId());
            customerFeatureMapper.insert(customerFeature);
        }

        if (Objects.nonNull(customerFeatureRequest.getWarmth())) {
            if (Objects.nonNull(customerFeatureRequest.getWarmth().getFundsVolume()) &&
                    (Objects.nonNull(customerFeatureRequest.getWarmth().getFundsVolume().getSalesRecord()) ||
                            Objects.nonNull(customerFeatureRequest.getWarmth().getFundsVolume().getSalesManualTag()))) {
                customerFeature.setFundsVolumeSales(new FeatureContentSales(customerFeatureRequest.getWarmth().getFundsVolume().getSalesRecord(),
                        customerFeatureRequest.getWarmth().getFundsVolume().getSalesManualTag(), DateUtil.getCurrentDateTime()));
            }
            if (Objects.nonNull(customerFeatureRequest.getWarmth().getStockPosition()) &&
                    (Objects.nonNull(customerFeatureRequest.getWarmth().getStockPosition().getSalesRecord()) ||
                            Objects.nonNull(customerFeatureRequest.getWarmth().getStockPosition().getSalesManualTag()))) {
                customerFeature.setEarningDesireSales(new FeatureContentSales(customerFeatureRequest.getWarmth().getStockPosition().getSalesRecord(),
                        customerFeatureRequest.getWarmth().getStockPosition().getSalesManualTag(), DateUtil.getCurrentDateTime()));
            }
        }

        if (Objects.nonNull(customerFeatureRequest.getBasic())) {
            if (Objects.nonNull(customerFeatureRequest.getBasic().getTeacherApproval()) &&
                    (Objects.nonNull(customerFeatureRequest.getBasic().getTeacherApproval().getCustomerConclusion().getSalesRecord()) ||
                            Objects.nonNull(customerFeatureRequest.getBasic().getTeacherApproval().getCustomerConclusion().getSalesManualTag()))) {
                customerFeature.setCurrentStocksSales(new FeatureContentSales(customerFeatureRequest.getBasic().getTeacherApproval().getCustomerConclusion().getSalesRecord(),
                        customerFeatureRequest.getBasic().getTeacherApproval().getCustomerConclusion().getSalesManualTag(), DateUtil.getCurrentDateTime()));
            }
            if (Objects.nonNull(customerFeatureRequest.getBasic().getContinueFollowingStock()) &&
                    (Objects.nonNull(customerFeatureRequest.getBasic().getContinueFollowingStock().getCustomerConclusion().getSalesRecord()) ||
                            Objects.nonNull(customerFeatureRequest.getBasic().getContinueFollowingStock().getCustomerConclusion().getSalesManualTag()))) {
                customerFeature.setLearningAbilitySales(new FeatureContentSales(customerFeatureRequest.getBasic().getContinueFollowingStock().getCustomerConclusion().getSalesRecord(),
                        customerFeatureRequest.getBasic().getContinueFollowingStock().getCustomerConclusion().getSalesManualTag(), DateUtil.getCurrentDateTime()));
            }
            if (Objects.nonNull(customerFeatureRequest.getBasic().getSoftwareValueApproval()) &&
                    (Objects.nonNull(customerFeatureRequest.getBasic().getSoftwareValueApproval().getCustomerConclusion().getSalesRecord()) ||
                            Objects.nonNull(customerFeatureRequest.getBasic().getSoftwareValueApproval().getCustomerConclusion().getSalesManualTag()))) {
                customerFeature.setSoftwareValueApprovalSales(new FeatureContentSales(customerFeatureRequest.getBasic().getSoftwareValueApproval().getCustomerConclusion().getSalesRecord(),
                        customerFeatureRequest.getBasic().getSoftwareValueApproval().getCustomerConclusion().getSalesManualTag(), DateUtil.getCurrentDateTime()));
            }
            if (Objects.nonNull(customerFeatureRequest.getBasic().getSoftwarePurchaseAttitude()) &&
                    (Objects.nonNull(customerFeatureRequest.getBasic().getSoftwarePurchaseAttitude().getCustomerConclusion().getSalesRecord()) ||
                            Objects.nonNull(customerFeatureRequest.getBasic().getSoftwarePurchaseAttitude().getCustomerConclusion().getSalesManualTag()))) {
                customerFeature.setSoftwarePurchaseAttitudeSales(new FeatureContentSales(customerFeatureRequest.getBasic().getSoftwarePurchaseAttitude().getCustomerConclusion().getSalesRecord(),
                        customerFeatureRequest.getBasic().getSoftwarePurchaseAttitude().getCustomerConclusion().getSalesManualTag(), DateUtil.getCurrentDateTime()));
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
    public String getRedirectUrl(String customerId, String activeId, String ownerId, String owner, String from, String manager) {
        String urlFormatter = "https://newcmp.emoney.cn/chat/customer?customer_id=%s&activity_id=%s&embed=true";
        if (!StringUtils.isEmpty(ownerId)) {
            urlFormatter = urlFormatter + "&owner_id=" + ownerId;
        }
        if (!StringUtils.isEmpty(owner)) {
            urlFormatter = urlFormatter + "&owner=" + owner;
        }
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
            if (!equal(featureProfile.getWarmth().getFundsVolume())) {
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
        List<CustomerInfo> characterList = customerRelationService.getByActivity("");
        for (CustomerInfo info : characterList) {
            CustomerBase customerBase = customerBaseMapper.selectByCustomerId(info.getUserId());
            if (Objects.nonNull(customerBase)) {
                // 判断销售是否发生变更
                if (!Objects.equals(info.getSalesId(), customerBase.getOwnerId())) {
                    customerBaseMapper.updateSalesById(customerBase.getId(), info.getSalesId().toString(), info.getSalesName());
                }

                if (!Objects.equals(info.getAccessTime(), customerBase.getCreateTime())) {
                    customerBaseMapper.updateAccessTimeById(customerBase.getId(), info.getAccessTime());
                }
            } else {
                customerBase = new CustomerBase();
                customerBase.setId(CommonUtils.generatePrimaryKey());
                customerBase.setCustomerId(info.getUserId());
                customerBase.setOwnerId(info.getSalesId());
                customerBase.setCustomerName(info.getUserName());
                customerBase.setOwnerName(info.getSalesName());
                customerBase.setCreateTime(info.getAccessTime());
                customerBase.setActivityId(activityId);
                customerBaseMapper.insert(customerBase);
            }
        }
    }

    private boolean equal(Feature.CustomerConclusion feature) {
        if (Objects.isNull(feature.getSalesManualTag())) {
            return true;
        }
        if (Objects.isNull(feature.getModelRecord())) {
            return false;
        } else if (feature.getModelRecord().equals(feature.getSalesManualTag())) {
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
            customerListVO.setCustomerId(CommonUtils.encrypt(customerListVO.getCustomerId()));
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
        // 设置温度
        customerFeatureResponse.getWarmth().setFundsVolume(
                convertBaseFeatureByOverwrite(featureFromLLM.getFundsVolume(), Objects.isNull(featureFromSale) ? null : featureFromSale.getFundsVolumeSales(), FundsVolumeEnum.class, String.class).getCustomerConclusion());
        customerFeatureResponse.getWarmth().setStockPosition(
                convertBaseFeatureByOverwrite(featureFromLLM.getStockPosition(),  Objects.isNull(featureFromSale) ? null : featureFromSale.getEarningDesireSales(), StockPositonEnum.class, String.class).getCustomerConclusion());
        customerFeatureResponse.getWarmth().setTradingStyle(
                convertBaseFeatureByOverwrite(featureFromLLM.getTradingStyle(),  null, TradingStyleEnum.class, String.class).getCustomerConclusion()
        );
        customerFeatureResponse.getWarmth().setPurchaseSimilarProduct(
                convertBaseFeatureByOverwrite(featureFromLLM.getPurchaseSimilarProduct(),  null, null, Boolean.class).getCustomerConclusion());
        // 设置base
        customerFeatureResponse.getBasic().setMemberStocksBuy(
                convertBaseFeatureByOverwrite(featureFromLLM.getMemberStocksBuy(), null, null, Boolean.class)
        );
        customerFeatureResponse.getBasic().setMemberStocksPrice(
                convertBaseFeatureByOverwrite(featureFromLLM.getMemberStocksPrice(), null, PriceFluctuationsEnum.class, String.class)
        );
        customerFeatureResponse.getBasic().setWelfareStocksBuy(
                convertBaseFeatureByOverwrite(featureFromLLM.getWelfareStocksBuy(), null, null, Boolean.class)
        );
        customerFeatureResponse.getBasic().setWelfareStocksPrice(
                convertBaseFeatureByOverwrite(featureFromLLM.getWelfareStocksPrice(), null, PriceFluctuationsEnum.class, String.class)
        );
        customerFeatureResponse.getBasic().setConsultingPracticalClass(
                convertBaseFeatureByOverwrite(featureFromLLM.getConsultingPracticalClass(), null, null, Boolean.class)
        );
        // 老师的认可
        BaseFeature teacherAppr =
                convertBaseFeatureByOverwrite(featureFromLLM.getTeacherApproval(), Objects.isNull(featureFromSale) ? null : featureFromSale.getCurrentStocksSales(), null, Boolean.class);
        CourseTeacherFeature courseTeacherFeature = new CourseTeacherFeature(teacherAppr);
        if (Objects.nonNull(featureFromLLM.getTeacherApproval())
                && StringUtils.hasText(featureFromLLM.getTeacherApproval().getBuildText())) {
            courseTeacherFeature.setTeacherProfession(Boolean.TRUE);
            CommunicationContent teacher = featureFromLLM.getTeacherApproval();
            courseTeacherFeature.setTeacherProfessionChat(CommonUtils.getOriginChatFromChatText(StringUtils.isEmpty(teacher.getCallId()) ? featureFromLLM.getCallId() : teacher.getCallId(),
                    teacher.getBuildText()));
        }
        customerFeatureResponse.getBasic().setTeacherApproval(courseTeacherFeature);
        customerFeatureResponse.getBasic().setCustomerLearningFreq(getCustomerLearningFrequencyContent(featureFromLLM.getCustomerLearning()));
        customerFeatureResponse.getBasic().setContinueFollowingStock(convertBaseFeatureByOverwrite(featureFromLLM.getContinueFollowingStock(), Objects.isNull(featureFromSale) ? null : featureFromSale.getLearningAbilitySales(), null, Boolean.class));
        customerFeatureResponse.getBasic().setSoftwareValueApproval(convertBaseFeatureByOverwrite(featureFromLLM.getSoftwareValueApproval(), Objects.isNull(featureFromSale) ? null : featureFromSale.getSoftwareValueApprovalSales(), null, Boolean.class));
        customerFeatureResponse.getBasic().setSoftwarePurchaseAttitude(convertBaseFeatureByOverwrite(featureFromLLM.getSoftwarePurchaseAttitude(), Objects.isNull(featureFromSale) ? null : featureFromSale.getSoftwarePurchaseAttitudeSales(), null, Boolean.class));
        return customerFeatureResponse;
    }

    public CustomerProcessSummary convert2CustomerProcessSummaryResponse(CustomerFeatureFromLLM featureFromLLM, CustomerFeature featureFromSale) {
        if (Objects.isNull(featureFromLLM)) {
            return null;
        }
        CustomerProcessSummary customerSummaryResponse = new CustomerProcessSummary();
        CustomerProcessSummary.TradingMethod tradingMethod = new CustomerProcessSummary.TradingMethod();
        tradingMethod.setTradingStyle(convertTradeMethodFeatureByOverwrite(featureFromLLM.getTradingStyle(), Objects.isNull(featureFromSale) ? null : featureFromSale.getTradingStyleSales(), null, String.class));
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
                            "学会".equals(resultAnswer) ||
                            "愿意".equals(resultAnswer) ||
                            "买过".equals(resultAnswer) ||
                            "清晰".equals(resultAnswer)) {
                        customerConclusion.setModelRecord(Boolean.TRUE);
                    } else if ("否".equals(resultAnswer) ||
                            "无购买意向".equals(resultAnswer) ||
                            "不认可".equals(resultAnswer) ||
                            "不愿意".equals(resultAnswer) ||
                            "没学会".equals(resultAnswer) ||
                            "没买过".equals(resultAnswer) ||
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

    private CustomerFeatureResponse.ProcessSummary getProcessSummary(CustomerFeatureResponse customerFeature, CustomerStageStatus stageStatus) {
        CustomerFeatureResponse.ProcessSummary processSummary = new CustomerFeatureResponse.ProcessSummary();
        if (true) {
            return processSummary;
        }
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


            // 【交付期】客户认可老师
            // 优点：-【交付期】客户认可老师：“客户认可老师”的“客户结论”为“认可”
            // 缺点：-【交付期】客户尚未认可老师：“客户认可老师”的“客户结论”为“尚未认可”
            int teacherApproval = stageStatus.getFunctionIntroduction();
            if (teacherApproval == 1) {
                advantage.add("【交付期】客户认可老师");
            } else {
                questions.add(new CustomerFeatureResponse.Question("【交付期】客户尚未认可老师"));
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

    private CustomerFeatureResponse.FrequencyContent getCustomerLearningFrequencyContent(CommunicationFreqContent communicationFreqContent){
        CustomerFeatureResponse.FrequencyContent frequencyContent = new CustomerFeatureResponse.FrequencyContent();
        if (communicationFreqContent.getRemindCount() > 0 ) {
            // 频率计算规则 提醒次数/通话次数
            double fre = (double) (communicationFreqContent.getRemindCount() * 60) / communicationFreqContent.getCommunicationTime();
            frequencyContent.setValue(fre);

            // 提醒查看交付课直播：
            CustomerFeatureResponse.RecordContent recordContent = new CustomerFeatureResponse.RecordContent();
            List<CustomerFeatureResponse.RecordTitle> columns = new ArrayList<>();
            columns.add(new CustomerFeatureResponse.RecordTitle("communication_time", "会话时间"));
            columns.add(new CustomerFeatureResponse.RecordTitle("remind_count", "请教次数"));
            columns.add(new CustomerFeatureResponse.RecordTitle("content", "原文摘要"));
            recordContent.setColumns(columns);

            List<Map<String, Object>> data = new ArrayList<>();
            if (!CollectionUtils.isEmpty(communicationFreqContent.getFrequencyItemList())) {
                for (CommunicationFreqContent.FrequencyItem one : communicationFreqContent.getFrequencyItemList()) {
                    Map<String, Object> item = new HashMap<>();
                    item.put("communication_time", one.getCommunicationTime().format(formatter));
                    item.put("remind_count", one.getCount());
                    item.put("content", CommonUtils.getOriginChatFromChatText(one.getCallId(), one.getContent()));
                    data.add(item);
                }
            }
            recordContent.setData(data);
            frequencyContent.setRecords(recordContent);
        }
        return frequencyContent;
    }

}
