package com.smart.sso.server.service;

import com.smart.sso.server.model.RecommenderQuestion;
import com.smart.sso.server.model.RecommenderQuestionDetail;

import java.time.LocalDateTime;

public interface RecommenderService {

    RecommenderQuestion getRecommenderQuestions(String activityId, String questionType, LocalDateTime startTime, LocalDateTime endTime);

    RecommenderQuestionDetail getRecommenderQuestionDetail(String activityId, String question);

}
