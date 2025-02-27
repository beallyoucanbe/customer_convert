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
            "has_time=#{hasTime}, ",
            "complete_intro=#{completeIntro}, ",
            "complete_stock_info=#{completeStockInfo}, ",
            "remind_live_freq=#{remindLiveFreq}, ",
            "remind_community_freq=#{remindCommunityFreq}, ",
            "visit_live_freq=#{visitLiveFreq}, ",
            "visit_community_freq=#{visitCommunityFreq}, ",
            "function_freq=#{functionFreq}, ",
            "customer_refund_status=#{customerRefundStatus}, ",
            "refund_time=#{refundTime}, ",
            "latest_time_visit_live=#{latestTimeVisitLive}, ",
            "latest_time_remind_live=#{latestTimeRemindLive}, ",
            "latest_time_visit_community=#{latestTimeVisitCommunity}, ",
            "latest_time_remind_community=#{latestTimeRemindCommunity}, ",
            "latest_time_use_function=#{latestTimeUseFunction}, ",
            "time_add_customer=#{timeAddCustomer}, ",
            "latest_time_telephone=#{latestTimeTelephone}, ",
            "delivery_remind_live_freq=#{deliveryRemindLiveFreq}, ",
            "delivery_remind_callback_freq=#{deliveryRemindCallbackFreq}, ",
            "course1=#{course1}, ",
            "course2=#{course2}, ",
            "course3=#{course3}, ",
            "course4=#{course4}, ",
            "course5=#{course5}, ",
            "course6=#{course6}, ",
            "course7=#{course7}, ",
            "course8=#{course8}, ",
            "course9=#{course9}, ",
            "course10=#{course10}, ",
            "course11=#{course11}, ",
            "course12=#{course12}, ",
            "course13=#{course13}, ",
            "course14=#{course14}, ",
            "course15=#{course15}, ",
            "teacher_profession=#{teacherProfession}, ",
            "teacher_approve=#{teacherApprove}, ",
            "course_processed=#{courseProcessed}, ",
            "course_correct=#{courseCorrect}, ",
            "latest_time_task_interaction=#{latestTimeTaskInteraction}, ",
            "delivery_task_interaction_freq=#{deliveryTaskInteractionFreq}, ",
            "task1=#{task1}, ",
            "task2=#{task2}, ",
            "task3=#{task3}, ",
            "task4=#{task4}, ",
            "task5=#{task5}, ",
            "task6=#{task6}, ",
            "task7=#{task7}, ",
            "task8=#{task8}, ",
            "task9=#{task9}, ",
            "task10=#{task10}, ",
            "task11=#{task11}, ",
            "task12=#{task12}, ",
            "task13=#{task13}, ",
            "task14=#{task14}, ",
            "task15=#{task15} ",
            "WHERE id=#{id}"
    })
    int updateAllFields(CustomerCharacter customerCharacter);

}
