package com.it.elderhub.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.it.elderhub.entity.CustomerMeal;
import com.it.elderhub.vo.CustomerMealVO;

/**
 * 客户膳食配置Service接口
 */
public interface CustomerMealService {

    /**
     * 分页查询客户膳食配置（默认查全部，可按客户ID/客户姓名模糊筛选）
     */
    IPage<CustomerMealVO> listCustomerMeals(Integer pageNum, Integer pageSize, Integer customerId, String customerName);

    /**
     * 新增客户膳食配置
     */
    void saveCustomerMeal(CustomerMeal customerMeal);

    /**
     * 修改客户膳食配置
     */
    void updateCustomerMeal(CustomerMeal customerMeal);

    /**
     * 删除客户膳食配置（逻辑删除）
     */
    void removeCustomerMeal(Integer id);
}
