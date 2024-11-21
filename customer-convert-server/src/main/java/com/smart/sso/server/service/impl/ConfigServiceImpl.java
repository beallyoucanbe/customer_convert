package com.smart.sso.server.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.smart.sso.server.enums.ConfigTypeEnum;
import com.smart.sso.server.primary.mapper.ConfigMapper;
import com.smart.sso.server.model.Config;
import com.smart.sso.server.model.QiweiApplicationConfig;
import com.smart.sso.server.model.dto.LeadMemberRequest;
import com.smart.sso.server.service.ConfigService;
import com.smart.sso.server.util.JsonUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Service
@Slf4j
public class ConfigServiceImpl implements ConfigService {
    @Autowired
    private ConfigMapper configMapper;

    @Override
    public List<String> getStaffIds() {
        QueryWrapper<Config> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("type", ConfigTypeEnum.COMMON.getValue());
        queryWrapper.eq("name", ConfigTypeEnum.STAFF_ID_NEED_PROCESS.getValue());
        Config config = configMapper.selectOne(queryWrapper);
        if (Objects.isNull(config)) {
            log.error("没有配置参加活动的销售名单，请先配置");
            throw new RuntimeException("没有配置参加活动的销售名单，请先配置");
        }
        return JsonUtil.readValue(config.getValue(), new TypeReference<List<String>>() {
        });
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
    public List<LeadMemberRequest> addLeaderMember(List<LeadMemberRequest> members, boolean overwrite) {
        QueryWrapper<Config> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("type", ConfigTypeEnum.COMMON.getValue());
        queryWrapper.eq("name", ConfigTypeEnum.LEADER.getValue());
        Config config = configMapper.selectOne(queryWrapper);
        if (Objects.isNull(config)) {
            // 覆盖写入 或者 第一次配置，直接写入
            Config newConfig = new Config();
            newConfig.setType(ConfigTypeEnum.COMMON.getValue());
            newConfig.setName(ConfigTypeEnum.LEADER.getValue());
            newConfig.setValue(JsonUtil.serialize(members));
            configMapper.insert(newConfig);
        } else if (overwrite) {
            config.setValue(JsonUtil.serialize(members));
            configMapper.updateById(config);
        }
        return JsonUtil.readValue(configMapper.selectOne(queryWrapper).getValue(), new TypeReference<List<LeadMemberRequest>>() {
        });
    }
}
