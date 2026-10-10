package com.it.elderhub.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.it.elderhub.dto.UserAddDTO;
import com.it.elderhub.dto.UserQuery;
import com.it.elderhub.dto.UserUpdateDTO;
import com.it.elderhub.entity.Menu;
import com.it.elderhub.entity.Role;
import com.it.elderhub.entity.User;
import com.baomidou.mybatisplus.extension.service.IService;
import com.it.elderhub.vo.UserVO;

import java.util.List;

/**
* @author Ljz
* @description 针对表【user(系统用户表)】的数据库操作Service
* @createDate 2026-10-08 09:52:06
*/
public interface UserService extends IService<User> {

    User register(String username,String password);

    User login(String username,String password);

    IPage<UserVO> listUsers(UserQuery query);

    UserVO getUserById(Integer id);

    void saveUser(UserAddDTO dto);

    void updateUser(UserUpdateDTO dto);

    void removeUser(Integer id);

    List<Role> listRoles();

    List<Menu> listMenuTree();

    List<UserVO> listNurses(String nickname);



}
