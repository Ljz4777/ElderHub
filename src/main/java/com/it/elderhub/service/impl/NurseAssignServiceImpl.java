package com.it.elderhub.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.it.elderhub.entity.Customer;
import com.it.elderhub.mapper.CustomerMapper;
import com.it.elderhub.service.NurseAssignService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

/**
 * 健康管家-服务对象分配Service实现
 */
@Service
public class NurseAssignServiceImpl implements NurseAssignService {

    @Autowired
    private CustomerMapper customerMapper;

    /**
     * 查询指定管家负责的客户
     */
    @Override
    public List<Customer> listNurseCustomers(Integer nurseId) {
        QueryWrapper<Customer> wrapper = new QueryWrapper<>();

        wrapper.eq("user_id", nurseId);
        wrapper.eq("is_deleted", 0);
        wrapper.orderByDesc("create_time");

        return customerMapper.selectList(wrapper);
    }

    /**
     * 分配客户给管家
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignCustomer(Integer nurseId, Integer customerId) {

        // 1. 检查客户是否存在且未删除
        Long count = customerMapper.selectCount(
                new QueryWrapper<Customer>()
                        .eq("id", customerId)
                        .eq("is_deleted", 0)
        );

        if (count == null || count == 0) {
            throw new RuntimeException("客户不存在");
        }

        // 2. 检查客户是否已经分配给其他管家
        Long assignedCount = customerMapper.selectCount(
                new QueryWrapper<Customer>()
                        .eq("id", customerId)
                        .eq("is_deleted", 0)
                        .isNotNull("user_id")
                        .ne("user_id", -1)
        );

        if (assignedCount != null && assignedCount > 0) {
            throw new RuntimeException("该客户已有管家，不能重复分配");
        }

        // 3. 更新客户的管家ID和更新时间
        UpdateWrapper<Customer> wrapper = new UpdateWrapper<>();

        wrapper.eq("id", customerId)
                .eq("is_deleted", 0)
                .and(w -> w.isNull("user_id").or().eq("user_id", -1))
                .set("user_id", nurseId)
                .set("update_time", new Date());

        int rows = customerMapper.update(null, wrapper);

        if (rows == 0) {
            throw new RuntimeException("分配失败，客户可能已被其他管家分配，请刷新后重试");
        }
    }

    /**
     * 移除客户与管家的分配关系
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeCustomerAssign(Integer customerId) {

        // 1. 检查客户是否存在且未删除
        Long count = customerMapper.selectCount(
                new QueryWrapper<Customer>()
                        .eq("id", customerId)
                        .eq("is_deleted", 0)
        );

        if (count == null || count == 0) {
            throw new RuntimeException("客户不存在");
        }

        // 2. 移除管家关系，将 user_id 重置为 -1
        UpdateWrapper<Customer> wrapper = new UpdateWrapper<>();

        wrapper.eq("id", customerId)
                .eq("is_deleted", 0)
                .set("user_id", -1)
                .set("update_time", new Date());

        int rows = customerMapper.update(null, wrapper);

        if (rows == 0) {
            throw new RuntimeException("移除管家失败，请刷新后重试");
        }

        // 注意：不影响护理记录信息
    }
}