package com.it.elderhub.controller;

import com.it.elderhub.common.Result;
import com.it.elderhub.entity.Menu;
import com.it.elderhub.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/admin/menu")
public class MenuController {
    @Autowired
    private UserService userService;

    @GetMapping("/tree")
    public Result<List<Menu>> listMenuTree() {
        return Result.success(userService.listMenuTree());
    }
}