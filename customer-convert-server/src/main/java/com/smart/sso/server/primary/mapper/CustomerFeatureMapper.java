package com.smart.sso.server.primary.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smart.sso.server.model.CustomerFeature;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

public interface CustomerFeatureMapper extends BaseMapper<CustomerFeature> {

    @Update("UPDATE customer_feature SET software_purchase_attitude_sales = #{software_purchase_attitude_sales} WHERE id = #{id}")
    int updateSoftwarePurchaseAttitudeSalesById(@Param("id") String id, @Param("software_purchase_attitude_sales") String softwarePurchaseAttitudeSales);

    @Update("UPDATE customer_feature SET funds_volume_sales = #{funds_volume_sales} WHERE id = #{id}")
    int updateFundsVolumeSalesById(@Param("id") String id, @Param("funds_volume_sales") String funds_volume_sales);

}
