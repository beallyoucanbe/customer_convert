package com.smart.sso.server.model;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName(autoResultMap = true)
public class CharacterCostTime {
    private String id; // 通话id
    private String customerId; // 客户id
    private String customerName; // 客户名称
    private String ownerId; // 销售id
    private String ownerName; // 销售名称
    private Integer communicationDurationFundsVolume; // 资金体量通话时长
    private Integer communicationRoundFundsVolume; // 资金体量通话
    private Integer communicationDurationFearningDesire; // 赚钱欲望通话时长
    private Integer communicationRoundEarningDesire; // 赚钱欲望通话轮次
    private Integer communicationDurationCourseTeacherApproval; // 课程和老师的认可度通话时长
    private Integer communicationRoundCourseTeacherApproval; // 课程和老师的认可度通话轮次
    private Integer communicationDurationSoftwareFunctionClarity; // 软件功能的清晰度通话时长
    private Integer communicationRoundSoftwareFunctionClarity; // 软件功能的清晰度通话轮次
    private Integer communicationDurationStockSelectionMethod; // 选股方法的认可度通话时长
    private Integer communicationRoundStockSelectionMethod; // 选股方法的认可度通话轮次
    private Integer communicationDurationSelfIssueRecognition; // 自身问题及影响的认可度通话时长
    private Integer communicationRoundSelfIssueRecognition; // 自身问题及影响的认可度通话轮次
    private Integer communicationDurationLearnNewMethodApproval; // 学习新方法认可通话时长
    private Integer communicationRoundLearnNewMethodApproval; // 学习新方法认可通话轮次
    private Integer communicationDurationContinuousLearnApproval; // 持续学习的意愿通话时长
    private Integer communicationRoundContinuousLearnApproval; // 持续学习的意愿通话轮次
    private Integer communicationDurationSoftwareValueApproval; // 软件价值的认可度通话时长
    private Integer communicationRoundSoftwareValueApproval; // 软软件价值的认可度通话轮次
    private Integer communicationDurationSoftwarePurchaseAttitude; // 购买软件的态度通话时长
    private Integer communicationRoundSoftwarePurchaseAttitude; // 购买软件的态度通话轮次
    private Integer communicationDurationCustomerIssuesQuantified; // 问题做量化放大通话时长
    private Integer communicationRoundCustomerIssuesQuantified; // 问题做量化放大度通话轮次
    private Integer communicationDurationSoftwareValueQuantified; // 软件的价值做量化通话时长
    private Integer communicationRoundSoftwareValueQuantified; // 软件的价值做量化通话轮次
    private LocalDateTime createTime; // 创建时间
    private LocalDateTime updateTime; // 更新时间
}