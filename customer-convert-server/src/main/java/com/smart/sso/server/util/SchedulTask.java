package com.smart.sso.server.util;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.smart.sso.server.constant.AppConstant;
import com.smart.sso.server.enums.ConfigTypeEnum;
import com.smart.sso.server.mapper.ConfigMapper;
import com.smart.sso.server.mapper.CustomerCharacterMapper;
import com.smart.sso.server.mapper.CustomerFeatureMapper;
import com.smart.sso.server.mapper.CustomerInfoMapper;
import com.smart.sso.server.mapper.ScheduledTasksMapper;
import com.smart.sso.server.model.*;
import com.smart.sso.server.model.dto.LeadMemberRequest;
import com.smart.sso.server.service.CustomerInfoService;
import com.smart.sso.server.service.MessageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;


@Component
@Slf4j
public class SchedulTask {

    @Autowired
    private CustomerFeatureMapper customerFeatureMapper;
    @Autowired
    private CustomerInfoMapper customerInfoMapper;
    @Autowired
    private ScheduledTasksMapper scheduledTasksMapper;
    @Autowired
    private CustomerInfoService customerInfoService;
    @Autowired
    private ConfigMapper configMapper;
    @Autowired
    private CustomerCharacterMapper customerCharacterMapper;
    @Autowired
    private MessageService messageService;

    @Scheduled(cron = "0 */15 * * * ?")
    public void refreshConversionRate() {
        // 是否有任务再执行
        log.error("开始执行客户匹配度刷新任务！");
        QueryWrapper<ScheduledTask> taskQueryWrapper = new QueryWrapper<>();
        taskQueryWrapper.eq("task_name", AppConstant.REFRESH_CONVERSION_RATE);
        taskQueryWrapper.eq("status", "in_progress");

        ScheduledTask tasks = scheduledTasksMapper.selectOne(taskQueryWrapper);
        if (Objects.nonNull(tasks)) {
            // 这里判断时间，防止意外崩溃的情况
            LocalDateTime dateTimeToCompare = tasks.getCreateTime();
            // 计算时间差
            Duration duration = Duration.between(dateTimeToCompare, LocalDateTime.now());
            // 超过半小时就强制退出
            if (duration.getSeconds() > 1800) {
                scheduledTasksMapper.updateStatusById(tasks.getId(), "abort");
            }
            log.error("有任务正在执行，该次不执行");
            return;
        }
        // 插入新的任务
        ScheduledTask newTasks = new ScheduledTask();
        newTasks.setId(CommonUtils.generatePrimaryKey());
        newTasks.setTaskName(AppConstant.REFRESH_CONVERSION_RATE);
        newTasks.setStatus("in_progress");
        scheduledTasksMapper.insert(newTasks);

        // 获取最后一次执行成功的开始时间， 来决定该次任务的筛选条件
        LocalDateTime dateTime = LocalDateTime.of(2024, 1, 1, 12, 0, 0);
        taskQueryWrapper = new QueryWrapper<>();
        taskQueryWrapper.eq("task_name", AppConstant.REFRESH_CONVERSION_RATE);
        taskQueryWrapper.eq("status", "success");
        taskQueryWrapper.orderByDesc("create_time");
        taskQueryWrapper.last("limit 1");
        tasks = scheduledTasksMapper.selectOne(taskQueryWrapper);
        if (Objects.nonNull(tasks)) {
            dateTime = tasks.getCreateTime();
        }
        // 查询所有符合条件的客户，并执行任务
        QueryWrapper<CustomerFeature> queryWrapper = new QueryWrapper<>();
        // 筛选时间
        queryWrapper.gt("update_time", dateTime);
        List<CustomerFeature> customerFeatureList = customerFeatureMapper.selectList(queryWrapper);
        if (CollectionUtils.isEmpty(customerFeatureList)) {
            log.error("没有客户匹配度需要更新，直接返回");
            scheduledTasksMapper.updateStatusById(newTasks.getId(), "success");
            return;
        }
        for (CustomerFeature customerFeature : customerFeatureList) {
            try {
                // 计算当前客户的匹配度
                String conversionRate = customerInfoService.getConversionRate(customerFeature);
                // 将转化概率更新到info 表中
                customerInfoMapper.updateConversionRateById(customerFeature.getId(), conversionRate);
            } catch (Exception e) {
                log.error("客户{}匹配度更新失败，错误信息：{}", customerFeature.getId(), e.getMessage());
                scheduledTasksMapper.updateStatusById(newTasks.getId(), "failed");
            }
        }
        // 更新成功，更新任务状态
        scheduledTasksMapper.updateStatusById(newTasks.getId(), "success");
    }

    /**
     * 每天12点和18点执行团队总结任务
     * 以下是您团队的半日报更新。截至X月X日XX：XX：
     * 【问题】
     * 未完成客户匹配度判断：当前共计30个
     * 跟进错的客户：当前共计10个
     * 未完成客户交易风格了解：当前共计9个
     * 未完成针对性介绍功能：当前共计15个
     * 客户对老师和课程不认可：当前共计15个
     * 客户对软件功能不理解：当前共计15个
     * 客户对选股方法不认可：当前共计15个
     * 客户对自身问题不认可：当前共计15个
     * 客户对软件价值不认可：当前共计15个
     * 客户拒绝购买：当前共计15个
     * <p>
     * 【进展】
     * 完成客户匹配度判断：当前共计30个
     * 完成客户交易风格了解：当前共计9个
     * 客户认可老师和课程：当前共计20个
     * 客户理解了软件功能：当前共计20个
     * 客户认可选股方法：当前共计20个
     * 客户认可自身问题：当前共计20个
     * 客户认可软件价值：当前共计20个
     * 客户确认购买：当前共计9个
     * 客户完成购买：当前共计5个
     * 详细内容链接：http://xxxxxxxxx（BI对应看板页面链接）
     */
    @Scheduled(cron = "0 0 12,18 * * ?")
    public void performTask() {
        log.error("开始执行客户情况总结任务");
        // 执行之前先全量更新数据到BI
        LocalDateTime dateTime = LocalDateTime.of(2024, 1, 1, 12, 0, 0);
        QueryWrapper<CustomerInfo> queryWrapperInfo = new QueryWrapper<>();
        // 筛选时间
        queryWrapperInfo.gt("update_time", dateTime);
        List<CustomerInfo> customerFeatureList = customerInfoMapper.selectList(queryWrapperInfo);
        for (CustomerInfo item : customerFeatureList) {
            try {
                messageService.updateCustomerCharacter(item.getId());
            } catch (Exception e) {
               log.error("更新CustomerCharacter失败，ID={}", item.getId(), e);
            }
        }
        //获取需要发送的组长
        QueryWrapper<Config> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("type", ConfigTypeEnum.COMMON.getValue());
        queryWrapper.eq("name", ConfigTypeEnum.LEADER.getValue());
        Config config = configMapper.selectOne(queryWrapper);
        if (Objects.isNull(config)) {
            log.error("没有配置组长信息，请先配置");
            return;
        }
        List<LeadMemberRequest> leadMemberList = JsonUtil.readValue(config.getValue(), new TypeReference<List<LeadMemberRequest>>() {
        });
        //获取需要当前的活动
        QueryWrapper<Config> queryWrapper1 = new QueryWrapper<>();
        queryWrapper1.eq("type", ConfigTypeEnum.COMMON.getValue());
        queryWrapper1.eq("name", ConfigTypeEnum.CURRENT_CAMPAIGN.getValue());
        config = configMapper.selectOne(queryWrapper1);
        if (Objects.isNull(config)) {
            log.error("没有当前的活动，请先配置");
            return;
        }
        String currentCampaign = config.getValue();

        for (LeadMemberRequest leadMember : leadMemberList) {
            messageService.sendNoticeForLeader(leadMember, currentCampaign);
        }
    }
}
