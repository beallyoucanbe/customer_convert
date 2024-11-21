package com.smart.sso.server.primary.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smart.sso.server.model.ScheduledTask;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

public interface ScheduledTasksMapper extends BaseMapper<ScheduledTask> {

    @Update("UPDATE scheduled_task SET status = #{status} WHERE id = #{id}")
    int updateStatusById(@Param("id") String id, @Param("status") String status);
}
