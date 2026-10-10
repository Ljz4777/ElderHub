package com.it.elderhub.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.it.elderhub.dto.OutwardAddDTO;
import com.it.elderhub.dto.OutwardQuery;
import com.it.elderhub.entity.Bed;
import com.it.elderhub.entity.Customer;
import com.it.elderhub.entity.Outward;
import com.it.elderhub.mapper.BedMapper;
import com.it.elderhub.mapper.CustomerMapper;
import com.it.elderhub.mapper.OutwardMapper;
import com.it.elderhub.service.OutwardService;
import com.it.elderhub.vo.OutwardVO;
import com.it.elderhub.vo.PageResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class OutwardServiceImpl extends ServiceImpl<OutwardMapper, Outward> implements OutwardService {

    @Autowired
    private CustomerMapper customerMapper;

    @Autowired
    private BedMapper bedMapper;

    // 床位状态（1:空闲 2:有人 3:外出）
    private static final int BED_STATUS_AWAY = 3;
    private static final int BED_STATUS_OCCUPIED = 2;

    /**
     * 管理员：分页查询外出列表 (支持客户姓名模糊搜索)
     */
    @Override
    public PageResult<OutwardVO> listOutwards(OutwardQuery query) {
        Page<Outward> page = new Page<>(query.getPageNum(), query.getPageSize());
        LambdaQueryWrapper<Outward> wrapper = new LambdaQueryWrapper<>();

        if (query.getCustomerName() != null && !query.getCustomerName().isEmpty()) {
            wrapper.apply("customer_id IN (SELECT id FROM customer WHERE name LIKE {0})", "%" + query.getCustomerName() + "%");
        }
        wrapper.orderByDesc(Outward::getOutgoingtime);
        return convertToPageResult(this.page(page, wrapper));
    }

    /**
     * 管理员：审核外出申请 (通过/拒绝)
     * 逻辑：审核通过 -> 更新床位状态为 2(外出/AWAY)
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void auditOutward(Integer id, Integer auditStatus, String auditPerson) {
        Outward record = this.getById(id);
        if (record == null || record.getAuditstatus() != 0) {
            return; // 记录不存在或不是待审核状态(0)，直接返回
        }

        // 1. 更新审核信息
        record.setAuditstatus(auditStatus);
        record.setAuditperson(auditPerson);
        record.setAudittime(LocalDateTime.now());
        this.updateById(record);

        // 2. 审核通过 (auditStatus == 1)
        if (auditStatus == 1) {
            Customer customer = customerMapper.selectById(record.getCustomer_id());
            if (customer != null && customer.getBed_id() != null) {
                Bed bed = bedMapper.selectById(customer.getBed_id());
                if (bed != null) {
                    bed.setBed_status(BED_STATUS_AWAY);
                    bedMapper.updateById(bed);
                    log.info("外出申请审核通过，客户 [{}] 的床位已更新为外出状态", customer.getId());
                }
            }
        }
    }

    /**
     * 管家：提交外出申请
     */
    @Override
    public void saveOutward(OutwardAddDTO dto) {
        Outward outward = new Outward();
        outward.setCustomer_id(dto.getCustomerId());
        outward.setOutgoingreason(dto.getOutgoingReason());
        outward.setOutgoingtime(dto.getOutgoingTime());
        outward.setExpectedreturntime(dto.getExpectedReturnTime());
        outward.setEscorted(dto.getEscorted());
        outward.setRelation(dto.getRelation());
        outward.setEscortedtel(dto.getEscortedTel());
        outward.setRemarks(dto.getRemarks());
        outward.setAuditstatus(0); // 默认为 0-已提交
        this.save(outward);
    }

    /**
     * 管家：查看自己名下客户的外出申请
     */
    @Override
    public PageResult<OutwardVO> listNurseOutwards(OutwardQuery query, Integer userId) {
        Page<Outward> page = new Page<>(query.getPageNum(), query.getPageSize());
        LambdaQueryWrapper<Outward> wrapper = new LambdaQueryWrapper<>();
        wrapper.apply("customer_id IN (SELECT id FROM customer WHERE butler_id = {0})", userId);
        wrapper.orderByDesc(Outward::getOutgoingtime);
        return convertToPageResult(this.page(page, wrapper));
    }

    /**
     * 登记回院时间
     * 逻辑：登记回院 -> 更新床位状态为 1(占用/OCCUPIED)
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void registerReturn(Integer id, Date actualReturnTime) {
        // 1. 记录实际回院时间 (使用服务器当前时间更为准确)
        Outward record = this.getById(id);
        if (record == null) {
            throw new RuntimeException("外出记录不存在");
        }

        record.setActualreturntime(LocalDateTime.now());
        this.updateById(record);

        // 2. 回院后，将床位状态恢复为“占用”
        Customer customer = customerMapper.selectById(record.getCustomer_id());
        if (customer != null && customer.getBed_id() != null) {
            Bed bed = bedMapper.selectById(customer.getBed_id());
            if (bed != null) {
                bed.setBed_status(BED_STATUS_OCCUPIED);
                bedMapper.updateById(bed);
                log.info("客户 [{}] 已登记回院，床位恢复为占用状态", customer.getId());
            }
        }
    }

    /**
     * 辅助方法：将 DO 分页对象转换为 VO 分页对象
     */
    private PageResult<OutwardVO> convertToPageResult(IPage<Outward> doPage) {
        List<OutwardVO> list = doPage.getRecords().stream().map(d -> {
            OutwardVO vo = new OutwardVO();
            vo.setCustomerId(d.getId());
            vo.setCustomerId(d.getCustomer_id());
            // 注意：如果前端需要展示客户姓名，这里建议通过联表查询或者单独查询赋值给 vo.setCustomerName(...)

            vo.setOutgoingReason(d.getOutgoingreason());
            vo.setOutgoingTime(d.getOutgoingtime());
            vo.setExpectedReturnTime(d.getExpectedreturntime());
            vo.setActualReturnTime(d.getActualreturntime());
            vo.setEscorted(d.getEscorted());
            vo.setRelation(d.getRelation());
            vo.setEscortedTel(d.getEscortedtel());
            vo.setAuditStatus(d.getAuditstatus());
            vo.setAuditPerson(d.getAuditperson());
            vo.setAuditTime(d.getAudittime());
            vo.setRemarks(d.getRemarks());
            return vo;
        }).collect(Collectors.toList());

        return new PageResult<>(list, doPage.getTotal(), doPage.getCurrent(), doPage.getSize());
    }
}