package com.it.elderhub.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.it.elderhub.dto.BackDownAddDTO;
import com.it.elderhub.dto.BackDownQuery;
import com.it.elderhub.entity.Backdown;
import com.it.elderhub.entity.Bed;
import com.it.elderhub.entity.Customer;
import com.it.elderhub.mapper.BedMapper;
import com.it.elderhub.mapper.CustomerMapper;
import com.it.elderhub.service.BackdownService;
import com.it.elderhub.mapper.BackdownMapper;
import com.it.elderhub.vo.BackDownVO;
import com.it.elderhub.vo.PageResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
* @author Ljz
* @description 针对表【backdown(退住登记表)】的数据库操作Service实现
* @createDate 2026-10-08 09:52:05
*/
@Slf4j
@Service
public class BackdownServiceImpl extends ServiceImpl<BackdownMapper, Backdown>
    implements BackdownService{

    private static final int NORMAL = 0;
    private static final int DEATH = 1;
    private static final int FREE = 1;

    @Autowired
    private BedMapper bedMapper;
    @Autowired
    private CustomerMapper customerMapper;
    /**
     *
     * @param query
     * @return
     */
    @Override
    public PageResult<BackDownVO> listBackDowns(BackDownQuery query) {
        Page<Backdown> page  = new Page<>(query.getPageNum(),query.getPageSize());
        LambdaQueryWrapper<Backdown> wrapper = new LambdaQueryWrapper<>();
        //模糊查询客户姓名
        if(query.getCustomerName()!=null&& !query.getCustomerName().isEmpty()){
            wrapper.apply(
                    "custom_id in (select id from customer where name like {0})","%"+query.getCustomerName()+"%"
            );
        }
        wrapper.orderByDesc(Backdown::getRetreattime);
        IPage<Backdown> doPage = this.page(page,wrapper);
        return convertToPageResult(doPage);
    }



    @Override
    @Transactional(rollbackFor = Exception.class)
    public void auditBackDown(Integer id, Integer auditStatus, String auditPerson) {
        Backdown backdown = this.getById(id);
        if(backdown==null||backdown.getAuditstatus()!=0){
            throw new RuntimeException("退住单不存在或已审核");
        }
        //更新审核信息
        backdown.setAuditstatus(auditStatus);
        backdown.setAuditperson(auditPerson);
        backdown.setAudittime(LocalDateTime.now());
        this.updateById(backdown);
        //审核通过而且为正常/死亡退住 释放床位
        if(auditStatus ==1&& (backdown.getRetreattype()==NORMAL||backdown.getRetreattype()==DEATH)){
            Customer customer = customerMapper.selectById(backdown.getCustomer_id());
            if(customer!=null&& customer.getBed_id()!=null){
                Bed bed = bedMapper.selectById(customer.getBed_id());
                if(bed!=null){
                    bed.setBed_status(FREE);
                    bedMapper.updateById(bed);
                    log.info("退住审核通过，释放床位[{}]",bed.getId());
                }
            }
        }
    }

    @Override
    public void saveBackDown(BackDownAddDTO dto) {
        Backdown backdown = new Backdown();
        backdown.setCustomer_id(dto.getCustomerId());
        if (dto.getRetreatTime() != null) {
            backdown.setRetreattime(dto.getRetreatTime());
        }
        backdown.setRetreattype(dto.getRetreatType());
        backdown.setRetreatreason(dto.getRetreatReason());
        backdown.setRemarks(dto.getRemarks());
        backdown.setAuditstatus(0); // 默认为待审核
        this.save(backdown);
    }

    @Override
    public PageResult<BackDownVO> listNurseBackDowns(BackDownQuery query, Integer userId) {
        Page<Backdown> page = new Page<>(query.getPageNum(), query.getPageSize());
        LambdaQueryWrapper<Backdown> wrapper = new LambdaQueryWrapper<>();

        // 根据当前登录管家ID筛选
        wrapper.apply("customer_id IN (SELECT id FROM customer WHERE butler_id = {0})", userId);
        wrapper.orderByDesc(Backdown::getRetreattime);

        IPage<Backdown> doPage = this.page(page, wrapper);
        return convertToPageResult(doPage);
    }

    private PageResult<BackDownVO> convertToPageResult(IPage<Backdown> doPage) {
        List<BackDownVO> voList = doPage.getRecords().stream().map(item -> {
            BackDownVO vo = new BackDownVO();
            vo.setId(item.getId());
            vo.setCustomerId(item.getCustomer_id());
            vo.setRetreatTime(item.getRetreattime() == null ? null : item.getRetreattime());
            vo.setRetreatType(item.getRetreattype());
            vo.setRetreatReason(item.getRetreatreason());
            vo.setAuditStatus(item.getAuditstatus());
            vo.setAuditPerson(item.getAuditperson());
            vo.setAuditTime(item.getAudittime() == null ? null : LocalDate.from(item.getAudittime()));
            vo.setRemarks(item.getRemarks());
            return vo;
        }).collect(Collectors.toList());

        return new PageResult<>(voList, doPage.getTotal(), doPage.getCurrent(), doPage.getSize());
    }
}




