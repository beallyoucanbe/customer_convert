package com.smart.sso.server.primary.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smart.sso.server.model.CustomerDoubt;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

public interface CustomerDoubtMapper extends BaseMapper<CustomerDoubt> {

    @Select("SELECT * FROM customer_doubt WHERE activity_id = #{activity_id} and communication_time > #{start_time} and communication_time < #{end_time}")
    List<CustomerDoubt> selectByActivityIdAndTime(@Param("activity_id") String activity_id,
                                                  @Param("start_time") LocalDateTime start_time,
                                                  @Param("end_time") LocalDateTime end_time);


}
