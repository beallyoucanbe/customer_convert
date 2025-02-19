package com.smart.sso.server.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.smart.sso.server.constant.AppConstant;
import com.smart.sso.server.enums.ConfigTypeEnum;
import com.smart.sso.server.model.AccessTokenResponse;
import com.smart.sso.server.model.dto.LeadMember;
import com.smart.sso.server.primary.mapper.ConfigMapper;
import com.smart.sso.server.model.Config;
import com.smart.sso.server.model.QiweiApplicationConfig;
import com.smart.sso.server.service.ConfigService;
import com.smart.sso.server.util.JsonUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
@Slf4j
public class ConfigServiceImpl implements ConfigService {
    @Autowired
    private ConfigMapper configMapper;
    @Autowired
    private RestTemplate restTemplate;

    @Override
    public Map<String, List<String>> getStaffIds() {
        QueryWrapper<Config> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("type", ConfigTypeEnum.COMMON.getValue());
        queryWrapper.eq("name", ConfigTypeEnum.LEADER_MEMBERS.getValue());
        List<Config> configList = configMapper.selectList(queryWrapper);
        if (CollectionUtils.isEmpty(configList)) {
            log.error("没有配置参加活动的销售名单，请先配置");
            throw new RuntimeException("没有配置参加活动的销售名单，请先配置");
        }
        Map<String, List<String>> result = new HashMap<>();
        for (Config config : configList) {
            List<LeadMember> leadMembers = JsonUtil.readValue(config.getValue(), new TypeReference<List<LeadMember>>() {
            });
            List<String> staffIds = new ArrayList<>();
            for (LeadMember item : leadMembers) {
                staffIds.add(item.getId());
                for (LeadMember.Team team : item.getTeams()) {
                    staffIds.add(team.getId());
                    staffIds.addAll(team.getMembers().keySet());
                }
            }
            result.put(config.getTenantId(), staffIds);
        }
        return result;
    }

    @Override
    public Map<String, String> getStaffLeaderMap() {
        QueryWrapper<Config> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("type", ConfigTypeEnum.COMMON.getValue());
        queryWrapper.eq("name", ConfigTypeEnum.LEADER_MEMBERS.getValue());
        List<Config> configList = configMapper.selectList(queryWrapper);
        if (CollectionUtils.isEmpty(configList)) {
            log.error("没有配置参加活动的销售名单，请先配置");
            throw new RuntimeException("没有配置参加活动的销售名单，请先配置");
        }
        Map<String, String> result = new HashMap<>();
        for (Config config : configList) {
            List<LeadMember> leadMembers = JsonUtil.readValue(config.getValue(), new TypeReference<List<LeadMember>>() {
            });
            for (LeadMember item : leadMembers) {
                for (LeadMember.Team team : item.getTeams()) {
                    for (String memberId : team.getMembers().keySet()){
                        result.put(memberId, team.getId());
                    }
                }
            }
        }
        return result;
    }

    @Override
    public Map<String, String> getStaffManagerMap() {
        QueryWrapper<Config> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("type", ConfigTypeEnum.COMMON.getValue());
        queryWrapper.eq("name", ConfigTypeEnum.LEADER_MEMBERS.getValue());
        List<Config> configList = configMapper.selectList(queryWrapper);
        if (CollectionUtils.isEmpty(configList)) {
            log.error("没有配置参加活动的销售名单，请先配置");
            throw new RuntimeException("没有配置参加活动的销售名单，请先配置");
        }
        Map<String, String> result = new HashMap<>();
        for (Config config : configList) {
            List<LeadMember> leadMembers = JsonUtil.readValue(config.getValue(), new TypeReference<List<LeadMember>>() {
            });
            for (LeadMember item : leadMembers) {
                for (LeadMember.Team team : item.getTeams()) {
                    for (String memberId : team.getMembers().keySet()){
                        result.put(memberId, item.getId());
                    }
                }
            }
        }
        return result;
    }

    @Override
    public Map<String, String> getUserIdNamerMap() {
        QueryWrapper<Config> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("type", ConfigTypeEnum.COMMON.getValue());
        queryWrapper.eq("name", ConfigTypeEnum.LEADER_MEMBERS.getValue());
        List<Config> configList = configMapper.selectList(queryWrapper);
        if (CollectionUtils.isEmpty(configList)) {
            log.error("没有配置参加活动的销售名单，请先配置");
            throw new RuntimeException("没有配置参加活动的销售名单，请先配置");
        }
        Map<String, String> result = new HashMap<>();
        for (Config config : configList) {
            List<LeadMember> leadMembers = JsonUtil.readValue(config.getValue(), new TypeReference<List<LeadMember>>() {
            });
            for (LeadMember item : leadMembers) {
                result.put(item.getId(), item.getManager());
                for (LeadMember.Team team : item.getTeams()) {
                    result.put(team.getId(), team.getLeader());
                    result.putAll(team.getMembers());
                }
            }
        }
        return result;
    }

    @Override
    public Map<String, List<String>> getStaffIdsLeader() {
        QueryWrapper<Config> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("type", ConfigTypeEnum.COMMON.getValue());
        queryWrapper.eq("name", ConfigTypeEnum.LEADER_MEMBERS.getValue());
        List<Config> configList = configMapper.selectList(queryWrapper);
        if (CollectionUtils.isEmpty(configList)) {
            log.error("没有配置参加活动的销售名单，请先配置");
            throw new RuntimeException("没有配置参加活动的销售名单，请先配置");
        }
        Map<String, List<String>> result = new HashMap<>();
        for (Config config : configList) {
            List<LeadMember> leadMembers = JsonUtil.readValue(config.getValue(), new TypeReference<List<LeadMember>>() {
            });
            for (LeadMember item : leadMembers) {
                for (LeadMember.Team team : item.getTeams()) {
                    result.put(team.getId(), new ArrayList<>(team.getMembers().values()));
                }
            }
        }
        return result;
    }

    @Override
    public String getStaffAreaRobotUrl(String memberId) {
        QueryWrapper<Config> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("type", ConfigTypeEnum.COMMON.getValue());
        queryWrapper.eq("name", ConfigTypeEnum.LEADER_MEMBERS.getValue());
        List<Config> configList = configMapper.selectList(queryWrapper);
        if (CollectionUtils.isEmpty(configList)) {
            log.error("没有配置参加活动的销售名单，请先配置");
            throw new RuntimeException("没有配置参加活动的销售名单，请先配置");
        }
        String area = null;
        for (Config config : configList) {
            List<LeadMember> leadMembers = JsonUtil.readValue(config.getValue(), new TypeReference<List<LeadMember>>() {
            });
            for (LeadMember item : leadMembers) {
                for (LeadMember.Team team : item.getTeams()) {
                    if (team.getMembers().keySet().contains(memberId)) {
                        area = item.getArea();
                        break;
                    }
                }
            }
        }
        if (!StringUtils.isEmpty(area)) {
            return AppConstant.robotUrl.get(area);
        }
        return null;
    }

    @Override
    public Map<String, QiweiApplicationConfig> getQiweiApplicationConfig() {
        QueryWrapper<Config> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("type", ConfigTypeEnum.COMMON.getValue());
        queryWrapper.eq("name", ConfigTypeEnum.QIWEI_APPLICATION_CONFIG.getValue());
        List<Config> configList = configMapper.selectList(queryWrapper);
        if (CollectionUtils.isEmpty(configList)) {
            log.error("没有企微自建应用的配置，请先配置");
            throw new RuntimeException("没有企微自建应用的配置，请先配置");
        }
        Map<String, QiweiApplicationConfig> result = new HashMap<>();
        for (Config config : configList) {
            result.put(config.getTenantId(), JsonUtil.readValue(config.getValue(), new TypeReference<QiweiApplicationConfig>() {
            }));
        }
        return result;
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

    @Override
    public void refreshCustomerConfig() {
        // 更新员工信息
        Map<String, List<String>> staffIds = getStaffIds();
        Map<String, Set<String>> staffIdMap = new HashMap<>();
        for (Map.Entry<String, List<String>> entry : staffIds.entrySet()) {
            staffIdMap.put(entry.getKey(), new HashSet<>(entry.getValue()));
        }
        AppConstant.staffIdMap = staffIdMap;
        // 更新员工和主管和大区经理的关系
        AppConstant.staffLeaderMap = getStaffLeaderMap();
        AppConstant.staffManagerrMap = getStaffManagerMap();
        AppConstant.userIdNameMap = getUserIdNamerMap();

        // 更新应用信息
        AppConstant.qiweiApplicationConfigMap = getQiweiApplicationConfig();

        // 更新企微机器人发送url
        AppConstant.robotUrl = getRobotMessageUrl();

        // 更新token信息
        Map<String, String> tokenMap = new HashMap<>();
        for (Map.Entry<String, QiweiApplicationConfig> entry : AppConstant.qiweiApplicationConfigMap.entrySet()) {
            String url = String.format(AppConstant.GET_SECRET_URL, entry.getValue().getCorpId(),  entry.getValue().getCorpSecret());
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            // 处理响应
            try {
                if (response.getStatusCode() == HttpStatus.OK) {
                    log.error("获取access token 结果" + response.getBody());
                    AccessTokenResponse accessTokenResponse = JsonUtil.readValue(response.getBody(), new TypeReference<AccessTokenResponse>() {
                    });
                    tokenMap.put(entry.getKey(), accessTokenResponse.getAccessToken());
                } else {
                    throw new RuntimeException("Failed to get access token: " + response.getStatusCode());
                }
            } catch (Exception e) {
                log.error("Failed to get access token: " + response.getStatusCode());
                throw new RuntimeException("Failed to get access token: " + response.getStatusCode());
            }
        }
        AppConstant.accessTokenMap = tokenMap;

    }

}
