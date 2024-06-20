package com.smart.sso.server.model;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.smart.sso.server.handler.*;
import com.smart.sso.server.model.dto.CustomerProcessSummaryResponse;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

@Data
@TableName(autoResultMap = true)
public class CustomerSummary implements Serializable {
    /**
     * 主键
     */
    @TableId
    private String id;
    @TableField(typeHandler = CommaSeparatedStringTypeHandler.class)
    private List<String> summaryAdvantage;
    @TableField(typeHandler = CommaSeparatedStringTypeHandler.class)
    private List<String> summaryQuestions;

    // 销售有结合客户的股票举例
    @TableField(typeHandler = SummaryContentTypeHandler.class)
    private List<SummaryContent> illustrateBasedStock;
    // 销售有基于客户交易风格做针对性的功能介绍
    @TableField(typeHandler = SummaryContentTypeHandler.class)
    private List<SummaryContent> tradeStyleIntroduce;
    // 销售有点评客户的选股方法
    @TableField(typeHandler = SummaryContentTypeHandler.class)
    private List<SummaryContent> stockPickMethodReview;
    // 销售有点评客户的选股时机
    @TableField(typeHandler = SummaryContentTypeHandler.class)
    private List<SummaryContent> stockPickTimingReview;


    @TableField(typeHandler = ProcessContentTypeHandler.class)
    private CustomerProcessSummaryResponse.ProcessContent approvalAnalysisMethod;
    @TableField(typeHandler = ProcessContentTypeHandler.class)
    private CustomerProcessSummaryResponse.ProcessContent approvalAnalysisIssue;
    @TableField(typeHandler = ProcessContentTypeHandler.class)
    private CustomerProcessSummaryResponse.ProcessContent approvalAnalysisValue;
    @TableField(typeHandler = ProcessContentTypeHandler.class)
    private CustomerProcessSummaryResponse.ProcessContent approvalAnalysisPrice;
    @TableField(typeHandler = ProcessContentTypeHandler.class)
    private CustomerProcessSummaryResponse.ProcessContent approvalAnalysisPurchase;
    @TableField(typeHandler = ProcessContentTypeHandler.class)
    private CustomerProcessSummaryResponse.ProcessContent invitAttendCourses;
    // 租户Id
    private String tenantId;
    // 预留信息
    private String reservedSummary;
    // 预留属性
    private String reservedProperty;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime  updateTime;
}
