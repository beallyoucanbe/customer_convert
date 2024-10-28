package com.smart.sso.server.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.smart.sso.server.mapper.TelephoneRecordMapper;
import com.smart.sso.server.model.CommunicationContent;
import com.smart.sso.server.model.CustomerFeatureFromLLM;
import com.smart.sso.server.model.TelephoneRecord;
import com.smart.sso.server.service.TelephoneRecordService;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Objects;

public class TelephoneRecordServiceImpl implements TelephoneRecordService {
    private TelephoneRecordMapper recordMapper;

    @Override
    public void getCustomerFeatureFromLLM(String customerId, String currentCampaign) {

        CustomerFeatureFromLLM customerFeatureFromLLM = new CustomerFeatureFromLLM();

        QueryWrapper<TelephoneRecord> queryWrapper = new QueryWrapper<>();
        // 按照沟通时间倒序排列
        queryWrapper.eq("customer_id", customerId);
        queryWrapper.eq("current_campaign", currentCampaign);
        queryWrapper.orderBy(false, false, "communication_time");
        List<TelephoneRecord> records = recordMapper.selectList(queryWrapper);
        // 对该客户下的所有的通话记录进行总结
        for (TelephoneRecord record : records) {
            //客户的资金体量
            if (!CollectionUtils.isEmpty(record.getFundsVolume()) && Objects.nonNull(customerFeatureFromLLM.getFundsVolume())){
                CommunicationContent communicationContent = record.getFundsVolume().get(0);

            }
            //客户的赚钱欲望
            //客户对软件功能的清晰度
            //客户对选股方法的认可度
            //客户对自身问题及影响的认可度
            //客户对软件价值的认可度
            //客户对购买软件的态度
            //客户当前持仓或关注的股票
            //客户为什么买这些股票
            //客户怎么决定的买卖这些股票的时机
            //客户的交易风格
            //客户的股龄
            //客户的学习能力
            //业务员有结合客户的股票举例
            //业务员有基于客户交易风格做针对性的功能介绍
            //业务员有点评客户的选股方法
            //业务员有点评客户的选股时机
            //业务员有对客户的问题做量化放大
            //业务员有对软件的价值做量化放大
        }


    }
}
