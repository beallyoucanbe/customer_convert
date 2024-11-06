package com.smart.sso.server.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smart.sso.server.model.TelephoneRecord;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface TelephoneRecordMapper extends BaseMapper<TelephoneRecord> {

    @Select("SELECT customer_id FROM telephone_record WHERE activity_id = #{activity_id}")
    List<String> selectCustomerIdByActivity(@Param("activity_id") String activity_id);
}
