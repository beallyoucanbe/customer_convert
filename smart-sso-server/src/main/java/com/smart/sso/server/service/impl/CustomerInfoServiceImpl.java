package com.smart.sso.server.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smart.sso.server.mapper.CustomerInfoMapper;
import com.smart.sso.server.model.CustomerInfo;
import com.smart.sso.server.model.dto.CustomerInfoListRequest;
import com.smart.sso.server.model.dto.PageListResponse;
import com.smart.sso.server.service.CustomerInfoService;
import com.smart.sso.server.util.CommonUtils;
import com.smart.sso.server.util.DateUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.UUID;

@Service
public class CustomerInfoServiceImpl implements CustomerInfoService {

    @Autowired
    private CustomerInfoMapper customerInfoMapper;
    @Override
    public PageListResponse<CustomerInfo> queryCustomerInfoList(CustomerInfoListRequest params) {
        Page<CustomerInfo> selectPage = new Page<>(params.getPage(), params.getLimit());
        QueryWrapper<CustomerInfo> queryWrapper = new QueryWrapper<>();

        if (!StringUtils.isEmpty(params.getName())){
            queryWrapper.like("name", params.getName());
        }
        Page<CustomerInfo> resultPage = customerInfoMapper.selectPage(selectPage, queryWrapper);

        return null;
    }

    @Override
    public void insetCustomerInfoList() {

        for (int i = 0; i < 10; i++) {
            CustomerInfo customerInfo = new CustomerInfo();
            customerInfo.setId(CommonUtils.generatePrimaryKey());
            customerInfo.setName(UUID.randomUUID().toString().substring(0, 10));
            customerInfo.setOwner(UUID.randomUUID().toString().substring(0, 10));
            customerInfo.setCurrentCampaign("123");
            customerInfo.setConversionRate("high");
            customerInfo.setCustomerStage(1);
            customerInfo.setCommunicationRounds(2);
            customerInfo.setLastCommunicationDate(DateUtil.getDateObj());
            customerInfo.setTotalDuration(123.0);
            customerInfoMapper.insert(customerInfo);
        }
    }
}
