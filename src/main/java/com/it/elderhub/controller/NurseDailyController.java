package com.it.elderhub.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.it.elderhub.common.Result;
import com.it.elderhub.dto.NurseRecordAddDTO;
import com.it.elderhub.entity.Customer;
import com.it.elderhub.service.NurseDailyService;
import com.it.elderhub.vo.CustomerNurseItemVO;
import com.it.elderhub.vo.NurseRecordVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 健康管家-日常护理Controller（管家操作）
 * 基础路径：/nurse
 * 注意：所有接口需要管家登录，userId从登录token中获取
 * 目前先用@RequestParam传入userId，后续接入JWT拦截器后改为从UserContext获取
 */
@RestController
@RequestMapping("/nurse")
public class NurseDailyController {

    @Autowired
    private NurseDailyService nurseDailyService;

    /**
     * 管家查询自己服务的客户列表（分页）
     * @param pageNum 页码（默认1）
     * @param pageSize 每页条数（默认10）
     * @param customerName 客户姓名（模糊查询，可空）
     * @param userId 当前登录管家ID
     */
    @GetMapping("/customer/list")
    public Result<IPage<Customer>> listMyCustomers(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            String customerName,
            @RequestParam Integer userId) {
        return Result.success(nurseDailyService.listMyCustomers(pageNum, pageSize, customerName, userId));
    }

    /**
     * 查询客户已购护理项目（含状态）
     * @param customerId 客户ID
     * @param userId 当前登录管家ID
     */
    @GetMapping("/customer/items/{customerId}")
    public Result<List<CustomerNurseItemVO>> listCustomerItems(
            @PathVariable Integer customerId,
            @RequestParam Integer userId) {
        return Result.success(nurseDailyService.listCustomerItems(customerId, userId));
    }

    /**
     * 录入护理记录（同时扣减客户项目次数）
     * @param dto 护理记录信息
     * @param userId 当前登录管家ID
     */
    @PostMapping("/nurse-record/save")
    public Result<Void> saveNurseRecord(@RequestBody NurseRecordAddDTO dto, @RequestParam Integer userId) {
        nurseDailyService.saveNurseRecord(dto, userId);
        return Result.success();
    }

    /**
     * 查询客户的护理记录
     * @param customerId 客户ID
     * @param userId 当前登录管家ID
     */
    @GetMapping("/nurse-record/list/{customerId}")
    public Result<List<NurseRecordVO>> listNurseRecords(
            @PathVariable Integer customerId,
            @RequestParam Integer userId) {
        return Result.success(nurseDailyService.listNurseRecords(customerId, userId));
    }
}
