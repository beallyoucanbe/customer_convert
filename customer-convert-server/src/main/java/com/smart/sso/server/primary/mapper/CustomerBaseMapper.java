package com.smart.sso.server.primary.mapper;

import com.smart.sso.server.model.ActivityInfo;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smart.sso.server.model.CustomerBase;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

public interface CustomerBaseMapper extends BaseMapper<CustomerBase> {
    @Update("UPDATE customer_base SET conversion_rate = #{conversion_rate} WHERE id = #{id}")
    int updateConversionRateById(@Param("id") String id, @Param("conversion_rate") String conversionRate);

    @Select("select * from customer_base where customer_id = #{customer_id} and activity_id = #{activity_id}")
    CustomerBase selectByCustomerIdAndCampaignId(@Param("customer_id") String customer_id, @Param("activity_id") String activity_id);

    @Select("SELECT activity_name AS activityName, activity_id AS activityId FROM customer_base WHERE customer_id = #{customer_id}")
    List<ActivityInfo> selectActivityInfoByCustomerId(@Param("customer_id") String customer_id);

    @Update("UPDATE customer_base SET communication_rounds = #{communication_rounds}, update_time = #{update_time} WHERE customer_id = #{customer_id} and activity_id = #{activity_id}")
    int updateCommunicationRounds(@Param("customer_id") String customer_id,
                                  @Param("activity_id") String activity_id,
                                  @Param("communication_rounds") Integer communication_rounds,
                                  @Param("update_time") Timestamp update_time);

    @Update("UPDATE customer_base SET purchase_time = #{purchase_time}, customer_purchase_status = 1 WHERE id = #{id}")
    int updatePurchaseTimeById(@Param("id") String id, @Param("purchase_time") LocalDateTime purchase_time);

    @Update("UPDATE customer_base SET owner_id = #{owner_id}, owner_name = #{owner_name} WHERE id = #{id}")
    int updateSalesById(@Param("id") String id,
                        @Param("owner_id") String owner_id,
                        @Param("owner_name") String owner_name);

}
