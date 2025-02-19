package com.smart.sso.server.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.smart.sso.server.model.CustomerDoubt;
import com.smart.sso.server.model.DoubtInfo;
import com.smart.sso.server.model.RecommenderQuestion;
import com.smart.sso.server.model.RecommenderQuestionDetail;
import com.smart.sso.server.model.TelephoneRecord;
import com.smart.sso.server.model.dto.CustomerFeatureResponse;
import com.smart.sso.server.primary.mapper.CustomerDoubtMapper;
import com.smart.sso.server.primary.mapper.DoubtInfoMapper;
import com.smart.sso.server.primary.mapper.TelephoneRecordMapper;
import com.smart.sso.server.service.RecommenderService;
import com.smart.sso.server.util.CommonUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class RecommenderServiceImpl implements RecommenderService {

    @Autowired
    private CustomerDoubtMapper customerDoubtMapper;

    @Autowired
    private DoubtInfoMapper doubtInfoMapper;

    @Autowired
    private TelephoneRecordMapper recordMapper;

    private DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public RecommenderQuestion getRecommenderQuestions(String activityId, String questionType, LocalDateTime startTime, LocalDateTime endTime) {
        QueryWrapper<CustomerDoubt> queryWrapper = new QueryWrapper<>();
        QueryWrapper<TelephoneRecord> recordQueryWrapper = new QueryWrapper<>();
        if (!StringUtils.isEmpty(activityId)) {
            queryWrapper.eq("activity_id", activityId);
            recordQueryWrapper.eq("activity_id", activityId);
        }
        if (!StringUtils.isEmpty(questionType)) {
            queryWrapper.like("norm_doubt", questionType);
        }
        if (Objects.nonNull(startTime)) {
            queryWrapper.gt("communication_time", startTime);
            recordQueryWrapper.gt("communication_time", startTime);
        }
        if (Objects.nonNull(endTime)) {
            queryWrapper.lt("communication_time", endTime);
            recordQueryWrapper.lt("communication_time", endTime);
        }
        List<CustomerDoubt> customerDoubtList = customerDoubtMapper.selectList(queryWrapper);
        Long communicationCount = recordMapper.selectCount(recordQueryWrapper);
        if (Objects.isNull(communicationCount)) {
            communicationCount = 0L;
        }
        RecommenderQuestion recommenderQuestion = new RecommenderQuestion();
        recommenderQuestion.setCommunicationCount(communicationCount.intValue());
        recommenderQuestion.setConversationRecordCount(communicationCount.intValue());
        if (CollectionUtils.isEmpty(customerDoubtList)) {
            return recommenderQuestion;
        }
        List<Map.Entry<String, Long>> sortedEntryList = customerDoubtList.stream()
                .collect(Collectors.groupingBy(CustomerDoubt::getNormDoubt, Collectors.counting()))
                .entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .collect(Collectors.toList());

        recommenderQuestion.setQuestionTypeCount(sortedEntryList.size());
        List<RecommenderQuestion.QuestionContent> questions = new ArrayList<>();
        sortedEntryList.forEach(item -> {
            questions.add(new RecommenderQuestion.QuestionContent(item.getKey(), item.getValue().intValue()));
        });
        recommenderQuestion.setQuestions(questions);
        return recommenderQuestion;
    }

    @Override
    public RecommenderQuestionDetail getRecommenderQuestionDetail(String activityId, String question) {
        RecommenderQuestionDetail result = new RecommenderQuestionDetail();
        QueryWrapper<CustomerDoubt> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("activity_id", activityId);
        queryWrapper.eq("norm_doubt", question);
        List<CustomerDoubt> customerDoubtList = customerDoubtMapper.selectList(queryWrapper);

        CustomerFeatureResponse.RecordContent recordContent = new CustomerFeatureResponse.RecordContent();
        List<CustomerFeatureResponse.RecordTitle> columns = new ArrayList<>();
        columns.add(new CustomerFeatureResponse.RecordTitle("owner_name", "业务员姓名"));
        columns.add(new CustomerFeatureResponse.RecordTitle("level", "级别"));
        columns.add(new CustomerFeatureResponse.RecordTitle("answer", "销售应对过程"));
        columns.add(new CustomerFeatureResponse.RecordTitle("communication_time", "会话时间"));
        recordContent.setColumns(columns);
        List<Map<String, Object>> data = new ArrayList<>();
        if (!CollectionUtils.isEmpty(customerDoubtList)) {
            for (CustomerDoubt doubt : customerDoubtList) {
                Map<String, Object> item = new HashMap<>();
                item.put("owner_name", doubt.getSaleName());
                item.put("level", doubt.getSaleCategory());
                item.put("answer", CommonUtils.getOriginChatFromChatText(doubt.getCallId(), doubt.getTalkText()));
                item.put("communication_time", doubt.getCommunicationTime().format(formatter));
                item.put("chat_id", doubt.getCallId());
                item.put("customer_id", doubt.getCustomerId());
                item.put("activity_id", doubt.getActivityId());
                data.add(item);
            }
        }
        recordContent.setData(data);
        result.setRecords(recordContent);
        List<DoubtInfo> doubtInfoList = doubtInfoMapper.selectByNormDoubt(question);
        if (!CollectionUtils.isEmpty(doubtInfoList)){
            result.setAiConclusion(doubtInfoList.get(0).getSummary());
        }
        return result;
    }

    @Override
    public List<CustomerDoubt> getRecommenderQuestionsListYesterday(String activityId) {
        // 获取当前时间
        LocalDateTime currentTime = LocalDateTime.now();
        // 获取昨天的日期
        LocalDateTime yesterday = currentTime.minusDays(1);
        // 获取昨天的开始时间（00:00:00）
        LocalDateTime startOfYesterday = yesterday.toLocalDate().atStartOfDay();
        // 获取昨天的结束时间（23:59:59）
        LocalDateTime endOfYesterday = yesterday.toLocalDate().atStartOfDay().plusDays(1).minusSeconds(1);
        return customerDoubtMapper.selectByActivityIdAndTime(activityId, startOfYesterday, endOfYesterday);
    }

    @Override
    public List<CustomerDoubt> getRecommenderQuestionsListToday(String activityId) {
        // 获取当前时间的 LocalDateTime 对象
        LocalDateTime currentTime = LocalDateTime.now();
        // 获取当天日期的开始时间（00:00:00）的 LocalDateTime 对象
        LocalDateTime startOfDay = currentTime.toLocalDate().atStartOfDay();
        return customerDoubtMapper.selectByActivityIdAndTime(activityId, startOfDay, currentTime);
    }

    @Override
    public String getAiSummaryByQuestion(String question) {
        List<DoubtInfo> doubtInfoList = doubtInfoMapper.selectByNormDoubt(question);
        if (!CollectionUtils.isEmpty(doubtInfoList)){
            return doubtInfoList.get(0).getSummary();
        }
        return null;
    }
}
