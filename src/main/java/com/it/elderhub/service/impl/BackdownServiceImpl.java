package com.it.elderhub.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.it.elderhub.dto.BackDownAddDTO;
import com.it.elderhub.dto.BackDownQuery;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.it.elderhub.entity.Backdown;
import com.it.elderhub.entity.Bed;
import com.it.elderhub.entity.BedDetails;
import com.it.elderhub.entity.Customer;
import com.it.elderhub.mapper.BedDetailsMapper;
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
import java.util.Map;
import java.util.Objects;
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
    @Autowired
    private BedDetailsMapper bedDetailsMapper;
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
                    "customer_id in (select id from customer where customer_name like {0})","%"+query.getCustomerName()+"%"
            );
        }
        if(query.getCustomerId()!=null){
            wrapper.eq(Backdown::getCustomer_id, query.getCustomerId());
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
            throw new IllegalArgumentException("退住单不存在或已审核");
        }
        //更新审核信息
        backdown.setAuditstatus(auditStatus);
        backdown.setAuditperson(auditPerson);
        backdown.setAudittime(LocalDateTime.now());
        this.updateById(backdown);
        //审核通过且为正常/死亡退住：释放床位、关闭入住记录、逻辑删除客户（保留床位类型不处理）
        if(auditStatus ==1&& (backdown.getRetreattype()==NORMAL||backdown.getRetreattype()==DEATH)){
            Customer customer = customerMapper.selectById(backdown.getCustomer_id());
            if(customer!=null){
                //1.关闭当前生效的入住记录（逻辑删除，保留使用历史）
                BedDetails activeDetails = bedDetailsMapper.selectOne(
                        new QueryWrapper<BedDetails>()
                                .eq("customer_id", customer.getId())
                                .eq("is_deleted", 0)
                                .orderByDesc("id")
                                .last("LIMIT 1")
                );
                if(activeDetails!=null){
                    activeDetails.setEnd_date(new Date());
                    activeDetails.setIs_deleted(1);
                    bedDetailsMapper.updateById(activeDetails);
                }
                //2.释放床位：以生效入住记录上的床位为准，客户档案床位一并兜底
                //（两者可能因调床/历史数据不同步，漏放任一个都会产生"已入住但无在住客户"的孤儿床位）
                freeBedIfOccupied(activeDetails == null ? null : activeDetails.getBed_id());
                freeBedIfOccupied(customer.getBed_id());
                //3.逻辑删除客户（退住完成，不再出现在在住客户中；保留行以关联退住/入住历史）
                customer.setIs_deleted(1);
                customerMapper.updateById(customer);
                log.info("退住审核通过，客户[{}]已退住", customer.getId());
            }
        }
    }

    /** 床位仍被占用时释放为空闲（幂等） */
    private void freeBedIfOccupied(Integer bedId) {
        if (bedId == null) {
            return;
        }
        Bed bed = bedMapper.selectById(bedId);
        if (bed != null && bed.getBed_status() != null && bed.getBed_status() != FREE) {
            bed.setBed_status(FREE);
            bedMapper.updateById(bed);
            log.info("释放床位[{}]", bedId);
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

        // 根据当前登录管家ID筛选（customer.user_id 为管家分配字段，butler_id 为历史遗留列恒为空）
        wrapper.apply("customer_id IN (SELECT id FROM customer WHERE user_id = {0})", userId);
        if(query.getCustomerName()!=null&& !query.getCustomerName().isEmpty()){
            wrapper.apply(
                    "customer_id IN (SELECT id FROM customer WHERE customer_name LIKE {0})","%"+query.getCustomerName()+"%"
            );
        }
        if(query.getCustomerId()!=null){
            wrapper.eq(Backdown::getCustomer_id, query.getCustomerId());
        }
        wrapper.orderByDesc(Backdown::getRetreattime);

        IPage<Backdown> doPage = this.page(page, wrapper);
        return convertToPageResult(doPage);
    }

    private PageResult<BackDownVO> convertToPageResult(IPage<Backdown> doPage) {
        List<Backdown> records = doPage.getRecords();
        // 批量关联客户姓名（含已逻辑删除的客户，保证退住历史仍显示姓名）
        List<Integer> customerIds = records.stream()
                .map(Backdown::getCustomer_id).filter(Objects::nonNull).distinct()
                .collect(Collectors.toList());
        Map<Integer, String> customerNameMap = customerIds.isEmpty() ? java.util.Map.of()
                : customerMapper.selectBatchIds(customerIds).stream()
                .collect(Collectors.toMap(Customer::getId, Customer::getCustomer_name, (v1, v2) -> v1));

        List<BackDownVO> voList = records.stream().map(item -> {
            BackDownVO vo = new BackDownVO();
            vo.setId(item.getId());
            vo.setCustomerId(item.getCustomer_id());
            vo.setCustomerName(customerNameMap.get(item.getCustomer_id()));
            vo.setRetreatTime(item.getRetreattime());
            vo.setRetreatType(item.getRetreattype());
            vo.setRetreatReason(item.getRetreatreason());
            vo.setAuditStatus(item.getAuditstatus());
            vo.setAuditPerson(item.getAuditperson());
            vo.setAuditTime(item.getAudittime());
            vo.setRemarks(item.getRemarks());
            return vo;
        }).collect(Collectors.toList());

        return new PageResult<>(voList, doPage.getTotal(), doPage.getCurrent(), doPage.getSize());
    }
}




