package com.smart.sso.server.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RecommenderQuestion {

    // 处理会话个数
    private Integer communicationCount;
    //常见问题的种类
    private Integer questionTypeCount;
    //业务员会话沟通记录
    private Integer conversationRecordCount;
    //问题列表
    private List<QuestionContent> questions;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class QuestionContent {
        private String questionName;
        private Integer count;
    }
}
