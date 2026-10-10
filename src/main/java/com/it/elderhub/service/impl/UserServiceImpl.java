package com.it.elderhub.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.it.elderhub.dto.UserAddDTO;
import com.it.elderhub.dto.UserQuery;
import com.it.elderhub.dto.UserUpdateDTO;
import com.it.elderhub.entity.Menu;
import com.it.elderhub.entity.Role;
import com.it.elderhub.entity.User;
import com.it.elderhub.mapper.MenuMapper;
import com.it.elderhub.mapper.RoleMapper;
import com.it.elderhub.mapper.UserMapper;
import com.it.elderhub.service.UserService;
import com.it.elderhub.utils.MD5Util;
import com.it.elderhub.vo.UserVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    @Autowired
    private UserMapper userMapper;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private RoleMapper roleMapper;
    @Autowired
    private MenuMapper menuMapper;

    
    @Override
    public User register(String username, String password) {

        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Username is required");
        }
        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException("Password is required");
        }

        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername,username);
        User exist = userMapper.selectOne(wrapper);

        if (exist != null) {
            throw new IllegalArgumentException("Username already exists");
        }

        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        // 补齐 NOT NULL 字段默认值（user 表 nickname/sex/phone_number/role_id/create_by 均为 NOT NULL）
        user.setNickname(username);
        user.setSex(0);
        user.setPhone_number("");
        user.setRole_id(2); // 默认注册为健康管家
        user.setCreate_by(0);

        userMapper.insert(user);

        return user;
    }

    @Override
    public User login(String username, String password) {
        // 1. 根据用户名去数据库查询用户
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, username);//直接引用实体类属性
        User user = userMapper.selectOne(wrapper);

        if (user == null) {
            throw new IllegalArgumentException("User not found");
        }

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new IllegalArgumentException("Invalid password");
        }

        // 3. 密码匹配，返回该用户对象
        return user;
    }
    /**
     * 分页查询用户列表
     */
    @Override
    public IPage<UserVO> listUsers(UserQuery query) {
        Page<User> page = new Page<>(query.getPageNum(), query.getPageSize());
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();

        // 条件过滤
        if (query.getNickname() != null && !query.getNickname().isEmpty()) {
            wrapper.like(User::getNickname, query.getNickname());
        }
        if (query.getRoleId() != null) {
            wrapper.eq(User::getRole_id, query.getRoleId());
        }
        // 默认过滤已删除的
        wrapper.eq(User::getIs_deleted, 0);

        IPage<User> userPage = this.page(page, wrapper);

        // 转换 VO 并填充角色名
        return userPage.convert(this::convertToVO);
    }

    /**
     * 详情 (根据ID获取用户)
     */
    @Override
    public UserVO getUserById(Integer id) {
        User user = this.getById(id);
        return convertToVO(user);
    }

    /**
     * 新增用户 (默认未删除，自动填充时间)
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveUser(UserAddDTO dto) {
        User user = new User();
        BeanUtils.copyProperties(dto, user);
        user.setIs_deleted(0);
        user.setCreate_time(LocalDateTime.now());
        // TODO: 后续需要加上密码加密逻辑，这里先直接存明文或简单加密
        this.save(user);
    }

    /**
     * 修改用户
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateUser(UserUpdateDTO dto) {
        User user = new User();
        BeanUtils.copyProperties(dto, user);
        user.setUpdate_time(LocalDateTime.now());
        this.updateById(user);
    }

    /**
     * 逻辑删除
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeUser(Integer id) {
        User user = this.getById(id);
        if (user != null) {
            user.setIs_deleted(1);
            user.setUpdate_time(LocalDateTime.now());
            this.updateById(user);
        }
    }

    /**
     * 角色列表 (未删除的)
     */
    @Override
    public List<Role> listRoles() {
        LambdaQueryWrapper<Role> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Role::getIs_deleted, 0);
        return roleMapper.selectList(wrapper);
    }

    /**
     * 菜单树 (获取所有菜单，并在前端或后端组装树)
     * 这里直接返回全量菜单给前端，前端自己用 component 组装树
     */
    @Override
    public List<Menu> listMenuTree() {
        return menuMapper.selectList(null);
    }

    /**
     * 管家列表 ( roleId = 2 )
     */
    @Override
    public List<UserVO> listNurses(String nickname) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getRole_id, 2); // 锁定健康管家
        wrapper.eq(User::getIs_deleted, 0);
        if (nickname != null && !nickname.isEmpty()) {
            wrapper.like(User::getNickname, nickname);
        }
        List<User> users = this.list(wrapper);
        return users.stream().map(this::convertToVO).collect(Collectors.toList());
    }

    // --- 内部工具方法：实体转VO ---
    private UserVO convertToVO(User user) {
        UserVO vo = new UserVO();
        BeanUtils.copyProperties(user, vo);
        // 填充角色名
        if (user.getRole_id() != null) {
            Role role = roleMapper.selectById(user.getRole_id());
            if (role != null) {
                vo.setRoleName(role.getName());
            }
        }
        return vo;
    }

}