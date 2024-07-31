package com.smart.sso.server.service.impl;

import java.util.Objects;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.smart.sso.server.mapper.UserMapper;
import com.smart.sso.server.mapper.UserRoleMapper;
import com.smart.sso.server.session.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.smart.sso.client.rpc.Result;
import com.smart.sso.client.rpc.SsoUser;
import com.smart.sso.server.service.UserService;

@Service
@Slf4j
public class UserServiceImpl implements UserService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private UserRoleMapper userRoleMapper;

    @Override
    public SsoUser login(String username, String password) {
        // 根据username 查询
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("username", username);
        User user = userMapper.selectOne(queryWrapper);
        // 判断用户是否正确
        if (Objects.isNull(user)) {
            log.error("用户不存在");
            throw new RuntimeException("用户不存在");
        }
        // 检查密码是否在正确
        if (!user.getPassword().equals(password)) {
            log.error("用户名密码错误");
            throw new RuntimeException("用户名密码错误");
        }
        // 查询用户的角色
        String userRole = userRoleMapper.getUserRole(user.getId());
        return new SsoUser(user.getId(), user.getUsername(), userRole);
    }
}
