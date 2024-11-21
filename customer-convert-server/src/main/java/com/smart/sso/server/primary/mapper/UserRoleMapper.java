package com.smart.sso.server.primary.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smart.sso.server.session.UserRole;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface UserRoleMapper extends BaseMapper<UserRole> {

    @Select("SELECT role FROM user_role LEFT JOIN role ON user_role.role_id = role.id WHERE user_role.role_id = #{id}")
    String getUserRole(@Param("id") String id);

}
