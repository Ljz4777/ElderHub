package com.it.elderhub.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.it.elderhub.dto.CustomerNurseItemAddDTO;
import com.it.elderhub.entity.Customer;
import com.it.elderhub.entity.CustomerNurseItem;
import com.it.elderhub.entity.NurseContent;
import com.it.elderhub.entity.NurseLevelItem;
import com.it.elderhub.mapper.CustomerMapper;
import com.it.elderhub.mapper.CustomerNurseItemMapper;
import com.it.elderhub.mapper.NurseContentMapper;
import com.it.elderhub.mapper.NurseLevelItemMapper;
import com.it.elderhub.service.CustomerNurseItemService;
import com.it.elderhub.vo.CustomerNurseItemVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class CustomerNurseItemServiceImpl extends ServiceImpl<CustomerNurseItemMapper, CustomerNurseItem> implements CustomerNurseItemService {

    @Autowired
    private CustomerMapper customerMapper;

    @Autowired
    private NurseContentMapper nurseContentMapper;

    @Autowired
    private NurseLevelItemMapper nurseLevelItemMapper;

    /**
     * 设置护理级别
     * 逻辑：校验 -> 查出该等级包含的所有项目 -> 批量插入到客户项目表
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void setCustomerLevel(Integer customerId, Integer levelId) {
        // 1. 校验客户是否存在
        Customer customer = customerMapper.selectById(customerId);
        if (customer == null) {
            throw new RuntimeException("客户不存在");
        }

        // 2. 校验客户当前是否已设置护理等级
        // (因为customer表没字段，我们查关联表看有没有 level_id 不为空的数据)
        LambdaQueryWrapper<CustomerNurseItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CustomerNurseItem::getCustomer_id, customerId)
                .isNotNull(CustomerNurseItem::getLevel_id);

        if (this.count(wrapper) > 0) {
            throw new RuntimeException("该客户已拥有护理等级，请先移除现有等级");
        }

        // 3. 获取该等级下包含的所有项目ID
        LambdaQueryWrapper<NurseLevelItem> levelItemWrapper = new LambdaQueryWrapper<>();
        levelItemWrapper.eq(NurseLevelItem::getLevel_id, levelId);
        List<NurseLevelItem> levelItems = nurseLevelItemMapper.selectList(levelItemWrapper);

        if (levelItems == null || levelItems.isEmpty()) {
            throw new RuntimeException("该护理等级下未配置任何项目，无法设置");
        }

        // 4. 组装数据并批量插入
        LocalDate now = LocalDate.now();
        LocalDate maturity = now.plusMonths(3); // 默认有效期3个月

        List<CustomerNurseItem> insertList = levelItems.stream().map(l -> {
            CustomerNurseItem item = new CustomerNurseItem();
            item.setCustomer_id(customerId);
            item.setItem_id(l.getItem_id());
            item.setLevel_id(levelId); // 标记这是等级赠送的项目
            item.setNurse_number(1);   // 默认赠送1次
            item.setBuy_time(now);
            item.setMaturity_time(maturity);
            return item;
        }).collect(Collectors.toList());

        this.saveBatch(insertList);
        log.info("客户 [{}] 成功设置护理等级 [{}]，自动绑定 {} 个项目", customerId, levelId, insertList.size());
    }

    /**
     * 移除护理级别
     * 逻辑：删除该客户下所有属于"等级绑定"的项目记录
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeCustomerLevel(Integer customerId) {
        LambdaQueryWrapper<CustomerNurseItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CustomerNurseItem::getCustomer_id, customerId)
                .isNotNull(CustomerNurseItem::getLevel_id); // 只删等级绑定的，保留单独购买的

        int count = this.baseMapper.delete(wrapper);
        log.info("客户 [{}] 移除护理等级，清理了 {} 条关联项目", customerId, count);
    }

    /**
     * 查看客户已购项目 (含状态计算)
     */
    @Override
    public List<CustomerNurseItemVO> listCustomerNurseItems(Integer customerId) {
        // 1. 查询该客户的所有项目记录
        LambdaQueryWrapper<CustomerNurseItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CustomerNurseItem::getCustomer_id, customerId);
        List<CustomerNurseItem> items = this.list(wrapper);

        if (items == null || items.isEmpty()) {
            return List.of();
        }

        // 2. 提取项目ID，批量查询项目名称 (避免循环查库)
        List<Integer> itemIds = items.stream()
                .map(CustomerNurseItem::getItem_id)
                .distinct()
                .collect(Collectors.toList());

        List<NurseContent> contents = nurseContentMapper.selectBatchIds(itemIds);

        // 建立 id -> NursingName 的映射 Map
        Map<Integer, String> nameMap = contents.stream()
                .collect(Collectors.toMap(NurseContent::getId, NurseContent::getNursing_name, (v1, v2) -> v1));

        // 3. 转换 VO 并计算状态
        LocalDate now = LocalDate.now();
        return items.stream().map(item -> {
            CustomerNurseItemVO vo = new CustomerNurseItemVO();
            vo.setId(item.getId());
            vo.setNursingName(nameMap.getOrDefault(item.getItem_id(), "未知项目"));
            vo.setNurseNumber(item.getNurse_number());
            vo.setBuyTime(item.getBuy_time());
            vo.setMaturityTime(item.getMaturity_time());

            // --- 核心状态判断逻辑 ---
            if (item.getNurse_number() <= 0) {
                vo.setStatus(2); // 欠费
                vo.setStatusMsg("欠费(次数<=0)");
            } else if (item.getMaturity_time() != null && item.getMaturity_time().isBefore(now)) {
                vo.setStatus(3); // 到期
                vo.setStatusMsg("已到期");
            } else {
                vo.setStatus(1); // 正常
                vo.setStatusMsg("数量正常/未到期");
            }
            return vo;
        }).collect(Collectors.toList());
    }

    /**
     * 续费 (增加次数 + 延长有效期)
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void renewItem(Integer id, Integer addCount) {
        CustomerNurseItem item = this.getById(id);
        if (item == null) {
            throw new RuntimeException("购买记录不存在");
        }

        // 1. 增加次数
        item.setNurse_number(item.getNurse_number() + addCount);

        // 2. 延长有效期：如果还没过期，就在原基础上加；如果过期了，从今天开始加
        LocalDate now = LocalDate.now();
        if (item.getMaturity_time() != null && item.getMaturity_time().isAfter(now)) {
            item.setMaturity_time(item.getMaturity_time().plusMonths(3)); // 续费默认加3个月
        } else {
            item.setMaturity_time(now.plusMonths(3));
        }

        this.updateById(item);
    }

    /**
     * 移除单个客户项目
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeItem(Integer id) {
        this.removeById(id);
    }

    /**
     * 单独购买项目
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void buyItem(CustomerNurseItemAddDTO dto) {
        CustomerNurseItem item = new CustomerNurseItem();
        item.setCustomer_id(dto.getCustomerId());
        item.setItem_id(dto.getItemId());
        item.setLevel_id(null); // 单独购买的不绑定等级ID
        // 购买数量默认1
        item.setNurse_number(dto.getAddCount() != null ? dto.getAddCount() : 1);
        item.setBuy_time(LocalDate.now());

        // 如果前端传了时间就用前端的，否则默认当前日期
        item.setMaturity_time(dto.getMaturityTime() != null ? dto.getMaturityTime() : LocalDate.now());

        this.save(item);
    }
}