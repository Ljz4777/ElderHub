package com.it.elderhub.controller;

import com.it.elderhub.common.Result;
import com.it.elderhub.entity.Customer;
import com.it.elderhub.service.NurseAssignService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 健康管家-服务对象分配Controller（管理员操作）
 * 基础路径：/admin/user
 */
@RestController
@RequestMapping("/admin/user")
public class NurseAssignController {

    @Autowired
    private NurseAssignService nurseAssignService;

    /**
     * 查询某管家服务的客户列表
     * @param nurseId 管家用户ID
     */
    @GetMapping("/nurse-customers/{nurseId}")
    public Result<List<Customer>> listNurseCustomers(@PathVariable Integer nurseId) {
        return Result.success(nurseAssignService.listNurseCustomers(nurseId));
    }

    /**
     * 给管家分配客户
     * @param nurseId 管家用户ID
     * @param customerId 客户ID
     */
    @PostMapping("/assign-customer")
    public Result<Void> assignCustomer(@RequestParam Integer nurseId, @RequestParam Integer customerId) {
        nurseAssignService.assignCustomer(nurseId, customerId);
        return Result.success();
    }

    /**
     * 移除客户的管家关系
     * @param customerId 客户ID
     */
    @DeleteMapping("/remove-customer/{customerId}")
    public Result<Void> removeCustomerAssign(@PathVariable Integer customerId) {
        nurseAssignService.removeCustomerAssign(customerId);
        return Result.success();
    }
}
