package com.smart.sso.server.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.smart.sso.server.enums.ConfigTypeEnum;
import com.smart.sso.server.model.ActivityInfo;
import com.smart.sso.server.model.dto.LeadMember;
import com.smart.sso.server.primary.mapper.ConfigMapper;
import com.smart.sso.server.model.Config;
import com.smart.sso.server.model.QiweiApplicationConfig;
import com.smart.sso.server.service.ConfigService;
import com.smart.sso.server.util.JsonUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.*;

@Service
@Slf4j
public class ConfigServiceImpl implements ConfigService {
    @Autowired
    private ConfigMapper configMapper;

    @Override
    public List<String> getStaffIds() {
        QueryWrapper<Config> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("type", ConfigTypeEnum.COMMON.getValue());
        queryWrapper.eq("name", ConfigTypeEnum.LEADER_MEMBERS.getValue());
        Config config = configMapper.selectOne(queryWrapper);
        if (Objects.isNull(config)) {
            log.error("没有配置参加活动的销售名单，请先配置");
            throw new RuntimeException("没有配置参加活动的销售名单，请先配置");
        }
        List<LeadMember> leadMembers = JsonUtil.readValue(config.getValue(), new TypeReference<List<LeadMember>>() {
        });
        List<String> staffIds = new ArrayList<>();
        for (LeadMember item : leadMembers) {
            for (LeadMember.Team team : item.getTeams()) {
                staffIds.addAll(team.getMembers().keySet());
            }
        }
        return staffIds;
    }

    @Override
    public String getStaffAreaRobotUrl(String memberId) {
        QueryWrapper<Config> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("type", ConfigTypeEnum.COMMON.getValue());
        queryWrapper.eq("name", ConfigTypeEnum.LEADER_MEMBERS.getValue());
        Config config = configMapper.selectOne(queryWrapper);
        if (Objects.isNull(config)) {
            log.error("没有配置参加活动的销售名单，请先配置");
            throw new RuntimeException("没有配置参加活动的销售名单，请先配置");
        }
        List<LeadMember> leadMembers = JsonUtil.readValue(config.getValue(), new TypeReference<List<LeadMember>>() {
        });
        String area = null;
        for (LeadMember item : leadMembers) {
            for (LeadMember.Team team : item.getTeams()) {
                if (team.getMembers().keySet().contains(memberId)){
                    area = item.getArea();
                    break;
                }
            }
        }
        Map<String, String> messageUrl = getRobotMessageUrl();
        if (!StringUtils.isEmpty(area)){
            return messageUrl.get(area);
        }
        return null;
    }

    @Override
    public QiweiApplicationConfig getQiweiApplicationConfig() {
        QueryWrapper<Config> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("type", ConfigTypeEnum.COMMON.getValue());
        queryWrapper.eq("name", ConfigTypeEnum.QIWEI_APPLICATION_CONFIG.getValue());
        Config config = configMapper.selectOne(queryWrapper);
        if (Objects.isNull(config)) {
            log.error("没有企微自建应用的配置，请先配置");
            throw new RuntimeException("没有企微自建应用的配置，请先配置");
        }
        return JsonUtil.readValue(config.getValue(), new TypeReference<QiweiApplicationConfig>() {
        });
    }

    @Override
    public String getCurrentActivityId() {
        QueryWrapper<Config> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("type", ConfigTypeEnum.COMMON.getValue());
        queryWrapper.eq("name", ConfigTypeEnum.CURRENT_ACTIVITY_ID.getValue());
        Config config = configMapper.selectOne(queryWrapper);
        if (Objects.isNull(config)) {
            log.error("没有配置参加活动的销售名单，请先配置");
            throw new RuntimeException("没有配置参加活动的销售名单，请先配置");
        }
        return config.getValue();
    }

    @Override
    public Map<String, String> getActivityIdNames() {
        QueryWrapper<Config> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("type", ConfigTypeEnum.COMMON.getValue());
        queryWrapper.eq("name", ConfigTypeEnum.ACTIVITY_ID_NAMES.getValue());
        Config config = configMapper.selectOne(queryWrapper);
        if (Objects.isNull(config)) {
            log.error("没有配置参加活动的销售名单，请先配置");
            throw new RuntimeException("没有配置参加活动的销售名单，请先配置");
        }
        try {
            return JsonUtil.readValue(config.getValue(), new TypeReference<Map<String, String>>() {
            });
        } catch (Exception e) {
            log.error("获取活动id和活动name的对应关系失败，返回空", e);
            return new HashMap<>();
        }
    }

    @Override
    public Map<String, String> getRobotMessageUrl() {
        QueryWrapper<Config> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("type", ConfigTypeEnum.COMMON.getValue());
        queryWrapper.eq("name", ConfigTypeEnum.ROBOT_MESSAGE_URL.getValue());
        Config config = configMapper.selectOne(queryWrapper);
        if (Objects.isNull(config)) {
            log.error("没有配置机器人的发送地址，请先配置");
            throw new RuntimeException("没有配置机器人的发送地址，请先配置");
        }
        try {
            return JsonUtil.readValue(config.getValue(), new TypeReference<Map<String, String>>() {
            });
        } catch (Exception e) {
            log.error("获取配置机器人的发送地址，返回空", e);
            return new HashMap<>();
        }
    }

}
