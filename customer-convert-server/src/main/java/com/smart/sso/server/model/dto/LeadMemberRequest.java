package com.smart.sso.server.model.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
public class LeadMemberRequest {
    private String area;
    private List<String> leaders;
    private List<String> members;
}
