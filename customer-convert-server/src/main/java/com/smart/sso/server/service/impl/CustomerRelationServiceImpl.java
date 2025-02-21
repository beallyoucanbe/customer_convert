package com.smart.sso.server.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.smart.sso.server.model.CustomerInfo;
import com.smart.sso.server.primary.mapper.CustomerInfoMapper;
import com.smart.sso.server.service.CustomerRelationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class CustomerRelationServiceImpl implements CustomerRelationService {
    @Autowired
    private CustomerInfoMapper customerInfoMapper;
    @Override
    public CustomerInfo getByActivityAndCustomer(String customerId, String ownerId, String activityId) {
        QueryWrapper<CustomerInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("sales_id", ownerId);
        queryWrapper.eq("customer_id", Long.parseLong(customerId));
        queryWrapper.eq("activity_id", activityId);
        return customerInfoMapper.selectOne(queryWrapper);
    }

    @Override
    public List<CustomerInfo> getByActivity(String activityId) {
        QueryWrapper<CustomerInfo> queryWrapper = new QueryWrapper<>();
        return customerInfoMapper.selectList(queryWrapper);
    }

    @Override
    public List<CustomerInfo> getByActivityAndSigned(String activityId) {
        QueryWrapper<CustomerInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("customer_purchase_status", 1);
        queryWrapper.eq("activity_id", activityId);
        return customerInfoMapper.selectList(queryWrapper);
    }
}
