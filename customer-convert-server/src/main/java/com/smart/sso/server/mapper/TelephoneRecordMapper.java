package com.smart.sso.server.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smart.sso.server.model.TelephoneRecord;
import com.smart.sso.server.model.TelephoneRecordStatics;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface TelephoneRecordMapper extends BaseMapper<TelephoneRecord> {

    @Select("SELECT customer_id FROM telephone_record WHERE activity_id = #{activity_id}")
    List<String> selectCustomerIdByActivity(@Param("activity_id") String activity_id);

    @Select("SELECT customer_id, activity_id, COUNT(*) AS total_calls, MAX(communication_time) AS latest_communication_time FROM telephone_record WHERE activity_id = #{activity_id} GROUP BY customer_id, activity_id")
    List<TelephoneRecordStatics> selectTelephoneRecordStatics(@Param("activity_id") String activity_id);
}
