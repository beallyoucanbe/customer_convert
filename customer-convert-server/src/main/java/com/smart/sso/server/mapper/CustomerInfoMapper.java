package com.smart.sso.server.mapper;

import com.smart.sso.server.model.ActivityInfo;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smart.sso.server.model.CustomerInfo;

import java.sql.Timestamp;
import java.util.List;

public interface CustomerInfoMapper extends BaseMapper<CustomerInfo> {
    @Update("UPDATE customer_info SET conversion_rate = #{conversion_rate} WHERE id = #{id}")
    int updateConversionRateById(@Param("id") String id, @Param("conversion_rate") String conversionRate);

    @Select("select * from customer_info where customer_id = #{customer_id} and activity_id = #{activity_id}")
    CustomerInfo selectByCustomerIdAndCampaignId(@Param("customer_id") String customer_id, @Param("activity_id") String activity_id);

    @Select("SELECT activity_name AS activityName, activity_id AS activityId FROM customer_info WHERE customer_id = #{customer_id}")
    List<ActivityInfo> selectActivityInfoByCustomerId(@Param("customer_id") String customer_id);

    @Update("UPDATE customer_info SET communication_rounds = #{communication_rounds}, update_time = #{update_time} WHERE customer_id = #{customer_id} and activity_id = #{activity_id}")
    int updateCommunicationRounds(@Param("customer_id") String customer_id,
                                  @Param("activity_id") String activity_id,
                                  @Param("communication_rounds") Integer communication_rounds,
                                  @Param("update_time") Timestamp update_time);

}
