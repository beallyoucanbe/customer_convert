package com.smart.sso.server.service;

import com.smart.sso.server.model.RecommenderQuestion;

import java.time.LocalDateTime;

public interface RecommenderService {

    RecommenderQuestion getRecommenderQuestions(String activityId, String questionType, LocalDateTime startTime, LocalDateTime endTime);
}
