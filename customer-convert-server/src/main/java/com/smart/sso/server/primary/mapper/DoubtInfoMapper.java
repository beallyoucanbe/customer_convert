package com.smart.sso.server.primary.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smart.sso.server.model.DoubtInfo;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface DoubtInfoMapper extends BaseMapper<DoubtInfo> {

    @Select("SELECT * FROM doubt_info WHERE norm_doubt = #{norm_doubt}")
    List<DoubtInfo> selectByNormDoubt(@Param("norm_doubt") String norm_doubt);

}
