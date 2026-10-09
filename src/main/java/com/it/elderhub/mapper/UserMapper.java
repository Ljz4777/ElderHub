package com.it.elderhub.mapper;

import com.it.elderhub.entity.User;
import com.it.elderhub.query.UserQuery;
import com.it.elderhub.vo.UserVO;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Param;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * @author Ljz
 * @description 针对表【user(系统用户表)】的数据库操作Mapper
 * @createDate 2026-10-08 09:52:06
 * @Entity com.it.elderhub.entity.User
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {

    /**
     * 用户分页查询
     * @param page 分页对象
     * @param query 查询条件
     * @return 分页VO
     */
    IPage<UserVO> selectUserPage(Page<UserVO> page, @Param("query") UserQuery query);
}




