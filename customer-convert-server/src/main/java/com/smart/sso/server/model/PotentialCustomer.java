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
    // 资金量≥5万且认可数≥3，购买态度为确认购买
    private List<String> high = new ArrayList<>();
    // 资金量≥5万且认可数≥3，购买态度为尚未确认购买
    private List<String> middle = new ArrayList<>();
    // 资金量≥5万且认可数≤2
    private List<String> low = new ArrayList<>();
    // 资金量≥5万且认可数≥3，超过3天未联系
    private List<String> longTimeNoSee = new ArrayList<>();
}
