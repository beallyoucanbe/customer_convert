package com.smart.sso.server.model.VO;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CustomerListVO {
    private String id ;
    @JsonProperty("name")
    private String customerName ;
    private String customerId ;
    @JsonProperty("owner")
    private String ownerName ;
    private String activityName;
    private String activityId;
    // 客户匹配度
    // -较高：资金体量=“充裕”或“大于等于10万” and 赚钱欲望=“高”
    // -中等：(资金体量=“匮乏”或“小于10万” and 赚钱欲望=“高”) or (资金体量=“充裕”或“大于等于10万” and 赚钱欲望=“低”)
    // -较低：资金体量=“匮乏”或“小于10万” and 赚钱欲望=“低”
    // -未完成判断：资金体量=空 or 赚钱欲望=空
    private String conversionRate ;
    @JsonProperty("last_update")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime ;
}
