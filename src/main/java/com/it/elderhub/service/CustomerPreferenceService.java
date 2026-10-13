package com.it.elderhub.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.it.elderhub.entity.CustomerPreference;

/**
 * 客户饮食喜好Service接口
 */
public interface CustomerPreferenceService {

    /**
     * 分页查询客户饮食喜好
     * @param pageNum 页码（默认1）
     * @param pageSize 每页条数（默认10）
     * @param customerId 客户ID筛选（可空）
     * @return 分页结果
     */
    IPage<CustomerPreference> listPreferences(Integer pageNum, Integer pageSize, Integer customerId);

    /**
     * 查询饮食喜好详情
     * @param id 喜好ID
     * @return 饮食喜好信息
     */
    CustomerPreference getPreferenceById(Integer id);

    /**
     * 新增客户饮食喜好
     * @param preference 饮食喜好信息
     */
    void savePreference(CustomerPreference preference);

    /**
     * 修改客户饮食喜好
     * @param preference 饮食喜好信息（必须带ID）
     */
    void updatePreference(CustomerPreference preference);

    /**
     * 删除客户饮食喜好（逻辑删除）
     * @param id 喜好ID
     */
    void removePreference(Integer id);
}
