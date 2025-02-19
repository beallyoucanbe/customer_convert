package com.smart.sso.server.service;

import com.smart.sso.server.model.CustomerDoubt;
import com.smart.sso.server.model.RecommenderQuestion;
import com.smart.sso.server.model.RecommenderQuestionDetail;

import java.time.LocalDateTime;
import java.util.List;

public interface RecommenderService {

    RecommenderQuestion getRecommenderQuestions(String activityId, String questionType, LocalDateTime startTime, LocalDateTime endTime);

    RecommenderQuestionDetail getRecommenderQuestionDetail(String activityId, String question);

    List<CustomerDoubt> getRecommenderQuestionsListYesterday(String activityId);

    List<CustomerDoubt> getRecommenderQuestionsListToday(String activityId);

    String getAiSummaryByQuestion(String question);


}
