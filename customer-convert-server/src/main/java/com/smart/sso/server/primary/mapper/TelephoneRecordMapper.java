package com.smart.sso.server.primary.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smart.sso.server.model.TelephoneRecord;
import com.smart.sso.server.model.TelephoneRecordStatics;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

public interface TelephoneRecordMapper extends BaseMapper<TelephoneRecord> {

    @Select("SELECT * FROM telephone_record WHERE customer_id = #{customer_id} and activity_id = #{activity_id} limit 1")
    TelephoneRecord selectOneTelephoneRecord(@Param("customer_id") String customer_id, @Param("activity_id") String activity_id);

    @Select("SELECT customer_id, activity_id, COUNT(*) AS total_calls, MAX(communication_time) AS latest_communication_time FROM telephone_record WHERE activity_id = #{activity_id} GROUP BY customer_id, activity_id")
    List<TelephoneRecordStatics> selectTelephoneRecordStatics(@Param("activity_id") String activity_id);


    @Select("SELECT customer_id, activity_id, COUNT(*) AS total_calls, MAX(communication_time) AS latest_communication_time FROM telephone_record WHERE update_time > #{update_time} GROUP BY customer_id, activity_id")
    List<TelephoneRecordStatics> selectTelephoneRecordStaticsRecent(@Param("update_time") LocalDateTime update_time);



    @Select("SELECT customer_id, activity_id, COUNT(*) AS total_calls, MAX(communication_time) AS latest_communication_time FROM telephone_record WHERE customer_id = #{customer_id} and activity_id = #{activity_id}")
    TelephoneRecordStatics selectTelephoneRecordStaticsOne(@Param("customer_id") String customer_id, @Param("activity_id") String activity_id);

}
