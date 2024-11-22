package com.smart.sso.server.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PotentialCustomer {
    // 认可数>=3旦态度为认可
    private List<String> high = new ArrayList<>();
    // 认可数>=3旦态度为尚未认可
    private List<String> middle = new ArrayList<>();
    // 匹配度为中高且认可数<=2
    private List<String> low = new ArrayList<>();
}
