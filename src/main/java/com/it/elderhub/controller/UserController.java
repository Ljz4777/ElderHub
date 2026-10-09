package com.it.elderhub.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.it.elderhub.dto.UserAddDTO;
import com.it.elderhub.dto.UserUpdateDTO;
import com.it.elderhub.entity.User;
import com.it.elderhub.query.UserQuery;
import com.it.elderhub.service.UserService;
import com.it.elderhub.vo.UserVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user")
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

    /**
     * 用户分页查询
     */
    @GetMapping("/page")
    public IPage<UserVO> page(UserQuery query) {
        return userService.getUserPage(query);
    }

    /**
     * 新增用户
     */
    @PostMapping("/add")
    public void add(@RequestBody UserAddDTO userAddDTO) {
        userService.addUser(userAddDTO);
    }

    /**
     * 修改用户
     */
    @PutMapping("/update")
    public void update(@RequestBody UserUpdateDTO userUpdateDTO) {
        userService.updateUser(userUpdateDTO);
    }

    /**
     * 删除用户
     */
    @DeleteMapping("/{id}")
    public void delete(@PathVariable Integer id) {
        userService.deleteUser(id);
    }

    /**
     * 根据id查询用户详情
     */
    @GetMapping("/{id}")
    public UserVO getInfo(@PathVariable Integer id) {
        return userService.getUserById(id);
    }
}
