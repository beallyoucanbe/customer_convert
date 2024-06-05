package com.smart.sso.server.model;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.smart.sso.server.handler.CommaSeparatedStringTypeHandler;
import com.smart.sso.server.handler.ProcessContentTypeHandler;
import com.smart.sso.server.handler.ProcessInfoExplanationTypeHandler;
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
    @TableField(typeHandler = ProcessInfoExplanationTypeHandler.class)
    private CustomerProcessSummaryResponse.ProcessInfoExplanation infoExplanation;
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
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime  updateTime;
}
