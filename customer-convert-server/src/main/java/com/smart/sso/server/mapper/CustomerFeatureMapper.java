package com.smart.sso.server.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smart.sso.server.model.CustomerFeature;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

public interface CustomerFeatureMapper extends BaseMapper<CustomerFeature> {

    @Update("UPDATE customer_feature SET software_purchase_attitude_sales = #{software_purchase_attitude_sales} WHERE id = #{id}")
    int updateSoftwarePurchaseAttitudeSalesById(@Param("id") String id, @Param("software_purchase_attitude_sales") String softwarePurchaseAttitudeSales);

}
