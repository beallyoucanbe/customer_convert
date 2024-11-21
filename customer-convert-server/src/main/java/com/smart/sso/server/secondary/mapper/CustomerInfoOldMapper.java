package com.smart.sso.server.secondary.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smart.sso.server.model.CustomerInfoOld;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
public interface CustomerInfoOldMapper extends BaseMapper<CustomerInfoOld> {
    @Select("select current_campaign from customer_info where customer_id = #{customer_id}")
    String selectActivityByCustomerId(@Param("customer_id") String customer_id);

}
