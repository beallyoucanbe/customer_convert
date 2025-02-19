package com.smart.sso.server.primary.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smart.sso.server.model.CustomerCharacter;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

public interface CustomerCharacterMapper extends BaseMapper<CustomerCharacter> {


    @Select("select * from customer_character where customer_id = #{customer_id} and activity_id = #{activity_id}")
    CustomerCharacter selectByCustomerIdAndActivityId(@Param("customer_id") String customer_id, @Param("activity_id") String activity_id);

    @Update({
            "UPDATE customer_character SET ",
            "customer_id=#{customerId}, ",
            "customer_name=#{customerName}, ",
            "owner_id=#{ownerId}, ",
            "owner_name=#{ownerName}, ",
            "activity_id=#{activityId}, ",
            "activity_name=#{activityName}, ",
            "conversion_rate=#{conversionRate}, ",
            "matching_judgment_stage=#{matchingJudgmentStage}, ",
            "transaction_style_stage=#{transactionStyleStage}, ",
            "function_introduction_stage=#{functionIntroductionStage}, ",
            "confirm_value_stage=#{confirmValueStage}, ",
            "confirm_purchase_stage=#{confirmPurchaseStage}, ",
            "complete_purchase_stage=#{completePurchaseStage}, ",
            "funds_volume=#{fundsVolume}, ",
            "earning_desire=#{earningDesire}, ",
            "software_function_clarity=#{softwareFunctionClarity}, ",
            "stock_selection_method=#{stockSelectionMethod}, ",
            "self_issue_recognition=#{selfIssueRecognition}, ",
            "software_value_approval=#{softwareValueApproval}, ",
            "software_purchase_attitude=#{softwarePurchaseAttitude}, ",
            "summary_match_judgment=#{summaryMatchJudgment}, ",
            "summary_transaction_style=#{summaryTransactionStyle}, ",
            "summary_function_introduction=#{summaryFunctionIntroduction}, ",
            "summary_confirm_value=#{summaryConfirmValue}, ",
            "issues_value_quantified=#{issuesValueQuantified}, ",
            "customer_continue_communicate=#{customerContinueCommunicate}, ",
            "owner_packaging_course=#{ownerPackagingCourse}, ",
            "owner_packaging_function=#{ownerPackagingFunction}, ",
            "is_send188=#{isSend188}, ",
            "cust_type_id=#{custTypeId}, ",
            "update_time=#{updateTime} ",
            "WHERE id=#{id}"
    })
    int updateAllFields(CustomerCharacter customerCharacter);

}
