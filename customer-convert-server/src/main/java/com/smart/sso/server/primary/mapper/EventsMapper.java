package com.smart.sso.server.primary.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smart.sso.server.model.Events;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface EventsMapper extends BaseMapper<Events> {

    @Select("select * from events where user_id = #{user_id} and event_name = #{event_name} and action_type = #{action_type} order by event_time desc")
    List<Events> getEventsByUserIdAndEventNameActionType(@Param("user_id") int user_id,
                                                         @Param("event_name") String event_name,
                                                         @Param("action_type") String action_type);

    @Select("select * from events where user_id = #{user_id} and event_name = #{event_name} and action_type = #{action_type} and class_type = #{class_type} order by event_time desc")
    List<Events> getEventsByUserIdAndEventNameActionTypeClassType(@Param("user_id") int user_id,
                                                                  @Param("event_name") String event_name,
                                                                  @Param("action_type") String action_type,
                                                                  @Param("class_type") String class_type);

    @Select("select * from events where user_id = #{user_id} and event_name = #{event_name} and action_type = #{action_type} and action_content like concat('%',#{action_content},'%') order by event_time desc")
    List<Events> getEventsByUserIdAndEventNameActionTypeActionContent(@Param("user_id") int user_id,
                                                                      @Param("event_name") String event_name,
                                                                      @Param("action_type") String action_type,
                                                                      @Param("action_content") String action_content);

    @Select("select count(1) from events where user_id = #{user_id} and event_name = #{event_name} and action_type = #{action_type} order by event_time desc")
    int getCountByUserIdAndEventNameActionType(@Param("user_id") int user_id,
                                               @Param("event_name") String event_name,
                                               @Param("action_type") String action_type);

}
