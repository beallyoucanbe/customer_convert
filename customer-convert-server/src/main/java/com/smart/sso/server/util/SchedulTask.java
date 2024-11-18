package com.smart.sso.server.util;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.smart.sso.server.constant.AppConstant;
import com.smart.sso.server.enums.ConfigTypeEnum;
import com.smart.sso.server.mapper.*;
import com.smart.sso.server.model.*;
import com.smart.sso.server.model.dto.LeadMemberRequest;
import com.smart.sso.server.service.ConfigService;
import com.smart.sso.server.service.CustomerInfoService;
import com.smart.sso.server.service.MessageService;
import com.smart.sso.server.service.TelephoneRecordService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
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
    private CustomerRelationMapper customerRelationMapper;
    @Autowired
    private MessageService messageService;
    @Autowired
    private ConfigService configService;
    @Autowired
    private TelephoneRecordService recordService;

//    @Scheduled(cron = "0 */15 * * * ?")
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
        List<TelephoneRecordStatics> customerRecordList = recordService.getCustomerIdUpdate(dateTime);
        if (CollectionUtils.isEmpty(customerRecordList)) {
            log.error("没有客户匹配度需要更新，直接返回");
            scheduledTasksMapper.updateStatusById(newTasks.getId(), "success");
            return;
        }
        for (TelephoneRecordStatics customerRecord : customerRecordList) {
            try {
                // 更新的匹配度（获取CustomerProfile会更新匹配度）
                customerInfoService.queryCustomerById(customerRecord.getCustomerId(), customerRecord.getActivityId());
            } catch (Exception e) {
                log.error("客户{}匹配度更新失败，错误信息：{}", customerRecord.getCustomerId(), e.getMessage());
                scheduledTasksMapper.updateStatusById(newTasks.getId(), "failed");
            }
        }
        // 更新成功，更新任务状态
        scheduledTasksMapper.updateStatusById(newTasks.getId(), "success");
    }

    /**
     * 每天8点半执行购买状态同步任务
     */
    @Scheduled(cron = "0 30 8 * * ?")
    public void purchaseTask() {
        log.error("开始执行客户购买状态同步任务");
        QueryWrapper<ScheduledTask> taskQueryWrapper = new QueryWrapper<>();
        taskQueryWrapper.eq("task_name", AppConstant.PURCHASE_STATE_CHECK);
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
        newTasks.setTaskName(AppConstant.PURCHASE_STATE_CHECK);
        newTasks.setStatus("in_progress");
        scheduledTasksMapper.insert(newTasks);

        // 开始执行实际的任务
        QueryWrapper<CustomerRelation> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("customer_signed", 1);
        List<CustomerRelation> customerRelationList= customerRelationMapper.selectList(queryWrapper);
        // 对每一个支付定金的客户，检查销售记录值是否正确
        if (CollectionUtils.isEmpty(customerRelationList)) {
            return;
        }
        for (CustomerRelation item : customerRelationList) {
            try {
                QueryWrapper<CustomerInfo> queryWrapper2 = new QueryWrapper<>();
                queryWrapper2.eq("owner_id", item.getOwnerId());
                queryWrapper2.eq("customer_id", item.getCustomerId().toString());
                CustomerInfo customerInfo = customerInfoMapper.selectOne(queryWrapper2);
                if (Objects.isNull(customerInfo)){
                    continue;
                }
                CustomerFeature customerFeature = customerFeatureMapper.selectById(customerInfo.getId());
                if (Objects.nonNull(customerFeature.getSoftwarePurchaseAttitudeSales())){
                    Map<String, Object> tag =
                            JsonUtil.readValue(JsonUtil.serialize(customerFeature.getSoftwarePurchaseAttitudeSales()),
                                    new TypeReference<Map<String, Object>>() {});
                    if (Objects.nonNull(tag.get("tag")) && (Boolean)tag.get("tag")) {
                        continue;
                    }
                    tag.put("tag", true);
                    customerFeatureMapper.updateSoftwarePurchaseAttitudeSalesById(customerFeature.getId(),
                            JsonUtil.serialize(tag));
                } else {
                    Map<String, Object> tag = new HashMap<>();
                    tag.put("tag", true);
                    customerFeatureMapper.updateSoftwarePurchaseAttitudeSalesById(customerFeature.getId(),
                            JsonUtil.serialize(tag));
                }
            } catch (Exception e) {
                log.error("执行购买状态任务失败：" + item.getCustomerId());
            }
        }
        // 更新成功，更新任务状态
        scheduledTasksMapper.updateStatusById(newTasks.getId(), "success");
    }

    /**
     * 每天9点，12点和18点执行团队总结任务
     */
//    @Scheduled(cron = "0 0 9,12,18 * * ?")
    public void performTask() {
        log.error("开始执行客户情况特征同步到bi");
        // 执行之前先全量更新数据到BI
        LocalDateTime dateTime = LocalDateTime.now().minusDays(14).with(LocalTime.MIN);
        List<TelephoneRecordStatics> customerRecordList = recordService.getCustomerIdUpdate(dateTime);
        if (CollectionUtils.isEmpty(customerRecordList)) {
            return;
        }
        for (TelephoneRecordStatics item : customerRecordList) {
            try {
                messageService.updateCustomerCharacter(item.getCustomerId(), item.getActivityId(), false);
            } catch (Exception e) {
               log.error("更新CustomerCharacter失败，CustomerId={}, activityId={}", item.getCustomerId(), item.getActivityId(), e);
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

        // 任务的判断
        log.error("开始执行总结信息发送");
        QueryWrapper<ScheduledTask> taskQueryWrapper = new QueryWrapper<>();
        taskQueryWrapper.eq("task_name", AppConstant.SEND_MESSAGE_STATE);
        taskQueryWrapper.eq("status", "in_progress");

        ScheduledTask tasks = scheduledTasksMapper.selectOne(taskQueryWrapper);
        if (Objects.nonNull(tasks)) {
            // 这里判断时间，防止意外崩溃的情况
            LocalDateTime dateTimeToCompare = tasks.getCreateTime();
            // 计算时间差
            Duration duration = Duration.between(dateTimeToCompare, LocalDateTime.now());
            // 超过1小时就强制退出
            if (duration.getSeconds() > 3600) {
                scheduledTasksMapper.updateStatusById(tasks.getId(), "abort");
            } else {
                log.error("有任务正在执行，该次不执行");
                return;
            }
        }
        // 插入新的任务
        ScheduledTask newTasks = new ScheduledTask();
        newTasks.setId(CommonUtils.generatePrimaryKey());
        newTasks.setTaskName(AppConstant.SEND_MESSAGE_STATE);
        newTasks.setStatus("in_progress");
        scheduledTasksMapper.insert(newTasks);

        // 获取最后一次执行成功的开始时间， 来决定该次任务的筛选条件
        dateTime = LocalDateTime.now().minusDays(1);
        taskQueryWrapper = new QueryWrapper<>();
        taskQueryWrapper.eq("task_name", AppConstant.SEND_MESSAGE_STATE);
        taskQueryWrapper.eq("status", "success");
        taskQueryWrapper.orderByDesc("create_time");
        taskQueryWrapper.last("limit 1");
        tasks = scheduledTasksMapper.selectOne(taskQueryWrapper);
        if (Objects.nonNull(tasks)) {
            dateTime = tasks.getCreateTime();
        }

        String currentCampaign = config.getValue();

        for (LeadMemberRequest leadMember : leadMemberList) {
            messageService.sendNoticeForLeader(leadMember, currentCampaign, dateTime);
        }
        // 更新成功，更新任务状态
        scheduledTasksMapper.updateStatusById(newTasks.getId(), "success");
    }

    /**
     * 这次参加活动的员工id
     */
    @Scheduled(cron = "0 */13 * * * ?")
    public void refreshStaffId() {
        log.error("开始执行业务员id同步任务");
        AppConstant.staffIdList.addAll(configService.getStaffIds());
    }


    // 通话次数刷新规则：1，每天凌晨全量刷新，即重新计算一次
    @Scheduled(cron = "0 30 3 * * ?")
    public void refreshCommunicationRounds() {
        log.error("开始全量刷新通话次数任务");
        recordService.refreshCommunicationRounds();
    }

}
