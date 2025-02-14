package com.smart.sso.server.model;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CourseListenDetail {
    // 课程名称
    private String courseName;
    // 听课开始时间
    private String courseListenTime;
    // 听课进度
    private int courseListenProcess;
    // 是否完播
    private Boolean playAll;
}
