package com.smart.sso.server.service.impl;

import com.smart.sso.server.model.RecommenderQuestion;
import com.smart.sso.server.service.RecommenderService;

import java.time.LocalDateTime;

public class RecommenderServiceImpl implements RecommenderService {
    @Override
    public RecommenderQuestion getRecommenderQuestions(String activityName, String questionType, LocalDateTime startTime, LocalDateTime endTime) {
        return null;
    }
}
