package com.smart.sso.server.primary.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smart.sso.server.model.Events;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface EventsMapper extends BaseMapper<Events> {

    @Select("select * from events where user_id = #{user_id} and event_name = #{event_name}")
    List<Events> getEventsByUserIdAndEventName(@Param("user_id") int user_id,
                                               @Param("event_name") String event_name);

}
