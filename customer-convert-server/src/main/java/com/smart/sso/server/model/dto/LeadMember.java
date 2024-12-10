package com.smart.sso.server.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
public class LeadMember {
    private String area;
    private List<Team> teams;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Team {
        private String leader;
        private String id;
        private Map<String, String> members;
    }
}
