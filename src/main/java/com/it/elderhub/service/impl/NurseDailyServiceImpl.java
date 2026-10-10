package com.it.elderhub.service.impl;
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

        wrapper.eq("user_id", userId);
        wrapper.eq("is_deleted", 0);

        if (customerName != null && !customerName.trim().isEmpty()) {
            wrapper.like("customer_name", customerName.trim());
        }

        wrapper.orderByDesc("create_time");

        Page<Customer> page = new Page<>(pageNum, pageSize);
        return customerMapper.selectPage(page, wrapper);
    }

    /**
     * 查询客户已购买的护理项目
     */
    @Override
    public List<CustomerNurseItemVO> listCustomerItems(
            Integer customerId,
            Integer userId) {

        checkCustomerOwnership(customerId, userId);

        QueryWrapper<CustomerNurseItem> wrapper =
                new QueryWrapper<>();

        wrapper.eq("customer_id", customerId);
        wrapper.eq("is_deleted", 0);
        wrapper.orderByDesc("create_time");

        List<CustomerNurseItem> items =
                customerNurseItemMapper.selectList(wrapper);

        if (items == null || items.isEmpty()) {
            return new ArrayList<>();
        }

        List<Integer> itemIds = items.stream()
                .map(CustomerNurseItem::getItem_id)
                .distinct()
                .collect(Collectors.toList());

        List<NurseContent> contents =
                nurseContentMapper.selectBatchIds(itemIds);

        if (contents == null) {
            contents = new ArrayList<>();
        }

        LocalDate today = LocalDate.now();
        List<CustomerNurseItemVO> voList = new ArrayList<>();

        for (CustomerNurseItem item : items) {
            CustomerNurseItemVO vo = new CustomerNurseItemVO();

            BeanUtils.copyProperties(item, vo);

            vo.setId(item.getId());
            vo.setNurseNumber(item.getNurse_number());

            for (NurseContent content : contents) {
                if (content.getId() != null
                        && content.getId().equals(item.getItem_id())) {
                    vo.setNursingName(content.getNursing_name());
                    break;
                }
            }

            calculateItemStatus(vo, today);
            voList.add(vo);
        }

        return voList;
    }

    /**
     * 保存日常护理记录
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveNurseRecord(
            NurseRecordAddDTO dto,
            Integer userId) {

        if (dto == null) {
            throw new RuntimeException("护理记录参数不能为空");
        }

        // 1. 校验客户归属
        checkCustomerOwnership(dto.getCustomerId(), userId);

        // 2. 校验护理次数
        if (dto.getNursingCount() == null
                || dto.getNursingCount() <= 0) {
            throw new RuntimeException("护理次数必须大于0");
        }

        // 3. 查询客户购买的护理项目
        QueryWrapper<CustomerNurseItem> itemWrapper =
                new QueryWrapper<>();

        itemWrapper.eq("customer_id", dto.getCustomerId());
        itemWrapper.eq("item_id", dto.getItemId());
        itemWrapper.eq("is_deleted", 0);

        CustomerNurseItem customerItem =
                customerNurseItemMapper.selectOne(itemWrapper);

        if (customerItem == null) {
            throw new RuntimeException("客户未购买该护理项目");
        }

        // 4. 校验剩余次数
        Integer remainingCount = customerItem.getNurse_number();

        if (remainingCount == null
                || remainingCount < dto.getNursingCount()) {
            throw new RuntimeException(
                    "该项目剩余次数不足，剩余："
                            + (remainingCount == null ? 0 : remainingCount)
                            + "次"
            );
        }

        // 5. 使用 LocalDateTime，匹配护理记录实体的时间类型
        LocalDateTime now = LocalDateTime.now();

        LocalDateTime nursingTime = dto.getNursingTime() != null
                ? dto.getNursingTime()
                : now;

        // 6. 创建护理记录
        NurseRecord record = new NurseRecord();

        record.setCustomer_id(dto.getCustomerId());
        record.setItem_id(dto.getItemId());
        record.setUser_id(userId);
        record.setNursing_time(nursingTime);
        record.setNursing_content(dto.getNursingContent());
        record.setNursing_count(dto.getNursingCount());
        record.setIs_deleted(0);

        nurseRecordMapper.insert(record);

        // 7. 更新护理记录创建时间和更新时间
        // NurseRecord 实体没有对应的 Java 字段，因此使用数据库列名。
        // 这里假设数据库支持插入时省略这两列。
        UpdateWrapper<NurseRecord> recordWrapper =
                new UpdateWrapper<>();

        recordWrapper.eq("id", record.getId());
        recordWrapper.set("create_time", now);
        recordWrapper.set("update_time", now);

        nurseRecordMapper.update(null, recordWrapper);

        // 8. 扣减护理项目剩余次数
        UpdateWrapper<CustomerNurseItem> updateWrapper =
                new UpdateWrapper<>();

        updateWrapper.eq("id", customerItem.getId());
        updateWrapper.eq("is_deleted", 0);
        updateWrapper.ge("nurse_number", dto.getNursingCount());

        updateWrapper.set(
                "nurse_number",
                remainingCount - dto.getNursingCount()
        );

        updateWrapper.set("update_time", new Date());

        int rows = customerNurseItemMapper.update(
                null,
                updateWrapper
        );

        if (rows == 0) {
            throw new RuntimeException(
                    "扣减护理次数失败，请刷新后重试"
            );
        }
    }

    /**
     * 查询客户护理记录
     */
    @Override
    public List<NurseRecordVO> listNurseRecords(
            Integer customerId,
            Integer userId) {

        checkCustomerOwnership(customerId, userId);

        QueryWrapper<NurseRecord> wrapper =
                new QueryWrapper<>();

        wrapper.eq("customer_id", customerId);
        wrapper.eq("is_deleted", 0);
        wrapper.orderByDesc("nursing_time");

        List<NurseRecord> records =
                nurseRecordMapper.selectList(wrapper);

        if (records == null || records.isEmpty()) {
            return new ArrayList<>();
        }

        List<Integer> itemIds = records.stream()
                .map(NurseRecord::getItem_id)
                .distinct()
                .collect(Collectors.toList());

        List<NurseContent> contents =
                nurseContentMapper.selectBatchIds(itemIds);

        if (contents == null) {
            contents = new ArrayList<>();
        }

        List<NurseRecordVO> voList = new ArrayList<>();

        for (NurseRecord record : records) {
            NurseRecordVO vo = new NurseRecordVO();

            BeanUtils.copyProperties(record, vo);

            for (NurseContent content : contents) {
                if (content.getId() != null
                        && content.getId().equals(record.getItem_id())) {
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
     */
    private void checkCustomerOwnership(
            Integer customerId,
            Integer userId) {

        QueryWrapper<Customer> customerWrapper =
                new QueryWrapper<>();

        customerWrapper.eq("id", customerId);
        customerWrapper.eq("is_deleted", 0);

        Customer customer =
                customerMapper.selectOne(customerWrapper);

        if (customer == null) {
            throw new RuntimeException("客户不存在");
        }

        QueryWrapper<Customer> ownershipWrapper =
                new QueryWrapper<>();

        ownershipWrapper.eq("id", customerId);
        ownershipWrapper.eq("is_deleted", 0);
        ownershipWrapper.eq("user_id", userId);

        Long count = customerMapper.selectCount(ownershipWrapper);

        if (count == null || count == 0) {
            throw new RuntimeException(
                    "无权操作该客户，该客户不属于您服务"
            );
        }
    }

    /**
     * 计算护理项目状态
     * 1：正常，2：次数不足，3：已到期
     */
    private void calculateItemStatus(
            CustomerNurseItemVO vo,
            LocalDate today) {

        LocalDate maturityTime = vo.getMaturityTime();

        if (maturityTime != null
                && maturityTime.isBefore(today)) {
            vo.setStatus(3);
            vo.setStatusMsg("已到期");
            return;
        }

        if (vo.getNurseNumber() == null
                || vo.getNurseNumber() <= 0) {
            vo.setStatus(2);
            vo.setStatusMsg("次数不足");
            return;
        }

        vo.setStatus(1);
        vo.setStatusMsg("正常");
    }
}