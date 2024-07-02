package com.smart.sso.server.util;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.smart.sso.server.constant.AppConstant;
import com.smart.sso.server.mapper.CustomerFeatureMapper;
import com.smart.sso.server.mapper.CustomerInfoMapper;
import com.smart.sso.server.mapper.ScheduledTasksMapper;
import com.smart.sso.server.model.CustomerFeature;
import com.smart.sso.server.model.ScheduledTask;
import com.smart.sso.server.service.CustomerInfoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.time.LocalDateTime;
import java.util.List;
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


    @Scheduled(cron = "0 */15 * * * ?")
    public void refreshConversionRate() {
        // 是否有任务再执行
        log.error("开始执行客户匹配度刷新任务！");
        QueryWrapper<ScheduledTask> taskQueryWrapper = new QueryWrapper<>();
        taskQueryWrapper.eq("task_name", AppConstant.REFRESH_CONVERSION_RATE);
        taskQueryWrapper.eq("status", "in_progress");

        ScheduledTask tasks = scheduledTasksMapper.selectOne(taskQueryWrapper);
        if (Objects.nonNull(tasks)) {
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
            newTasks.setStatus("success");
            scheduledTasksMapper.updateById(newTasks);
            return;
        }
        for (CustomerFeature customerFeature : customerFeatureList) {
            try {
                // 计算当前客户的匹配度
                String conversionRate = customerInfoService.getConversionRate(customerFeature);
                // 将转化概率更新到info 表中
                customerInfoMapper.updateConversionRateById(customerFeature.getId(), conversionRate);
                // 计算当前客户的阶段信息
            } catch (Exception e) {
                log.error("客户{}匹配度更新失败，错误信息：{}", customerFeature.getId(), e.getMessage());
                newTasks.setStatus("failed");
                scheduledTasksMapper.updateById(newTasks);
            }
        }
        // 更新成功，更新任务状态
        newTasks.setStatus("success");
        scheduledTasksMapper.updateById(newTasks);
    }
}
