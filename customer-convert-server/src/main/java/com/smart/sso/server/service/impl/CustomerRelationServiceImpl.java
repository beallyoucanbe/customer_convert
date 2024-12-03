package com.smart.sso.server.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.smart.sso.server.model.CustomerRelation;
import com.smart.sso.server.primary.mapper.CustomerRelationMapper;
import com.smart.sso.server.service.CustomerRelationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class CustomerRelationServiceImpl implements CustomerRelationService {
    @Autowired
    private CustomerRelationMapper customerRelationMapper;
    @Override
    public CustomerRelation getByActivityAndCustomer(String customerId, String ownerId, String activityId) {
        QueryWrapper<CustomerRelation> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("owner_id", ownerId);
        queryWrapper.eq("customer_id", Long.parseLong(customerId));
        queryWrapper.eq("activity_id", activityId);
        return customerRelationMapper.selectOne(queryWrapper);
    }

    @Override
    public List<CustomerRelation> getByActivityAndUpdateTime(String activityId, LocalDateTime dateTime) {
        QueryWrapper<CustomerRelation> queryWrapper = new QueryWrapper<>();
        queryWrapper.gt("update_time", dateTime);
        queryWrapper.eq("activity_id", activityId);
        return customerRelationMapper.selectList(queryWrapper);
    }

    @Override
    public List<CustomerRelation> getByActivityAndSigned(String activityId) {
        QueryWrapper<CustomerRelation> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("customer_signed", 1);
        queryWrapper.eq("activity_id", activityId);
        return customerRelationMapper.selectList(queryWrapper);
    }
}
