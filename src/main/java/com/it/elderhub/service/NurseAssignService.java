package com.it.elderhub.service;

import com.it.elderhub.entity.Customer;

import java.util.List;

/**
 * 健康管家-服务对象分配Service（管理员操作）
 */
public interface NurseAssignService {

    /**
     * 查询某管家服务的客户列表
     * @param nurseId 管家用户ID
     * @return 客户列表
     */
    List<Customer> listNurseCustomers(Integer nurseId);

    /**
     * 给管家分配客户
     * @param nurseId 管家用户ID
     * @param customerId 客户ID
     */
    void assignCustomer(Integer nurseId, Integer customerId);

    /**
     * 移除客户的管家关系
     * @param customerId 客户ID
     */
    void removeCustomerAssign(Integer customerId);
}
