package com.it.elderhub.controller;

import com.it.elderhub.common.Result;
import com.it.elderhub.entity.Role;
import com.it.elderhub.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/admin/role")
public class RoleController {
    @Autowired
    private UserService userService;

    @GetMapping("/list")
    public Result<List<Role>> listRoles() {
        return Result.success(userService.listRoles());
    }
}

