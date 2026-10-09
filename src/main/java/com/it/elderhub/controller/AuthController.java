package com.it.elderhub.controller;

import com.it.elderhub.annotation.RequireLogin;
import com.it.elderhub.common.Result;
import com.it.elderhub.entity.Role;
import com.it.elderhub.entity.User;
import com.it.elderhub.mapper.RoleMapper;
import com.it.elderhub.service.UserService;
import com.it.elderhub.utils.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 前端提交用户名和密码
 *     ↓
 * Service 校验用户是否存在
 *     ↓
 * 校验密码是否正确
 *     ↓
 * 生成 JWT Token
 *     ↓
 * 返回 token、userId、username
 */

@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private UserService userService;
    @Autowired
    private JwtUtil jwtUtil;
    @Autowired
    private RoleMapper roleMapper;

    @PostMapping("/register")
    public Result<Map<String, Object>> register(@RequestBody User user) {
        User registered = userService.register(user.getUsername(), user.getPassword());

        Map<String, Object> data = new HashMap<>();
        data.put("id", registered.getId());
        data.put("username", registered.getUsername());
        return Result.success(data);
    }

    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody User user) {
        User loginUser = userService.login(user.getUsername(), user.getPassword());

        Role role = roleMapper.selectById(loginUser.getRoleId());
        String roleName;

        if(loginUser.getRoleId()==1){
            roleName = "ADMIN";
        }else {
            roleName = "USER";
        }

        if(role!=null){
            roleName = role.getName();
        }

        String token = jwtUtil.generateToken(
                loginUser.getId(),
                loginUser.getUsername(),
                roleName);

        Map<String, Object> data = new HashMap<>();
        data.put("userId", loginUser.getId());
        data.put("username", loginUser.getUsername());
        data.put("token",token);
        data.put("role",roleName);
        // token 后面接 JWT 后再加上
        return Result.success(data);
    }

    @PostMapping("/logout")
    @RequireLogin
    public Result<Void> logout() {
        // JWT 是无状态的，服务端不保存 Token
        // 退出登录只需要前端删除 Token 即可
        // 这里返回成功，前端收到后清除本地存储的 token
        return Result.success(null);
    }
}