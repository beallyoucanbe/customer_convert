package com.smart.sso.server.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.smart.sso.server.mapper.CustomerFeatureMapper;
import com.smart.sso.server.mapper.CustomerInfoMapper;
import com.smart.sso.server.mapper.CustomerSummaryMapper;
import com.smart.sso.server.model.CustomerFeature;
import com.smart.sso.server.model.CustomerInfo;
import com.smart.sso.server.model.CustomerSummary;
import com.smart.sso.server.model.FeatureContent;
import com.smart.sso.server.model.VO.CustomerListVO;
import com.smart.sso.server.model.VO.CustomerProfile;
import com.smart.sso.server.model.dto.CustomerFeatureResponse;
import com.smart.sso.server.model.dto.CustomerInfoListRequest;
import com.smart.sso.server.model.dto.CustomerInfoListResponse;
import com.smart.sso.server.model.dto.CustomerProcessSummaryResponse;
import com.smart.sso.server.service.CustomerInfoService;
import com.smart.sso.server.util.CommonUtils;
import com.smart.sso.server.util.DateUtil;
import com.smart.sso.server.util.JsonUtil;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
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
            queryWrapper.eq("current_campaign", params.getCurrentCampaign());
        }

        String sortOrder = params.getSortBy();
        boolean isAsc = "asc".equalsIgnoreCase(params.getOrder());
        if ("conversion_rate".equals(sortOrder)) {
            queryWrapper.last("ORDER BY FIELD(conversion_rate, 'low', 'medium', 'high') " + (isAsc ? "ASC" : "DESC"));
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
        return convert2CustomerProfile(customerInfo);
    }

    @Override
    public CustomerFeatureResponse queryCustomerFeatureById(String id) {
        CustomerFeature customerFeature = customerFeatureMapper.selectById(id);
        return convert2CustomerFeatureResponse(customerFeature);
    }

    @Override
    public CustomerProcessSummaryResponse queryCustomerProcessSummaryById(String id) {
        CustomerSummary customerSummary = customerSummaryMapper.selectById(id);
        return convert2CustomerProcessSummaryResponse(customerSummary);
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
            customerInfo.setCustomerName(UUID.randomUUID().toString().substring(0, 10).replaceAll("-", ""));
            customerInfo.setOwnerName(getRandomElement(owners, random).toString());
            customerInfo.setCurrentCampaign(getRandomElement(campaigns, random).toString());
            customerInfo.setConversionRate(getRandomElement(conversionRates, random).toString());
            customerInfo.setCustomerStage((Integer) getRandomElement(customerStages, random));
            customerInfo.setCommunicationRounds(random.nextInt(10) + 1);
            customerInfo.setLastCommunicationDate(DateUtil.getDateObj());
            customerInfo.setTotalDuration((long) (random.nextInt(9999) + 1));
            customerInfoMapper.insert(customerInfo);
            // 同时写入客户特征表
            insetCustomerFeature(customerInfo.getId());
            // 同时写入客户特征表
            insetCustomerSummary(customerInfo.getId());
        }
    }

    @Override
    public void insetCustomerFeature(String id) {
        CustomerFeature customerFeature = new CustomerFeature();

        String[] useFrequency = {"high", "medium", "low"};

        Random random = new Random();
        customerFeature.setId(id);
        customerFeature.setCustomerLifecycle(UUID.randomUUID().toString().substring(0, 6).replaceAll("-", ""));
        customerFeature.setHasComputerVersion(random.nextBoolean());
        customerFeature.setClassLength((long) (random.nextInt(9999) + 1));
        customerFeature.setClassCount(random.nextInt(10) + 1);
        customerFeature.setPasswordEarnest(UUID.randomUUID().toString().substring(0, 10).replaceAll("-", ""));
        customerFeature.setUsageFrequency(getRandomElement(useFrequency, random).toString());
        customerFeatureMapper.insert(customerFeature);
    }

    @Override
    public void insetCustomerSummary(String id) {
        CustomerSummary customerSummary = new CustomerSummary();
        customerSummary.setId(id);
        CustomerProcessSummaryResponse customerProcessSummaryResponse = JsonUtil.readValue(JsonUtil.summary_string, new TypeReference<CustomerProcessSummaryResponse>() {
        });
        customerSummary.setSummaryAdvantage(customerProcessSummaryResponse.getSummary().getAdvantage());
        customerSummary.setSummaryQuestions(customerProcessSummaryResponse.getSummary().getQuestions());
        customerSummary.setInfoExplanation(customerProcessSummaryResponse.getInfoExplanation());

        customerSummary.setApprovalAnalysisMethod(customerProcessSummaryResponse.getApprovalAnalysis().getMethod());
        customerSummary.setApprovalAnalysisIssue(customerProcessSummaryResponse.getApprovalAnalysis().getIssue());
        customerSummary.setApprovalAnalysisPrice(customerProcessSummaryResponse.getApprovalAnalysis().getPrice());
        customerSummary.setApprovalAnalysisValue(customerProcessSummaryResponse.getApprovalAnalysis().getValue());
        customerSummary.setApprovalAnalysisPurchase(customerProcessSummaryResponse.getApprovalAnalysis().getPurchase());

        customerSummaryMapper.insert(customerSummary);
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
        // Basic 基本信息
        CustomerFeatureResponse.Basic basic = new CustomerFeatureResponse.Basic();
        basic.setFundsVolume(convertByOverwrite(customerFeature.getFundsVolumeModel(), customerFeature.getFundsVolumeSales()));
        basic.setProfitLossSituation(convertByOverwrite(customerFeature.getProfitLossSituationModel(), customerFeature.getProfitLossSituationSales()));
        basic.setEarningDesire(convertByOverwrite(customerFeature.getEarningDesireModel(), customerFeature.getEarningDesireSales()));
        basic.setCourseTeacherApproval(convertByOverwrite(customerFeature.getCourseTeacherApprovalModel(), customerFeature.getCourseTeacherApprovalSales()));
        customerFeatureResponse.setBasic(basic);

        // TradingMethod 客户自己的交易方法
        CustomerFeatureResponse.TradingMethod tradingMethod = new CustomerFeatureResponse.TradingMethod();
        tradingMethod.setCurrentStocks(convertByAppend(customerFeature.getCurrentStocksModel(), customerFeature.getCurrentStocksSales()));
        tradingMethod.setStockPurchaseReason(convertByAppend(customerFeature.getStockPurchaseReasonModel(), customerFeature.getStockPurchaseReasonSales()));
        tradingMethod.setTradeTimingDecision(convertByAppend(customerFeature.getTradeTimingDecisionModel(), customerFeature.getTradeTimingDecisionSales()));
        tradingMethod.setTradingStyle(convertByOverwrite(customerFeature.getTradingStyleModel(), customerFeature.getTradingStyleSales()));
        tradingMethod.setStockMarketAge(convertByOverwrite(customerFeature.getStockMarketAgeModel(), customerFeature.getStockMarketAgeSales()));
        tradingMethod.setLearningAbility(convertByOverwrite(customerFeature.getLearningAbilityModel(), customerFeature.getLearningAbilitySales()));
        customerFeatureResponse.setTradingMethod(tradingMethod);

        // Recognition 客户认可度
        CustomerFeatureResponse.Recognition recognition = new CustomerFeatureResponse.Recognition();
        recognition.setSoftwareFunctionClarity(convertByOverwrite(customerFeature.getSoftwareFunctionClarityModel(), customerFeature.getSoftwareFunctionClaritySales()));
        recognition.setStockSelectionMethod(convertByOverwrite(customerFeature.getStockSelectionMethodModel(), customerFeature.getStockSelectionMethodSales()));
        recognition.setSelfIssueRecognition(convertByOverwrite(customerFeature.getSelfIssueRecognitionModel(), customerFeature.getSelfIssueRecognitionSales()));
        recognition.setSoftwareValueApproval(convertByOverwrite(customerFeature.getSoftwareValueApprovalModel(), customerFeature.getSoftwareValueApprovalSales()));
        recognition.setSoftwarePurchaseAttitude(convertByOverwrite(customerFeature.getSoftwarePurchaseAttitudeModel(), customerFeature.getSoftwarePurchaseAttitudeSales()));
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

        CustomerProcessSummaryResponse.ProcessInfoExplanation infoExplanation = customerSummary.getInfoExplanation();
        customerSummaryResponse.setInfoExplanation(infoExplanation);

        CustomerProcessSummaryResponse.ProcessApprovalAnalysis approvalAnalysis = new CustomerProcessSummaryResponse.ProcessApprovalAnalysis();
        approvalAnalysis.setMethod(customerSummary.getApprovalAnalysisMethod());
        approvalAnalysis.setIssue(customerSummary.getApprovalAnalysisIssue());
        approvalAnalysis.setPrice(customerSummary.getApprovalAnalysisPrice());
        approvalAnalysis.setPurchase(customerSummary.getApprovalAnalysisPurchase());
        approvalAnalysis.setPrice(customerSummary.getApprovalAnalysisPrice());
        customerSummaryResponse.setApprovalAnalysis(approvalAnalysis);

        return customerSummaryResponse;
    }


    private CustomerFeatureResponse.Feature convertByOverwrite(List<FeatureContent> featureContentByModel, List<FeatureContent> featureContentBySales) {
        CustomerFeatureResponse.Feature featureVO = new CustomerFeatureResponse.Feature();
        // 多通电话覆盖+规则加工
        featureVO.setModelRecord(CollectionUtils.isEmpty(featureContentByModel) ? null : featureContentByModel.get(featureContentByModel.size() - 1).getAnswer());
        featureVO.setSalesRecord(CollectionUtils.isEmpty(featureContentBySales) ? null : featureContentBySales.get(featureContentBySales.size() - 1).getAnswer());
        //“已询问”有三个值：“是”、“否”、“不需要”。
        // “是”代表模型提取出了销售有询问，“否”代表模型提取出了销售没询问，“不需要”代表“客户情况（模型记录）或（销售补充）”有值且销售没询问（即客户主动说了，销售不需要询问了）
        return featureVO;
    }

    private CustomerFeatureResponse.Feature convertByAppend(List<FeatureContent> featureContentByModel, List<FeatureContent> featureContentBySales) {
        CustomerFeatureResponse.Feature featureVO = new CustomerFeatureResponse.Feature();
        // 多通电话追加+规则加工
         List<String> modelRecord =  new ArrayList<>();
         if (!CollectionUtils.isEmpty(featureContentByModel)) {
             ListIterator<FeatureContent> iterator = featureContentByModel.listIterator(featureContentByModel.size());
             while (iterator.hasPrevious()) {
                 FeatureContent item = iterator.previous();
                 modelRecord.add(item.getAnswer());
             }
         }

        List<String> sailRecord =  new ArrayList<>();
        if (!CollectionUtils.isEmpty(featureContentBySales)) {
            ListIterator<FeatureContent> iterator = featureContentBySales.listIterator(featureContentBySales.size());
            while (iterator.hasPrevious()) {
                FeatureContent item = iterator.previous();
                sailRecord.add(item.getAnswer());
            }
        }

        featureVO.setModelRecord(CollectionUtils.isEmpty(modelRecord) ? null : JsonUtil.serialize(modelRecord));
        featureVO.setSalesRecord(CollectionUtils.isEmpty(sailRecord) ? null : JsonUtil.serialize(sailRecord));
        //“已询问”有三个值：“是”、“否”、“不需要”。
        // “是”代表模型提取出了销售有询问，“否”代表模型提取出了销售没询问，“不需要”代表“客户情况（模型记录）或（销售补充）”有值且销售没询问（即客户主动说了，销售不需要询问了）
        return featureVO;
    }

}
