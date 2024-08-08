package com.smart.sso.server.util;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.google.common.collect.ImmutableMap;
import com.smart.sso.server.constant.AppConstant;
import com.smart.sso.server.enums.ConfigTypeEnum;
import com.smart.sso.server.mapper.ConfigMapper;
import com.smart.sso.server.mapper.CustomerCharacterMapper;
import com.smart.sso.server.mapper.CustomerFeatureMapper;
import com.smart.sso.server.mapper.CustomerInfoMapper;
import com.smart.sso.server.mapper.ScheduledTasksMapper;
import com.smart.sso.server.model.Config;
import com.smart.sso.server.model.CustomerCharacter;
import com.smart.sso.server.model.CustomerFeature;
import com.smart.sso.server.model.ScheduledTask;
import com.smart.sso.server.model.TextMessage;
import com.smart.sso.server.service.CustomerInfoService;
import com.smart.sso.server.service.MessageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;


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
        //获取当前阶段的活动
        QueryWrapper<Config> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("type", ConfigTypeEnum.COMMON.getValue());
        queryWrapper.eq("name", ConfigTypeEnum.CURRENT_CAMPAIGN.getValue());
        Config config = configMapper.selectOne(queryWrapper);
        // 获取当前活动内的所有客户
        QueryWrapper<CustomerCharacter> queryWrapper1 = new QueryWrapper<>();
        queryWrapper1.eq("type", ConfigTypeEnum.COMMON.getValue());
        queryWrapper1.eq("name", ConfigTypeEnum.CURRENT_CAMPAIGN.getValue());
        List<CustomerCharacter> characterList = customerCharacterMapper.selectList(queryWrapper1);

        Map<String, Integer> questions = new TreeMap<>();
        questions.put("未完成客户匹配度判断", 0);
        questions.put("跟进错的客户", 0);
        questions.put("未完成客户交易风格了解", 0);
        questions.put("未完成针对性介绍功能", 0);
        questions.put("客户对老师和课程不认可", 0);
        questions.put("客户对软件功能不理解", 0);
        questions.put("客户对选股方法不认可", 0);
        questions.put("客户对自身问题不认可", 0);
        questions.put("客户对软件价值不认可", 0);
        questions.put("客户拒绝购买", 0);

        Map<String, Integer> advantages = new TreeMap<>();
        advantages.put("完成客户匹配度判断", 0);
        advantages.put("完成客户交易风格了解", 0);
        advantages.put("客户认可老师和课程", 0);
        advantages.put("客户理解了软件功能", 0);
        advantages.put("客户认可选股方法", 0);
        advantages.put("客户认可自身问题", 0);
        advantages.put("客户认可软件价值", 0);
        advantages.put("客户确认购买", 0);
        advantages.put("客户完成购买", 0);


        // 对获取的所有客户进行总结
        execute(characterList, questions, advantages);

        StringBuilder complete = new StringBuilder();
        StringBuilder incomplete = new StringBuilder();
        int i = 1;
        for (Map.Entry<String, Integer> item : advantages.entrySet()) {
            if (item.getValue() == 0) {
                continue;
            }
            complete.append(i++).append(". ").append(item.getKey()).append("：当前共计").append(item.getValue())
                    .append("个\n");
        }
        i = 1;
        for (Map.Entry<String, Integer> item : questions.entrySet()) {
            incomplete.append(i++).append(". ").append(item.getKey()).append("：当前共计").append(item.getValue())
                    .append("个\n");
        }
        String url = "https://newcmp.emoney.cn/chat/customers";
        String message = String.format(AppConstant.LEADER_SUMMARY_MARKDOWN_TEMPLATE, DateUtil.getFormatCurrentTime("yyyy-MM-dd HH:mm"),
                incomplete,
                complete,
                url, url);

        // 获取要发送的url
        QueryWrapper<Config> queryWrapper2 = new QueryWrapper<>();
        queryWrapper2.eq("type", ConfigTypeEnum.NOTIFY_URL.getValue());
        queryWrapper2.eq("name", "");
        config = configMapper.selectOne(queryWrapper2);
        String notifyUrl = "";
        if (Objects.isNull(config)) {
            log.error("没有配置该销售的报警url，使用默认的报警配置");
            notifyUrl = "https://qyapi.weixin.qq.com/cgi-bin/webhook/send?key=599ac6c1-091c-4dd3-99b6-1fbd76411d87";
        } else {
            notifyUrl = config.getValue();
        }
        TextMessage textMessage = new TextMessage();
        TextMessage.TextContent textContent = new TextMessage.TextContent();
        textContent.setContent(message);
        textMessage.setMsgtype("markdown");
        textMessage.setMarkdown(textContent);
        messageService.sendMessageToChat(notifyUrl, textMessage);
    }

    private void execute(List<CustomerCharacter> characterList, Map<String, Integer> questions, Map<String, Integer> advantages) {
        if (CollectionUtils.isEmpty(characterList)) {
            return;
        }
        for (CustomerCharacter character : characterList) {
            if (character.getMatchingJudgmentStage()) {
                advantages.put("完成客户匹配度判断", advantages.get("完成客户匹配度判断") + 1);
            } else {
                questions.put("未完成客户匹配度判断", questions.get("未完成客户匹配度判断") + 1);
            }
            if (character.getTransactionStyleStage()) {
                advantages.put("完成客户交易风格了解", advantages.get("完成客户交易风格了解") + 1);
            } else {
                questions.put("未完成客户交易风格了解", questions.get("未完成客户交易风格了解") + 1);
            }
            if (character.getFunctionIntroductionStage()) {
                advantages.put("完成客户交易风格了解", advantages.get("完成客户交易风格了解") + 1);
            } else {
                questions.put("未完成针对性介绍功能", questions.get("未完成针对性介绍功能") + 1);
            }


        }
    }
}
