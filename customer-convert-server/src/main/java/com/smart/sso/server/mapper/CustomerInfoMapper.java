package com.smart.sso.server.mapper;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smart.sso.server.model.CustomerInfo;

public interface CustomerInfoMapper extends BaseMapper<CustomerInfo> {
    @Update("UPDATE customer_info SET conversion_rate = #{conversion_rate} WHERE id = #{id}")
    int updateConversionRateById(@Param("id") String id, @Param("conversion_rate") String conversionRate);

    @Select("select * from customer_info where customer_id = #{customer_id} and current_campaign = #{current_campaign}")
    CustomerInfo selectByCustomerIdAndCampaignId(@Param("customer_id") String customer_id, @Param("current_campaign") String current_campaign);

}
