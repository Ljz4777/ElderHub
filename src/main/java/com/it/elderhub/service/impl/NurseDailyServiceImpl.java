package com.it.elderhub.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.it.elderhub.dto.NurseRecordAddDTO;
import com.it.elderhub.entity.Customer;
import com.it.elderhub.entity.CustomerNurseItem;
import com.it.elderhub.entity.NurseContent;
import com.it.elderhub.entity.NurseRecord;
import com.it.elderhub.mapper.CustomerMapper;
import com.it.elderhub.mapper.CustomerNurseItemMapper;
import com.it.elderhub.mapper.NurseContentMapper;
import com.it.elderhub.mapper.NurseRecordMapper;
import com.it.elderhub.service.NurseDailyService;
import com.it.elderhub.vo.CustomerNurseItemVO;
import com.it.elderhub.vo.NurseRecordVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 健康管家-日常护理Service实现
 */
@Service
public class NurseDailyServiceImpl implements NurseDailyService {

    @Autowired
    private CustomerMapper customerMapper;

    @Autowired
    private CustomerNurseItemMapper customerNurseItemMapper;

    @Autowired
    private NurseContentMapper nurseContentMapper;

    @Autowired
    private NurseRecordMapper nurseRecordMapper;

    /**
     * 分页查询当前管家服务的客户
     */
    @Override
    public IPage<Customer> listMyCustomers(
            Integer pageNum,
            Integer pageSize,
            String customerName,
            Integer userId) {

        QueryWrapper<Customer> wrapper = new QueryWrapper<>();

        // 只查询当前管家负责的客户
        wrapper.eq("user_id", userId);
        wrapper.eq("is_deleted", 0);

        // 客户姓名模糊查询
        if (customerName != null && !customerName.isEmpty()) {
            wrapper.like("customer_name", customerName);
        }

        // 按创建时间倒序
        wrapper.orderByDesc("create_time");

        Page<Customer> page = new Page<>(pageNum, pageSize);
        return customerMapper.selectPage(page, wrapper);
    }

    /**
     * 查询客户购买的护理项目
     */
    @Override
    public List<CustomerNurseItemVO> listCustomerItems(
            Integer customerId,
            Integer userId) {

        // 1. 校验客户归属
        checkCustomerOwnership(customerId, userId);

        // 2. 查询客户购买的护理项目
        QueryWrapper<CustomerNurseItem> wrapper = new QueryWrapper<>();

        wrapper.eq("customer_id", customerId);
        wrapper.eq("is_deleted", 0);
        wrapper.orderByDesc("create_time");

        List<CustomerNurseItem> items =
                customerNurseItemMapper.selectList(wrapper);

        if (items == null || items.isEmpty()) {
            return new ArrayList<>();
        }

        // 3. 批量查询护理项目信息
        List<Integer> itemIds = items.stream()
                .map(CustomerNurseItem::getItem_id)
                .distinct()
                .collect(Collectors.toList());

        List<NurseContent> contents =
                nurseContentMapper.selectBatchIds(itemIds);

        // 4. 组装VO并计算状态
        Date now = new Date();
        List<CustomerNurseItemVO> voList = new ArrayList<>();

        for (CustomerNurseItem item : items) {
            CustomerNurseItemVO vo = new CustomerNurseItemVO();

            BeanUtils.copyProperties(item, vo);

            // 填充护理项目信息
            for (NurseContent content : contents) {
                if (content.getId().equals(item.getItem_id())) {
                    vo.setNursingName(content.getNursing_name());
                    vo.setSerialNumber(content.getSerial_number());
                    vo.setServicePrice(content.getService_price());
                    break;
                }
            }

            // 计算项目状态
            calculateItemStatus(vo, now);

            voList.add(vo);
        }

        return voList;
    }

    /**
     * 保存日常护理记录
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveNurseRecord(NurseRecordAddDTO dto, Integer userId) {

        // 1. 校验客户归属
        checkCustomerOwnership(dto.getCustomerId(), userId);

        // 2. 校验护理次数
        if (dto.getNursingCount() == null
                || dto.getNursingCount() <= 0) {
            throw new RuntimeException("护理次数必须大于0");
        }

        // 3. 查询客户购买的护理项目
        LambdaQueryWrapper<CustomerNurseItem> itemWrapper =
                new LambdaQueryWrapper<>();

        itemWrapper.eq(
                CustomerNurseItem::getCustomer_id,
                dto.getCustomerId()
        );
        itemWrapper.eq(
                CustomerNurseItem::getItem_id,
                dto.getItemId()
        );
        itemWrapper.eq(
                CustomerNurseItem::getIs_deleted,
                0
        );

        CustomerNurseItem customerItem =
                customerNurseItemMapper.selectOne(itemWrapper);

        if (customerItem == null) {
            throw new RuntimeException("客户未购买该护理项目");
        }

        // 4. 校验剩余次数
        if (customerItem.getNurse_number() == null
                || customerItem.getNurse_number()
                < dto.getNursingCount()) {
            throw new RuntimeException(
                    "该项目剩余次数不足，剩余："
                            + customerItem.getNurse_number()
                            + "次"
            );
        }

        // 5. 创建护理记录
        LocalDateTime now = LocalDateTime.now();

        NurseRecord record = new NurseRecord();

        record.setCustomer_id(dto.getCustomerId());
        record.setItem_id(dto.getItemId());
        record.setUser_id(userId);

        record.setNursing_time(
                dto.getNursingTime() != null
                        ? dto.getNursingTime()
                        : now
        );

        record.setNursing_content(dto.getNursingContent());
        record.setNursing_count(dto.getNursingCount());
        record.setIs_deleted(0);

        // 先插入护理记录
        nurseRecordMapper.insert(record);

        // NurseRecord实体中没有create_time和update_time属性，
        // 因此通过数据库字段名更新这两个字段。
        // 此处假设插入时数据库允许这两个字段使用默认值或暂时为空。
        UpdateWrapper<NurseRecord> recordWrapper =
                new UpdateWrapper<>();

        recordWrapper.eq("id", record.getId());
        recordWrapper.set("create_time", now);
        recordWrapper.set("update_time", now);

        nurseRecordMapper.update(null, recordWrapper);

        // 6. 扣减客户护理项目剩余次数
        UpdateWrapper<CustomerNurseItem> updateWrapper =
                new UpdateWrapper<>();

        updateWrapper.eq("id", customerItem.getId());

        updateWrapper.set(
                "nurse_number",
                customerItem.getNurse_number()
                        - dto.getNursingCount()
        );

        updateWrapper.set("update_time", new Date());

        customerNurseItemMapper.update(null, updateWrapper);
    }

    /**
     * 查询客户护理记录
     */
    @Override
    public List<NurseRecordVO> listNurseRecords(
            Integer customerId,
            Integer userId) {

        // 1. 校验客户归属
        checkCustomerOwnership(customerId, userId);

        // 2. 查询护理记录
        LambdaQueryWrapper<NurseRecord> wrapper =
                new LambdaQueryWrapper<>();

        wrapper.eq(NurseRecord::getCustomer_id, customerId);
        wrapper.eq(NurseRecord::getIs_deleted, 0);
        wrapper.orderByDesc(NurseRecord::getNursing_time);

        List<NurseRecord> records =
                nurseRecordMapper.selectList(wrapper);

        if (records == null || records.isEmpty()) {
            return new ArrayList<>();
        }

        // 3. 批量查询护理项目信息
        List<Integer> itemIds = records.stream()
                .map(NurseRecord::getItem_id)
                .distinct()
                .collect(Collectors.toList());

        List<NurseContent> contents =
                nurseContentMapper.selectBatchIds(itemIds);

        // 4. 组装VO
        List<NurseRecordVO> voList = new ArrayList<>();

        for (NurseRecord record : records) {
            NurseRecordVO vo = new NurseRecordVO();

            BeanUtils.copyProperties(record, vo);

            // 填充护理项目名称
            for (NurseContent content : contents) {
                if (content.getId().equals(record.getItem_id())) {
                    vo.setNursingName(content.getNursing_name());
                    break;
                }
            }

            voList.add(vo);
        }

        return voList;
    }

    /**
     * 校验客户归属
     * 管家只能操作自己服务的客户
     */
    private void checkCustomerOwnership(
            Integer customerId,
            Integer userId) {

        // 1. 检查客户是否存在且未删除
        QueryWrapper<Customer> customerWrapper =
                new QueryWrapper<>();

        customerWrapper.eq("id", customerId);
        customerWrapper.eq("is_deleted", 0);

        Customer customer =
                customerMapper.selectOne(customerWrapper);

        if (customer == null) {
            throw new RuntimeException("客户不存在");
        }

        // 2. 检查客户是否属于当前管家
        QueryWrapper<Customer> ownershipWrapper =
                new QueryWrapper<>();

        ownershipWrapper.eq("id", customerId);
        ownershipWrapper.eq("is_deleted", 0);
        ownershipWrapper.eq("user_id", userId);

        Customer ownedCustomer =
                customerMapper.selectOne(ownershipWrapper);

        if (ownedCustomer == null) {
            throw new RuntimeException(
                    "无权操作该客户，该客户不属于您服务"
            );
        }
    }

    /**
     * 计算客户护理项目状态
     * 1：正常  2：到期  3：次数不足
     */
    private void calculateItemStatus(
            CustomerNurseItemVO vo,
            Date now) {

        // 到期判断
        if (vo.getMaturityTime() != null
                && vo.getMaturityTime().isBefore(LocalDate.now())) {

            vo.setStatus(2);
            vo.setStatusDesc("已到期");
            return;
        }

        // 剩余护理次数判断
        if (vo.getNurseNumber() == null
                || vo.getNurseNumber() <= 0) {

            vo.setStatus(3);
            vo.setStatusDesc("次数不足");
            return;
        }

        // 正常
        vo.setStatus(1);
        vo.setStatusDesc("正常");
    }
}