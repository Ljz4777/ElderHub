package com.it.elderhub.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.it.elderhub.common.Result;
import com.it.elderhub.dto.UserAddDTO;
import com.it.elderhub.dto.UserQuery;
import com.it.elderhub.dto.UserUpdateDTO;
import com.it.elderhub.entity.User;
import com.it.elderhub.service.UserService;
import com.it.elderhub.vo.UserVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/user")
public class UserController {

    @Autowired
    private UserService userService;

    @PostMapping("/login")
    public String login(@RequestBody User user){
        User loginUser = userService.login(user.getUsername(), user.getPassword());
        if(loginUser!=null){
            return "登录成功";
        }else {
            return "登录失败";
        }
    }
    @GetMapping("/list")
    public Result<IPage<UserVO>> listUsers(UserQuery query) {
        return Result.success(userService.listUsers(query));
    }

    @GetMapping("/{id}")
    public Result<UserVO> getUserById(@PathVariable Integer id) {
        return Result.success(userService.getUserById(id));
    }

    @PostMapping("/save")
    public Result<Void> saveUser(@RequestBody UserAddDTO dto) {
        userService.saveUser(dto);
        return Result.success(null);
    }

    @PutMapping("/update")
    public Result<Void> updateUser(@RequestBody UserUpdateDTO dto) {
        userService.updateUser(dto);
        return Result.success(null);
    }

    @DeleteMapping("/remove/{id}")
    public Result<Void> removeUser(@PathVariable Integer id) {
        userService.removeUser(id);
        return Result.success(null);
    }

    @GetMapping("/nurse-list")
    public Result<List<UserVO>> listNurses(@RequestParam(required = false) String nickname) {
        return Result.success(userService.listNurses(nickname));
    }
}
