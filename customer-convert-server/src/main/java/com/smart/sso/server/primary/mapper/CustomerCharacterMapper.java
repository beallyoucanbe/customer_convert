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
            "class_attend_times_2=#{classAttendTimes_2}, ",
            "class_attend_times_3=#{classAttendTimes_3}, ",
            "class_attend_duration_2=#{classAttendDuration_2}, ",
            "class_attend_duration_3=#{classAttendDuration_3}, ",
            "funds_volume=#{fundsVolume}, ",
            "stock_position=#{stockPosition}, ",
            "trading_style=#{tradingStyle}, ",
            "customer_response=#{customerResponse}, ",
            "latest_time_customer_response=#{latestTimeCustomerResponse}, ",
            "purchase_similar_product=#{purchaseSimilarProduct}, ",
            "member_stocks_buy=#{memberStocksBuy}, ",
            "member_stocks_price=#{memberStocksPrice}, ",
            "welfare_stocks_buy=#{welfareStocksBuy}, ",
            "welfare_stocks_price=#{welfareStocksPrice}, ",
            "consulting_practical_class=#{consultingPracticalClass}, ",
            "customer_learning_freq=#{customerLearningFreq}, ",
            "continue_following_stock=#{continueFollowingStock}, ",
            "teacher_approval=#{teacherApproval}, ",
            "teacher_profession=#{teacherProfession}, ",
            "software_value_approval=#{softwareValueApproval}, ",
            "software_purchase_attitude=#{softwarePurchaseAttitude}, ",
            "create_time=#{createTime}, ",
            "update_time=#{updateTime} ",
            "WHERE id=#{id}"
    })
    int updateAllFields(CustomerCharacter customerCharacter);

}
