package com.smart.sso.server.service.impl;

import java.util.Objects;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.smart.sso.server.primary.mapper.RoleMapper;
import com.smart.sso.server.primary.mapper.UserMapper;
import com.smart.sso.server.primary.mapper.UserRoleMapper;
import com.smart.sso.server.session.Role;
import com.smart.sso.server.session.User;
import com.smart.sso.server.session.UserRole;
import com.smart.sso.server.util.CommonUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.smart.sso.client.rpc.SsoUser;
import com.smart.sso.server.service.UserService;

@Service
@Slf4j
public class UserServiceImpl implements UserService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private UserRoleMapper userRoleMapper;

    @Autowired
    private RoleMapper roleMapper;

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

    @Override
    public String signup(String username, String password, String roleName) {
        // 插入新用户
        User newUser = new User();
        newUser.setId(CommonUtils.generatePrimaryKey());
        newUser.setUsername(username);
        newUser.setPassword(password);
        userMapper.insert(newUser);
        // 查询角色id
        QueryWrapper<Role> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("role", roleName);
        Role role = roleMapper.selectOne(queryWrapper);
        if (Objects.isNull(role)) {
            throw new RuntimeException("用户角色不存在");
        }
        // 写入角色
        UserRole userRole = new UserRole();
        userRole.setUser_id(newUser.getId());
        userRole.setRole_id(role.getId());
        userRoleMapper.insert(userRole);
        return newUser.getId();
    }

    @Override
    public SsoUser getUserById(String userId) {
        User user = userMapper.selectById(userId);
        String userRole = userRoleMapper.getUserRole(user.getId());
        return new SsoUser(user.getId(), user.getUsername(), userRole);
    }

    @Override
    public Role signRole(String role, String roleName, String permission) {
        Role newRole = new Role();
        newRole.setId(CommonUtils.generatePrimaryKey());
        newRole.setRole(role);
        newRole.setRoleName(roleName);
        newRole.setPermission(permission);
        roleMapper.insert(newRole);
        return roleMapper.selectById(newRole.getId());
    }
}
