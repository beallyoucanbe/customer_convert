package com.smart.sso.server.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.smart.sso.server.model.CustomerDoubt;
import com.smart.sso.server.model.RecommenderQuestion;
import com.smart.sso.server.primary.mapper.CustomerDoubtMapper;
import com.smart.sso.server.service.RecommenderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
public class RecommenderServiceImpl implements RecommenderService {

    @Autowired
    private CustomerDoubtMapper customerDoubtMapper;

    @Override
    public RecommenderQuestion getRecommenderQuestions(String activityId, String questionType, LocalDateTime startTime, LocalDateTime endTime) {

        QueryWrapper<CustomerDoubt> queryWrapper = new QueryWrapper<>();
        if (!StringUtils.isEmpty(activityId)) {
            queryWrapper.like("customer_name", activityId);
        }
        if (!StringUtils.isEmpty(questionType)) {
            queryWrapper.like("owner_name", questionType);
        }
        if (Objects.nonNull(startTime)) {
            queryWrapper.eq("conversion_rate", startTime);
        }
        if (Objects.nonNull(endTime)) {
            queryWrapper.like("activity_name", endTime);
        }
        List<CustomerDoubt> resultPage = customerDoubtMapper.selectList(queryWrapper);

        return null;
    }
}
