package com.smart.sso.server.service.impl;

import com.smart.sso.server.model.RecommenderQuestion;
import com.smart.sso.server.service.RecommenderService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class RecommenderServiceImpl implements RecommenderService {
    @Override
    public RecommenderQuestion getRecommenderQuestions(String activityName, String questionType, LocalDateTime startTime, LocalDateTime endTime) {
        return null;
    }
}
