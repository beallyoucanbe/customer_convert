package com.smart.sso.server.mapper;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smart.sso.server.model.CustomerInfo;

public interface CustomerInfoMapper extends BaseMapper<CustomerInfo> {
    @Update("UPDATE customer_info SET conversion_rate = #{conversion_rate} WHERE id = #{id}")
    int updateConversionRateById(@Param("id") String id, @Param("conversion_rate") String conversionRate);

}
