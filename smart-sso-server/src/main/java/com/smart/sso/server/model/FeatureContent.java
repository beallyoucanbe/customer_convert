package com.smart.sso.server.model;

import lombok.*;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FeatureContent {

    private List<ContentItem> contentList;

    @Getter
    @Setter
    public static class ContentItem {
        private String callId;
        private String question;
        private String answer;
    }
}


