package com.it.elderhub.service;

import com.it.elderhub.dto.UserAddDTO;
import com.it.elderhub.dto.UserUpdateDTO;
import com.it.elderhub.query.UserQuery;
import com.it.elderhub.vo.UserVO;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.it.elderhub.entity.User;

public interface UserService extends IService<User> {

    /**
     * 用户分页列表
     */
    IPage<UserVO> getUserPage(UserQuery query);

    /**
     * 新增用户
     */
    void addUser(UserAddDTO userAddDTO);

    /**
     * 修改用户
     */
    void updateUser(UserUpdateDTO userUpdateDTO);

    /**
     * 删除用户
     */
    void deleteUser(Integer id);

    /**
     * 根据id查询用户详情
     */
    UserVO getUserById(Integer id);
    User login(String username, String password);
    User register(String username, String password);
}
