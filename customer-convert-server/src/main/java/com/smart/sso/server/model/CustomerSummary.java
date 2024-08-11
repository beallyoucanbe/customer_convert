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
    // 销售有对客户的问题做量化放大
    @TableField(typeHandler = SummaryContentTypeHandler.class)
    private List<SummaryContent> customerIssuesQuantified;
    // 销售有对软件的价值做量化放大
    @TableField(typeHandler = SummaryContentTypeHandler.class)
    private List<SummaryContent> softwareValueQuantified;
    // 方法认可
    @TableField(typeHandler = SummaryContentTypeHandler.class)
    private List<SummaryContent> approvalAnalysisMethod;
    // 问题认可
    @TableField(typeHandler = SummaryContentTypeHandler.class)
    private List<SummaryContent> approvalAnalysisIssue;
    // 价值认可
    @TableField(typeHandler = SummaryContentTypeHandler.class)
    private List<SummaryContent> approvalAnalysisValue;
    // 价格认可
    @TableField(typeHandler = SummaryContentTypeHandler.class)
    private List<SummaryContent> approvalAnalysisPrice;
    // 购买认可
    @TableField(typeHandler = SummaryContentTypeHandler.class)
    private List<SummaryContent> approvalAnalysisPurchase;
    // 客户学不会软件操作
    @TableField(typeHandler = SummaryContentTypeHandler.class)
    private List<SummaryContent> approvalAnalysisSoftwareOperation;
    // 购买认可
    @TableField(typeHandler = SummaryContentTypeHandler.class)
    private List<SummaryContent> approvalAnalysisCourse;
    // 购买认可
    @TableField(typeHandler = SummaryContentTypeHandler.class)
    private List<SummaryContent> approvalAnalysisNoMoney;
    // 购买认可
    @TableField(typeHandler = SummaryContentTypeHandler.class)
    private List<SummaryContent> approvalAnalysisOthers;
    //邀请客户参加课程
    @TableField(typeHandler = SummaryContentTypeHandler.class)
    private List<SummaryContent> invitAttendCourses;
    // 租户Id
    private String tenantId;
    // 预留信息
    private String reservedSummary;
    // 预留属性
    private String reservedProperty;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime  updateTime;
}
