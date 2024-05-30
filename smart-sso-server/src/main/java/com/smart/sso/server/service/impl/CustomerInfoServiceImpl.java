package com.smart.sso.server.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smart.sso.server.mapper.CustomerFeatureMapper;
import com.smart.sso.server.mapper.CustomerInfoMapper;
import com.smart.sso.server.model.CustomerFeature;
import com.smart.sso.server.model.CustomerInfo;
import com.smart.sso.server.model.FeatureContent;
import com.smart.sso.server.model.VO.CustomerListVO;
import com.smart.sso.server.model.VO.CustomerProfile;
import com.smart.sso.server.model.dto.CustomerFeatureResponse;
import com.smart.sso.server.model.dto.CustomerInfoListRequest;
import com.smart.sso.server.model.dto.CustomerInfoListResponse;
import com.smart.sso.server.service.CustomerInfoService;
import com.smart.sso.server.util.CommonUtils;
import com.smart.sso.server.util.DateUtil;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class CustomerInfoServiceImpl implements CustomerInfoService {

    @Autowired
    private CustomerInfoMapper customerInfoMapper;
    @Autowired
    private CustomerFeatureMapper customerFeatureMapper;

    @Override
    public CustomerInfoListResponse queryCustomerInfoList(CustomerInfoListRequest params) {
        Page<CustomerInfo> selectPage = new Page<>(params.getPage(), params.getLimit());
        QueryWrapper<CustomerInfo> queryWrapper = new QueryWrapper<>();

        if (!StringUtils.isEmpty(params.getName())) {
            queryWrapper.like("name", params.getName());
        }
        if (!StringUtils.isEmpty(params.getOwner())) {
            queryWrapper.like("owner", params.getOwner());
        }
        if (!StringUtils.isEmpty(params.getConversionRate())) {
            queryWrapper.eq("conversion_rate", params.getConversionRate());
        }
        if (!StringUtils.isEmpty(params.getCurrentCampaign())) {
            queryWrapper.eq("current_campaign", params.getCurrentCampaign());
        }
        queryWrapper.orderBy(Boolean.TRUE, "asc".equals(params.getOrder()), params.getSortBy());
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
        return convert2CustomerProfile(customerInfo);
    }

    @Override
    public CustomerFeatureResponse queryCustomerFeatureById(String id) {
        CustomerFeature customerFeature = customerFeatureMapper.selectById(id);
        return convert2CustomerFeatureResponse(customerFeature);
    }

    @Override
    public void insetCustomerInfoList() {
        // 定义客户名称
        String[] owners = {"张三", "李四", "王五", "tom", "gerge"};
        // 当前归属活动
        String[] campaigns = {"第一期活动", "第二期活动", "五月活动", "六月活动", "第三期活动"};
        // 转化概率
        String[] conversionRates = {"high", "medium", "low"};
        // 客户阶段
        Integer[] customerStages = {0, 1, 2, 3, 4};
        Random random = new Random();
        for (int i = 0; i < 100; i++) {
            CustomerInfo customerInfo = new CustomerInfo();
            customerInfo.setId(CommonUtils.generatePrimaryKey());
            customerInfo.setName(UUID.randomUUID().toString().substring(0, 10).replaceAll("-", ""));
            customerInfo.setOwner(getRandomElement(owners, random).toString());
            customerInfo.setCurrentCampaign(getRandomElement(campaigns, random).toString());
            customerInfo.setConversionRate(getRandomElement(conversionRates, random).toString());
            customerInfo.setCustomerStage((Integer) getRandomElement(customerStages, random));
            customerInfo.setCommunicationRounds(random.nextInt(10) + 1);
            customerInfo.setLastCommunicationDate(DateUtil.getDateObj());
            customerInfo.setTotalDuration((long) (random.nextInt(9999) + 1));
            customerInfoMapper.insert(customerInfo);
            // 同时写入客户特征表
            insetCustomerFeature(customerInfo.getId());
        }
    }

    @Override
    public void insetCustomerFeature(String id) {
        CustomerFeature customerFeature = new CustomerFeature();
        Random random = new Random();
        customerFeature.setId(id);
        customerFeature.setClassLength((long) (random.nextInt(9999) + 1));
        customerFeature.setClassCount(random.nextInt(10) + 1);

        FeatureContent contentWithNum = new FeatureContent(Boolean.TRUE, "12", "tell me");
        FeatureContent contentWithBoolean = new FeatureContent(Boolean.TRUE, "true", "这是一个测试的记录");
        FeatureContent contentWithString = new FeatureContent(Boolean.TRUE, "字符串测试", "另一个test");
        customerFeature.setFundsVolume(contentWithNum);
        customerFeature.setEarningDesire(contentWithBoolean);
        customerFeature.setSoftwareValueApproval(contentWithString);
        customerFeatureMapper.insert(customerFeature);
    }

    public Object getRandomElement(Object[] array, Random random) {
        int randomIndex = random.nextInt(array.length);
        return array[randomIndex];
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
        // Basic
        CustomerFeatureResponse.Basic basic = new CustomerFeatureResponse.Basic();
        basic.setFundsVolume(customerFeature.getFundsVolume());
        basic.setProfitLossSituation(customerFeature.getProfitLossSituation());
        basic.setEarningDesire(customerFeature.getEarningDesire());
        customerFeatureResponse.setBasic(basic);
        // TradingMethod
        CustomerFeatureResponse.TradingMethod tradingMethod = new CustomerFeatureResponse.TradingMethod();
        tradingMethod.setCurrentStocks(customerFeature.getCurrentStocks());
        tradingMethod.setStockPurchaseReason(customerFeature.getStockPurchaseReason());
        tradingMethod.setTradeTimingDecision(customerFeature.getTradeTimingDecision());
        tradingMethod.setTradingStyle(customerFeature.getTradingStyle());
        tradingMethod.setStockMarketAge(customerFeature.getStockMarketAge());
        tradingMethod.setLearningAbility(customerFeature.getLearningAbility());
        customerFeatureResponse.setTradingMethod(tradingMethod);
        // Recognition
        CustomerFeatureResponse.Recognition recognition = new CustomerFeatureResponse.Recognition();
        recognition.setCourseTeacherApproval(customerFeature.getCourseTeacherApproval());
        recognition.setSoftwareFunctionClarity(customerFeature.getSoftwareFunctionClarity());
        recognition.setStockSelectionMethod(customerFeature.getStockSelectionMethod());
        recognition.setSelfIssueRecognition(customerFeature.getSelfIssueRecognition());
        recognition.setSoftwareValueApproval(customerFeature.getSoftwareValueApproval());
        recognition.setSoftwarePurchaseAttitude(customerFeature.getSoftwarePurchaseAttitude());
        customerFeatureResponse.setRecognition(recognition);
        // Note
        customerFeatureResponse.setNote(customerFeature.getNote());

        return customerFeatureResponse;
    }
}
