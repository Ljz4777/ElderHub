package com.it.elderhub.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.it.elderhub.dto.NurseRecordAddDTO;
import com.it.elderhub.entity.Customer;
import com.it.elderhub.vo.CustomerNurseItemVO;
import com.it.elderhub.vo.NurseRecordVO;

import java.util.List;

/**
 * 健康管家-日常护理Service（管家操作）
 */
public interface NurseDailyService {

    /**
     * 管家查询自己服务的客户列表（分页）
     * @param pageNum 页码
     * @param pageSize 每页条数
     * @param customerName 客户姓名（模糊查询，可空）
     * @param userId 当前登录管家ID
     * @return 客户分页列表
     */
    IPage<Customer> listMyCustomers(Integer pageNum, Integer pageSize, String customerName, Integer userId);

    /**
     * 查询客户已购护理项目（含状态）
     * @param customerId 客户ID
     * @param userId 当前登录管家ID（用于校验客户归属）
     * @return 护理项目列表
     */
    List<CustomerNurseItemVO> listCustomerItems(Integer customerId, Integer userId);

    /**
     * 录入护理记录（同时扣减客户项目次数）
     * @param dto 护理记录信息
     * @param userId 当前登录管家ID
     */
    void saveNurseRecord(NurseRecordAddDTO dto, Integer userId);

    /**
     * 查询客户的护理记录
     * @param customerId 客户ID
     * @param userId 当前登录管家ID（用于校验客户归属）
     * @return 护理记录列表
     */
    List<NurseRecordVO> listNurseRecords(Integer customerId, Integer userId);
}
