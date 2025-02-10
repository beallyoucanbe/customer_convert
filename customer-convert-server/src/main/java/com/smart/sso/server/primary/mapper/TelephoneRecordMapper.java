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


    @Select("SELECT customer_id, activity_id, COUNT(*) AS total_calls, MAX(communication_time) AS latest_communication_time FROM telephone_record WHERE activity_id = #{activity_id} and update_time > #{update_time} GROUP BY customer_id, activity_id")
    List<TelephoneRecordStatics> selectTelephoneRecordStaticsRecent(@Param("activity_id") String activity_id,
                                                                    @Param("update_time") LocalDateTime update_time);



    @Select("SELECT customer_id, activity_id, COUNT(*) AS total_calls, MAX(communication_time) AS latest_communication_time FROM telephone_record WHERE customer_id = #{customer_id} and activity_id = #{activity_id}")
    TelephoneRecordStatics selectTelephoneRecordStaticsOne(@Param("customer_id") String customer_id, @Param("activity_id") String activity_id);

    /**
     * 在某个时间段内有打过电话的销售人员名单
     * @param activity_id
     * @param start_time
     * @param end_time
     * @return
     */
    @Select("SELECT DISTINCT owner_id FROM telephone_record WHERE activity_id = #{activity_id} and communication_time > #{start_time} and communication_time < #{end_time}")
    List<String> selectOwnerHasTele(@Param("activity_id") String activity_id,
                                    @Param("start_time") LocalDateTime start_time,
                                    @Param("end_time") LocalDateTime end_time);

    /**
     * 获取在某个时间段内某个销售的所有通话
     * @param owner_id
     * @param start_time
     * @param end_time
     * @return
     */
    @Select("SELECT * FROM telephone_record WHERE owner_id = #{owner_id} and communication_time > #{start_time} and communication_time < #{end_time}")
    List<TelephoneRecord> selectOwnerTelephoneRecord(@Param("owner_id") String owner_id,
                                    @Param("start_time") LocalDateTime start_time,
                                    @Param("end_time") LocalDateTime end_time);

    /**
     * 查询累计通话超过8小时的客户
     * @return
     */
    @Select("SELECT customer_id FROM telephone_record WHERE activity_id = #{activity_id} group by customer_id HAVING SUM(communication_duration) > 240")
    List<String> selectCustomerExceed8Hour(@Param("activity_id") String activity_id);
}
