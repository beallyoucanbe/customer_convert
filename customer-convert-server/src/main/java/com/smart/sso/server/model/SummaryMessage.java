package com.smart.sso.server.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.LinkedHashMap;
import java.util.Map;


@Data
public class SummaryMessage {
    private Map<String, Integer> questions;
    private Map<String, Integer> advantages;

    public SummaryMessage(){
        questions = new LinkedHashMap<>();
        questions.put("尚未完成客户资金体量收集，需继续收集客户信息", 0);
        questions.put("尚未完成客户匹配度判断，需继续收集客户信息", 0);
        questions.put("跟进匹配度低的客户，需确认匹配度高和中的客户都已跟进完毕再跟进匹配度低的客户", 0);
        questions.put("尚未完成客户交易风格了解，需继续收集客户信息", 0);
        questions.put("SOP执行顺序错误，需完成前序任务", 0);
        questions.put("尚未完成痛点和价值量化放大，需后续完成", 0);
        questions.put("客户对软件功能尚未理解清晰，需根据客户学习能力更白话讲解", 0);
        questions.put("客户对选股方法尚未认可，需加强选股成功的真实案例证明", 0);
        questions.put("客户对自身问题尚未认可，需列举与客户相近的真实反面案例证明", 0);
        questions.put("客户对软件价值尚未认可，需加强使用软件的真实成功案例证明", 0);
        questions.put("质疑应对失败次数多，需参考调整应对话", 0);
        questions.put("客户拒绝购买，需暂停劝说客户购买，明确拒绝原因进行化解", 0);

        advantages = new LinkedHashMap<>();
        advantages.put("完成客户资金体量收集", 0);
        advantages.put("完成客户匹配度判断", 0);
        advantages.put("跟进对的客户", 0);
        advantages.put("完成客户交易风格了解", 0);
        advantages.put("SOP执行顺序正确", 0);
        advantages.put("痛点和价值量化放大", 0);
        advantages.put("客户对软件功能理解清晰", 0);
        advantages.put("客户认可选股方法", 0);
        advantages.put("客户认可自身问题", 0);
        advantages.put("客户认可软件价值", 0);
        advantages.put("客户确认购买", 0);
        advantages.put("客户完成购买", 0);
    }
}
