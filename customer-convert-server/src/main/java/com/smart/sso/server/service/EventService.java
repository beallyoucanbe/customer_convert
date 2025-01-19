package com.smart.sso.server.service;

import com.smart.sso.server.model.dto.CustomerFeatureResponse;

import java.time.LocalDateTime;

public interface EventService {


    // 交付课听课情况
    CustomerFeatureResponse.CourseContent getDeliveryCourseListenContent(String userId);

    // 营销课听课情况
    CustomerFeatureResponse.CourseContent getMarketingCourseListenContent(String userId);

    // 直播访问频次
    CustomerFeatureResponse.FrequencyContent getVisitLiveFreqContent(String userId, LocalDateTime customerCreateTime);

    // 圈子访问频次
    CustomerFeatureResponse.FrequencyContent getVisitCommunityFreqContent(String userId, LocalDateTime customerCreateTime);

    // 功能指标使用频次
    CustomerFeatureResponse.FrequencyContent getFunctionFreqContent(String userId,  LocalDateTime customerCreateTime);


}
