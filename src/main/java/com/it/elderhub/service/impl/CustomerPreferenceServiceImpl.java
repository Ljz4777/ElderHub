package com.it.elderhub.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.it.elderhub.entity.CustomerPreference;
import com.it.elderhub.mapper.CustomerPreferenceMapper;
import com.it.elderhub.service.CustomerPreferenceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 客户饮食喜好Service实现类
 */
@Service
public class CustomerPreferenceServiceImpl implements CustomerPreferenceService {

    @Autowired
    private CustomerPreferenceMapper customerPreferenceMapper;

    @Override
    public IPage<CustomerPreference> listPreferences(Integer pageNum, Integer pageSize, Integer customerId) {
        LambdaQueryWrapper<CustomerPreference> wrapper = new LambdaQueryWrapper<>();
        // 只查未删除的
        wrapper.eq(CustomerPreference::getIs_deleted, 0);
        // 按客户筛选
        if (customerId != null) {
            wrapper.eq(CustomerPreference::getCustomer_id, customerId);
        }
        // 按ID升序
        wrapper.orderByAsc(CustomerPreference::getId);

        Page<CustomerPreference> page = new Page<>(pageNum, pageSize);
        return customerPreferenceMapper.selectPage(page, wrapper);
    }

    @Override
    public CustomerPreference getPreferenceById(Integer id) {
        return customerPreferenceMapper.selectById(id);
    }

    @Override
    public void savePreference(CustomerPreference preference) {
        preference.setIs_deleted(0);
        customerPreferenceMapper.insert(preference);
    }

    @Override
    public void updatePreference(CustomerPreference preference) {
        if (preference.getId() == null) {
            throw new RuntimeException("喜好ID不能为空");
        }
        customerPreferenceMapper.updateById(preference);
    }

    @Override
    public void removePreference(Integer id) {
        // 逻辑删除
        CustomerPreference preference = new CustomerPreference();
        preference.setId(id);
        preference.setIs_deleted(1);
        customerPreferenceMapper.updateById(preference);
    }
}
