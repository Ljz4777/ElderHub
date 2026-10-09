package com.it.elderhub.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.it.elderhub.dto.UserAddDTO;
import com.it.elderhub.dto.UserUpdateDTO;
import com.it.elderhub.entity.User;
import com.it.elderhub.mapper.UserMapper;
import com.it.elderhub.query.UserQuery;
import com.it.elderhub.service.UserService;
import com.it.elderhub.vo.UserVO;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    @Autowired
    private UserMapper userMapper;

    @Override
    public User register(String username, String password) {
        User user = new User();
        user.setUsername(username);
        user.setPassword(password);
        userMapper.insert(user);
        return user;
    }

    @Override
    public IPage<UserVO> getUserPage(UserQuery query) {
        Page<User> page = new Page<>(query.getPageNum(), query.getPageSize());
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        // 用户名模糊查询
        if(StringUtils.hasText(query.getUsername())){
            wrapper.like(User::getUsername, query.getUsername());
        }
        // 角色id条件
        if(query.getRoleId() != null){
            wrapper.eq(User::getRoleId, query.getRoleId());
        }
        // 只查未删除
        wrapper.eq(User::getIsDeleted, 0);
        Page<User> userPage = userMapper.selectPage(page, wrapper);
        return userPage.convert(this::toVO);
    }

    @Override
    public void addUser(UserAddDTO userAddDTO) {
        User user = new User();
        user.setUsername(userAddDTO.getUsername());
        user.setPassword(userAddDTO.getPassword());
        user.setNickname(userAddDTO.getNickname());
        user.setSex(userAddDTO.getSex());
        user.setPhoneNumber(userAddDTO.getPhoneNumber());
        user.setEmail(userAddDTO.getEmail());
        user.setRoleId(userAddDTO.getRoleId());
        user.setIsDeleted(0);
        save(user);
    }

    @Override
    public void updateUser(UserUpdateDTO userUpdateDTO) {
        User user = getById(userUpdateDTO.getId());
        if(user == null){
            throw new RuntimeException("用户不存在");
        }
        user.setNickname(userUpdateDTO.getNickname());
        user.setSex(userUpdateDTO.getSex());
        user.setPhoneNumber(userUpdateDTO.getPhoneNumber());
        user.setEmail(userUpdateDTO.getEmail());
        user.setRoleId(userUpdateDTO.getRoleId());
        updateById(user);
    }

    @Override
    public void deleteUser(Integer id) {
        User user = getById(id);
        if(user == null){
            throw new RuntimeException("用户不存在");
        }
        // 逻辑删除，标记is_deleted=1
        user.setIsDeleted(1);
        updateById(user);
    }

    @Override
    public UserVO getUserById(Integer id) {
        User user = getById(id);
        if(user == null || user.getIsDeleted() == 1){
            return null;
        }
        return toVO(user);
    }

    @Override
    public User login(String username, String password) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, username)
                .eq(User::getPassword, password)
                .eq(User::getIsDeleted,0);
        return getOne(wrapper);
    }

    // entity转VO
    private UserVO toVO(User user){
        UserVO vo = new UserVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setSex(user.getSex());
        vo.setPhoneNumber(user.getPhoneNumber());
        vo.setEmail(user.getEmail());
        vo.setRoleId(user.getRoleId());
        vo.setCreateTime(user.getCreateTime());
        return vo;
    }
}