package com.smart.sso.server.primary.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smart.sso.server.model.Message;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

public interface MessageMapper extends BaseMapper<Message> {

    @Update("UPDATE message SET status = #{status} WHERE id = #{id}")
    int updateStatusById(@Param("id") String id, @Param("status") int status);

}
