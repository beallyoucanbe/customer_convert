package com.smart.sso.server.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smart.sso.server.mapper.CustomerInfoMapper;
import com.smart.sso.server.model.CustomerInfo;
import com.smart.sso.server.model.VO.CustomerListVO;
import com.smart.sso.server.model.VO.CustomerProfile;
import com.smart.sso.server.model.dto.CustomerInfoListRequest;
import com.smart.sso.server.model.dto.PageListResponse;
import com.smart.sso.server.service.CustomerInfoService;
import com.smart.sso.server.util.CommonUtils;
import com.smart.sso.server.util.DateUtil;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class CustomerInfoServiceImpl implements CustomerInfoService {

    @Autowired
    private CustomerInfoMapper customerInfoMapper;

    @Override
    public PageListResponse<CustomerListVO> queryCustomerInfoList(CustomerInfoListRequest params) {
        Page<CustomerInfo> selectPage = new Page<>(params.getPage(), params.getLimit());
        QueryWrapper<CustomerInfo> queryWrapper = new QueryWrapper<>();

        if (!StringUtils.isEmpty(params.getName())) {
            queryWrapper.like("name", params.getName());
        }
        if (!StringUtils.isEmpty(params.getOwner())) {
            queryWrapper.like("owner", params.getName());
        }
        if (!StringUtils.isEmpty(params.getConversionRate())) {
            queryWrapper.eq("conversion_rate", params.getConversionRate());
        }
        if (!StringUtils.isEmpty(params.getCurrentCampaign())) {
            queryWrapper.eq("current_campaign", params.getCurrentCampaign());
        }
        queryWrapper.orderBy(Boolean.TRUE, "asc".equals(params.getOrder()), params.getSortBy());
        Page<CustomerInfo> resultPage = customerInfoMapper.selectPage(selectPage, queryWrapper);
        PageListResponse<CustomerListVO> result = new PageListResponse<>();
        result.setTotal(resultPage.getTotal());
        result.setLimit(params.getLimit());
        result.setOffset(params.getPage());
        result.setData(convert(resultPage.getRecords()));
        return result;
    }

    @Override
    public CustomerProfile queryCustomerById(String id) {
        CustomerInfo customerInfo =  customerInfoMapper.selectById(id);
        return convert2CustomerProfile(customerInfo);
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
        }
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
        CustomerProfile customerProfile = new CustomerProfile();
        BeanUtils.copyProperties(customerInfo, customerProfile);
        return customerProfile;
    }

}
