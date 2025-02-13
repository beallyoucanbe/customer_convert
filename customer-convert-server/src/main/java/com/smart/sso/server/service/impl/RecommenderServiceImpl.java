package com.smart.sso.server.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.smart.sso.server.model.CustomerDoubt;
import com.smart.sso.server.model.RecommenderQuestion;
import com.smart.sso.server.model.RecommenderQuestionDetail;
import com.smart.sso.server.model.TelephoneRecord;
import com.smart.sso.server.primary.mapper.CustomerDoubtMapper;
import com.smart.sso.server.primary.mapper.TelephoneRecordMapper;
import com.smart.sso.server.service.RecommenderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class RecommenderServiceImpl implements RecommenderService {

    @Autowired
    private CustomerDoubtMapper customerDoubtMapper;

    @Autowired
    private TelephoneRecordMapper recordMapper;

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
        QueryWrapper<CustomerDoubt> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("activity_id", activityId);
        queryWrapper.eq("norm_doubt", question);
        List<CustomerDoubt> resultPage = customerDoubtMapper.selectList(queryWrapper);


        return null;
    }
}
