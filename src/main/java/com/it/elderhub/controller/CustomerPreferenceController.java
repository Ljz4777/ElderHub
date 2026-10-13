package com.it.elderhub.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.it.elderhub.common.Result;
import com.it.elderhub.entity.CustomerPreference;
import com.it.elderhub.service.CustomerPreferenceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 客户饮食喜好Controller
 */
@RestController
@RequestMapping("/admin/customer-preference")
public class CustomerPreferenceController {

    @Autowired
    private CustomerPreferenceService customerPreferenceService;

    /**
     * 分页查询客户饮食喜好
     * @param pageNum 页码（默认1）
     * @param pageSize 每页条数（默认10）
     * @param customerId 客户ID筛选（可空）
     */
    @GetMapping("/list")
    public Result<IPage<CustomerPreference>> list(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            Integer customerId) {
        return Result.success(customerPreferenceService.listPreferences(pageNum, pageSize, customerId));
    }

    /**
     * 查询饮食喜好详情
     * @param id 喜好ID
     */
    @GetMapping("/{id}")
    public Result<CustomerPreference> detail(@PathVariable Integer id) {
        return Result.success(customerPreferenceService.getPreferenceById(id));
    }

    /**
     * 新增客户饮食喜好
     */
    @PostMapping("/save")
    public Result<Void> save(@RequestBody CustomerPreference preference) {
        customerPreferenceService.savePreference(preference);
        return Result.success();
    }

    /**
     * 修改客户饮食喜好
     */
    @PutMapping("/update")
    public Result<Void> update(@RequestBody CustomerPreference preference) {
        customerPreferenceService.updatePreference(preference);
        return Result.success();
    }

    /**
     * 删除客户饮食喜好（逻辑删除）
     * @param id 喜好ID
     */
    @DeleteMapping("/remove/{id}")
    public Result<Void> remove(@PathVariable Integer id) {
        customerPreferenceService.removePreference(id);
        return Result.success();
    }
}
