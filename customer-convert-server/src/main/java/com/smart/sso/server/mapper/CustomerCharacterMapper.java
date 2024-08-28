package com.smart.sso.server.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smart.sso.server.model.CustomerCharacter;
import org.apache.ibatis.annotations.Update;

public interface CustomerCharacterMapper extends BaseMapper<CustomerCharacter> {

    @Update({
            "UPDATE customer_character SET ",
            "customer_id=#{customerId}, ",
            "customer_name=#{customerName}, ",
            "owner_id=#{ownerId}, ",
            "owner_name=#{ownerName}, ",
            "current_campaign=#{currentCampaign}, ",
            "conversion_rate=#{conversionRate}, ",
            "matching_judgment_stage=#{matchingJudgmentStage}, ",
            "transaction_style_stage=#{transactionStyleStage}, ",
            "function_introduction_stage=#{functionIntroductionStage}, ",
            "confirm_value_stage=#{confirmValueStage}, ",
            "confirm_purchase_stage=#{confirmPurchaseStage}, ",
            "complete_purchase_stage=#{completePurchaseStage}, ",
            "funds_volume=#{fundsVolume}, ",
            "profit_loss_situation=#{profitLossSituation}, ",
            "earning_desire=#{earningDesire}, ",
            "course_teacher_approval=#{courseTeacherApproval}, ",
            "software_function_clarity=#{softwareFunctionClarity}, ",
            "stock_selection_method=#{stockSelectionMethod}, ",
            "self_issue_recognition=#{selfIssueRecognition}, ",
            "software_value_approval=#{softwareValueApproval}, ",
            "software_purchase_attitude=#{softwarePurchaseAttitude}, ",
            "continuous_learn_approval=#{continuousLearnApproval}, ",
            "learn_new_method_approval=#{learnNewMethodApproval}, ",
            "summary_match_judgment=#{summaryMatchJudgment}, ",
            "summary_transaction_style=#{summaryTransactionStyle}, ",
            "summary_follow_customer=#{summaryFollowCustomer}, ",
            "summary_function_introduction=#{summaryFunctionIntroduction}, ",
            "summary_confirm_value=#{summaryConfirmValue}, ",
            "summary_execute_order=#{summaryExecuteOrder}, ",
            "summary_invit_course=#{summaryInvitCourse}, ",
            "question_count=#{questionCount}, ",
            "issues_value_quantified=#{issuesValueQuantified}, ",
            "doubt_frequent=#{doubtFrequent}, ",
            "update_time=#{updateTime} ",
            "WHERE id=#{id}"
    })
    int updateAllFields(CustomerCharacter customerCharacter);

}
