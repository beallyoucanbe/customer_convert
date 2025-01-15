package com.smart.sso.server.util;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.smart.sso.server.constant.AppConstant;
import com.smart.sso.server.model.VO.MessageSendVO;
import com.smart.sso.server.primary.mapper.CustomerFeatureMapper;
import com.smart.sso.server.primary.mapper.CustomerBaseMapper;
import com.smart.sso.server.primary.mapper.ScheduledTasksMapper;
import com.smart.sso.server.model.*;
import com.smart.sso.server.service.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.annotation.Id;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.*;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Stream;


@Component
@Slf4j
public class SchedulTask {

    @Autowired
    private CustomerFeatureMapper customerFeatureMapper;
    @Autowired
    private CustomerBaseMapper customerBaseMapper;
    @Autowired
    private ScheduledTasksMapper scheduledTasksMapper;
    @Autowired
    private CustomerInfoService customerInfoService;
    @Autowired
    private CustomerRelationService customerRelationService;
    @Autowired
    private MessageService messageService;
    @Autowired
    private ConfigService configService;
    @Autowired
    private TelephoneRecordService recordService;

    @Autowired
    private CommunicationService communicationService;

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
        List<TelephoneRecordStatics> customerRecordList = recordService.getCustomerIdUpdate(dateTime);
        if (CollectionUtils.isEmpty(customerRecordList)) {
            log.error("没有客户匹配度需要更新，直接返回");
            scheduledTasksMapper.updateStatusById(newTasks.getId(), "success");
            return;
        }
        for (TelephoneRecordStatics customerRecord : customerRecordList) {
            try {
                // 检查customer_Info 是否存在，如果不存在，则更新。这里是为了从 record 向info 表同步
                recordService.syncCustomerInfoFromRecord(customerRecord.getCustomerId(), customerRecord.getActivityId());
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
//    @Scheduled(cron = "0 40 8 * * ?")
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
        //获取需要当前的活动
        String activityId = configService.getCurrentActivityId();
        if (Objects.isNull(activityId)) {
            log.error("没有当前的活动，请先配置");
            return;
        }
        List<CustomerInfo> customerRelationList = customerRelationService.getByActivityAndSigned(activityId);
        // 对每一个支付定金的客户，检查销售记录值是否正确
        if (CollectionUtils.isEmpty(customerRelationList)) {
            return;
        }
        for (CustomerInfo item : customerRelationList) {
            try {
                QueryWrapper<CustomerBase> queryWrapper2 = new QueryWrapper<>();
                queryWrapper2.eq("owner_id", item.getSalesId().toString());
                queryWrapper2.eq("customer_id", item.getCustomerId().toString());
                CustomerBase customerBase = customerBaseMapper.selectOne(queryWrapper2);
                if (Objects.isNull(customerBase)) {
                    continue;
                }
                // 检查info表中是否有购买时间，如果有，代表已购买，跳过不处理，如果没有，就记录首次探测到购买的时间
                if (Objects.isNull(customerBase.getPurchaseTime())) {
                    customerBaseMapper.updatePurchaseTimeById(customerBase.getId(), item.getPurchaseTime());
                }
                CustomerFeature customerFeature = customerFeatureMapper.selectById(customerBase.getId());
                if (Objects.nonNull(customerFeature) && Objects.nonNull(customerFeature.getSoftwarePurchaseAttitudeSales())) {
                    Map<String, Object> tag =
                            JsonUtil.readValue(JsonUtil.serialize(customerFeature.getSoftwarePurchaseAttitudeSales()),
                                    new TypeReference<Map<String, Object>>() {
                                    });
                    if (Objects.nonNull(tag.get("tag")) && (Boolean) tag.get("tag")) {
                        continue;
                    }
                    tag.put("tag", true);
                    tag.put("updateTime", DateUtil.getCurrentDateTime());
                    customerFeatureMapper.updateSoftwarePurchaseAttitudeSalesById(customerFeature.getId(), JsonUtil.serialize(tag));
                } else if (Objects.nonNull(customerFeature)) {
                    Map<String, Object> tag = new HashMap<>();
                    tag.put("tag", true);
                    tag.put("updateTime", DateUtil.getCurrentDateTime());
                    customerFeatureMapper.updateSoftwarePurchaseAttitudeSalesById(customerFeature.getId(), JsonUtil.serialize(tag));
                } else {
                    CustomerFeature feature = new CustomerFeature();
                    feature.setId(customerBase.getId());
                    FeatureContentSales featureContent = new FeatureContentSales();
                    featureContent.setTag(true);
                    feature.setSoftwarePurchaseAttitudeSales(featureContent);
                    featureContent.setUpdateTime(DateUtil.getCurrentDateTime());
                    customerFeatureMapper.insert(feature);
                }
            } catch (Exception e) {
                log.error("执行购买状态任务失败：" + item.getCustomerId());
            }
        }
        // 更新成功，更新任务状态
        scheduledTasksMapper.updateStatusById(newTasks.getId(), "success");
    }

    /**
     * 每天8点半发送一次总结
     */
//    @Scheduled(cron = "0 30 8 * * ?")
    public void performTask() {
        refreshFeatureToBI();
        //获取需要当前的活动
        String activityId = configService.getCurrentActivityId();
        if (Objects.isNull(activityId)) {
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
        messageService.sendPurchaseAttitudeSummary(activityId);
        // 更新成功，更新任务状态
        scheduledTasksMapper.updateStatusById(newTasks.getId(), "success");
    }

    /**
     * 执行特征同步到BI的任务
     */
    @Scheduled(cron = "0 33 7,10,18 * * ?")
    public void refreshFeatureToBI() {
        log.error("开始执行客户情况特征同步到bi");
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
        log.error("客户情况特征同步到bi执行完成");
    }

    /**
     * 执行发送消息给领导的任务
     */
//    @Scheduled(cron = "0 0 9,18 * * ?")
    public void sendMessageToLeader() {
        log.error("开始执行发送消息给领导");
        // 获取该领导下的所有员工
    }


    /**
     * 这次参加活动的员工id
     */
//    @Scheduled(cron = "0 */13 * * * ?")
    public void refreshStaffId() {
        log.error("开始客户配置同步任务");
        configService.refreshCustomerConfig();
        log.error("客户配置同步任务执行完成");
    }


    // 通话次数刷新规则：1，每天凌晨全量刷新，即重新计算一次
    @Scheduled(cron = "0 30 3 * * ?")
    public void refreshCommunicationRounds() {
        log.error("开始全量刷新通话次数任务");
        recordService.refreshCommunicationRounds();
        log.error("全量刷新通话次数任务执行完成");
    }

    // 每天更新一次info到base表的同步
    @Scheduled(cron = "0 30 7 * * ?")
    public void refreshCustomerInfos() {
        log.error("开始刷新客户信息");
        customerInfoService.syncCustomerInfoFromRelation();
        log.error("全量刷新客户信息任务执行完成");
    }

    // 每天早上8点，判断昨晚有无需要发送的信息
//    @Scheduled(cron = "0 0 8 * * ?")
    public void executeMessageSend() {
        log.error("开始执行延迟消息发送任务");
        int size = AppConstant.messageNeedSend.size();
        log.error("需要发送的消息数是：" + size);
        if (size < 1) {
            return;
        }
        while (!AppConstant.messageNeedSend.isEmpty()) {
            MessageSendVO vo = AppConstant.messageNeedSend.poll();
            if (StringUtils.isEmpty(vo.getSendUrl())) {
                messageService.sendMessageToChat(vo.getTextMessage());
            } else {
                messageService.sendMessageToChat(vo.getSendUrl(), vo.getTextMessage());
            }
        }
        log.error("延迟消息发送任务执行完成");
    }

    /**
     * 定时检查是否有需要处理的通话
     */
    @Scheduled(cron = "0 */50 * * * ?")
    public void processCommunication() {
        log.error("开始检查是否有需要处理的通话");
        // 检查微信
        String dateTimeStr = DateUtil.getCurrentDateTime();
        String dateStr = dateTimeStr.split(" ")[0];
        Path folderPath = Paths.get("/data/customer-convert/callback/wecom/" + dateStr);
        // 获取文件夹下面的所有文件
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(folderPath)) {
            for (Path entry : stream) {
                try {
                    String fileName = entry.getFileName().toString();
                    if (!recordService.existId(fileName)) {
                        Thread.sleep(3000L);
                        log.error("开始执行python脚本处理微信对话，{}", entry);
                        communicationService.wecomCallBack(entry.toString());
                    }
                } catch (Exception e) {
                    log.error("微信记录处理失败，文件路径为：{}", entry.toString(), e);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        // 检查语音通话
        Path teleFilePath = Paths.get("/data/customer-convert/callback/telephone/" + dateStr + "_message.txt");
        try (Stream<String> lines = Files.lines(teleFilePath)) {
            // 逐行处理文件内容
            lines.forEach(item -> {
                        try {
                            Map<String, Object> one = JsonUtil.readValue(item, new TypeReference<Map<String, Object>>() {
                            });
                            String taskId = one.get("task_id").toString();
                            if (!recordService.existId(taskId)) {
                                Thread.sleep(3000L);
                                log.error("开始执行python脚本处理语音对话，task_id : {}", taskId);
                                communicationService.telephoneCallBack(item);
                            }
                        } catch (Exception e) {
                            log.error("语音记录处理失败，task_id：{}", item, e);
                        }
                    }
            );
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
