package com.it.elderhub.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.it.elderhub.dto.BedDetailsQuery;
import com.it.elderhub.dto.BedTransferDTO;
import com.it.elderhub.entity.Bed;
import com.it.elderhub.entity.BedDetails;
import com.it.elderhub.entity.Customer;
import com.it.elderhub.entity.Room;
import com.it.elderhub.mapper.BedDetailsMapper;
import com.it.elderhub.mapper.BedMapper;
import com.it.elderhub.mapper.CustomerMapper;
import com.it.elderhub.mapper.RoomMapper;
import com.it.elderhub.service.BedService;
import com.it.elderhub.vo.BedDetailsVO;
import com.it.elderhub.vo.BedStatisticsVO;
import com.it.elderhub.vo.BedVO;
import com.it.elderhub.vo.PageResult;
import com.it.elderhub.vo.RoomBedVO;
import com.it.elderhub.vo.RoomVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
* @author Ljz
* @description 针对表【bed(床位信息表)】的数据库操作Service实现
* @createDate 2026-10-08 09:52:05
*/
@Slf4j
@Service
public class BedServiceImpl extends ServiceImpl<BedMapper, Bed>
    implements BedService{

    // 床位状态（1:空闲 2:已入住 3:外出）
    private static final int BED_STATUS_FREE = 1;
    private static final int BED_STATUS_OCCUPIED = 2;
    private static final int BED_STATUS_AWAY = 3;

    // 楼栋固定606
    private static final String BUILDING_NO = "606";

    @Autowired
    private BedDetailsMapper bedDetailsMapper;

    @Autowired
    private RoomMapper roomMapper;

    @Autowired
    private CustomerMapper customerMapper;

    /**
     * 床位统计（总数/空闲/已入住/外出）
     */
    @Override
    public BedStatisticsVO getBedStatistics() {
        BedStatisticsVO vo = new BedStatisticsVO();
        vo.setTotalBeds(this.count());
        vo.setFreeBeds(countByStatus(BED_STATUS_FREE));
        vo.setOccupiedBeds(countByStatus(BED_STATUS_OCCUPIED));
        vo.setAwayBeds(countByStatus(BED_STATUS_AWAY));
        return vo;
    }

    private Long countByStatus(int bedStatus) {
        QueryWrapper<Bed> wrapper = new QueryWrapper<>();
        wrapper.eq("bed_status", bedStatus);
        return this.count(wrapper);
    }

    /**
     * 床位示意图（未指定楼层时返回全部楼层）
     */
    @Override
    public List<RoomBedVO> listBedMap(String floor) {
        QueryWrapper<Room> roomWrapper = new QueryWrapper<>();
        // 指定楼层时按楼层过滤，否则返回全部楼层
        if (floor != null && !floor.isEmpty()) {
            roomWrapper.eq("room_floor", floor);
        }
        roomWrapper.orderByAsc("room_floor").orderByAsc("room_no");
        List<Room> rooms = roomMapper.selectList(roomWrapper);
        if (rooms == null || rooms.isEmpty()) {
            return List.of();
        }

        List<Integer> roomNos = rooms.stream().map(Room::getRoom_no).collect(Collectors.toList());
        QueryWrapper<Bed> bedWrapper = new QueryWrapper<>();
        bedWrapper.in("room_no", roomNos).orderByAsc("room_no").orderByAsc("bed_no");
        List<Bed> beds = this.list(bedWrapper);

        Map<Integer, List<Bed>> bedGroup = beds.stream()
                .collect(Collectors.groupingBy(Bed::getRoom_no, LinkedHashMap::new, Collectors.toList()));

        return rooms.stream().map(room -> {
            RoomBedVO vo = new RoomBedVO();
            vo.setId(room.getId());
            vo.setRoomFloor(room.getRoom_floor());
            vo.setRoomNo(room.getRoom_no());
            List<Bed> roomBeds = bedGroup.getOrDefault(room.getRoom_no(), List.of());
            vo.setBeds(roomBeds.stream().map(this::toBedVO).collect(Collectors.toList()));
            return vo;
        }).collect(Collectors.toList());
    }

    /**
     * 床位使用详情分页
     * 正在使用：deleted=0 且 endDate 为空；使用历史：deleted=1 或 endDate <= 当前日期
     */
    @Override
    public PageResult<BedDetailsVO> listBedDetails(BedDetailsQuery query) {
        Page<BedDetails> page = new Page<>(query.getPageNum(), query.getPageSize());
        QueryWrapper<BedDetails> wrapper = new QueryWrapper<>();

        if (query.getCustomerName() != null && !query.getCustomerName().isEmpty()) {
            wrapper.apply("customer_id IN (SELECT id FROM customer WHERE customer_name LIKE {0})",
                    "%" + query.getCustomerName() + "%");
        }
        if (query.getRoomNo() != null) {
            wrapper.apply("bed_id IN (SELECT id FROM bed WHERE room_no = {0})", query.getRoomNo());
        }
        if (query.getStartDate() != null) {
            wrapper.eq("start_date", query.getStartDate());
        }
        // 老人类型筛选：护理老人=已配置护理项目，自理老人=未配置（默认查自理）
        if (query.getCustomerType() != null && query.getCustomerType() == 2) {
            wrapper.apply("customer_id IN (SELECT customer_id FROM customer_nurse_item WHERE is_deleted = 0)");
        } else if (query.getCustomerType() != null && query.getCustomerType() == 1) {
            wrapper.apply("customer_id NOT IN (SELECT customer_id FROM customer_nurse_item WHERE is_deleted = 0)");
        }
        // 申请候选人过滤（applyType：1-退住申请 2-外出申请）
        if (query.getApplyType() != null) {
            if (query.getApplyType() == 1) {
                // 退住申请候选人：床位占用中即可（在住/外出中均可退住），
                // 排除已提交待审核退住申请的客户；已拒绝(2)的允许重新申请
                wrapper.apply("bed_id IN (SELECT id FROM bed WHERE bed_status IN (2, 3))");
                wrapper.apply("customer_id NOT IN (SELECT customer_id FROM backdown WHERE is_deleted = 0 AND audit_status = 0)");
            } else if (query.getApplyType() == 2) {
                // 外出申请候选人：仅限在住（外出中的不能重复外出），
                // 排除已提交待审核或尚未回院的外出申请客户
                wrapper.apply("bed_id IN (SELECT id FROM bed WHERE bed_status = 2)");
                wrapper.apply("customer_id NOT IN (SELECT customer_id FROM outward WHERE is_deleted = 0 AND audit_status IN (0, 1) AND actual_return_time IS NULL)");
            }
        }
        // 管家范围过滤：只返回该管家名下客户
        if (query.getUserId() != null) {
            wrapper.apply("customer_id IN (SELECT id FROM customer WHERE user_id = {0})", query.getUserId());
        }
        Date todayStart = startOfToday();
        if (query.getStatus() != null && query.getStatus() == 1) {
            // 正在使用：未退住/调床（is_deleted=0）且合同未到期（end_date 为空 或 到期日为今天及以后）
            wrapper.eq("is_deleted", 0)
                    .and(w -> w.isNull("end_date").or().ge("end_date", todayStart));
        } else if (query.getStatus() != null && query.getStatus() == 2) {
            // 使用历史：已退住/调床（is_deleted=1），或合同已到期但尚未办理退住
            wrapper.and(w -> w.eq("is_deleted", 1)
                    .or(o -> o.eq("is_deleted", 0).lt("end_date", todayStart)));
        }
        wrapper.orderByDesc("id");

        IPage<BedDetails> doPage = bedDetailsMapper.selectPage(page, wrapper);
        List<BedDetails> records = doPage.getRecords();
        List<BedDetailsVO> list = new ArrayList<>();

        if (records != null && !records.isEmpty()) {
            List<Integer> customerIds = records.stream()
                    .map(BedDetails::getCustomer_id).filter(Objects::nonNull).distinct()
                    .collect(Collectors.toList());
            List<Integer> bedIds = records.stream()
                    .map(BedDetails::getBed_id).filter(Objects::nonNull).distinct()
                    .collect(Collectors.toList());

            Map<Integer, Customer> customerMap = customerIds.isEmpty() ? Map.of()
                    : customerMapper.selectBatchIds(customerIds).stream()
                    .collect(Collectors.toMap(Customer::getId, c -> c, (v1, v2) -> v1));
            Map<Integer, Bed> bedMap = bedIds.isEmpty() ? Map.of()
                    : baseMapper.selectBatchIds(bedIds).stream()
                    .collect(Collectors.toMap(Bed::getId, b -> b, (v1, v2) -> v1));

            for (BedDetails details : records) {
                BedDetailsVO vo = new BedDetailsVO();
                vo.setId(details.getId());
                vo.setCustomerId(details.getCustomer_id());

                Customer customer = details.getCustomer_id() == null ? null : customerMap.get(details.getCustomer_id());
                vo.setCustomerName(customer == null ? null : customer.getCustomer_name());

                vo.setBedId(details.getBed_id());
                Bed bed = details.getBed_id() == null ? null : bedMap.get(details.getBed_id());
                vo.setBedNo(bed == null ? null : bed.getBed_no());
                vo.setRoomNo(bed == null ? null : bed.getRoom_no());

                vo.setStartDate(details.getStart_date());
                vo.setEndDate(details.getEnd_date());
                vo.setBedDetails(details.getBed_details());

                boolean notDeleted = details.getIs_deleted() == null || details.getIs_deleted() == 0;
                boolean notExpired = details.getEnd_date() == null || !details.getEnd_date().before(todayStart);
                boolean inUse = notDeleted && notExpired;
                vo.setStatus(inUse ? 1 : 2);
                vo.setBedStatus(bed == null ? null : bed.getBed_status());
                if (!notDeleted) {
                    // 已退住或已调床
                    vo.setStatusMsg("使用历史");
                } else if (!notExpired) {
                    // 合同已到期但尚未办理退住：不再算在住
                    vo.setStatusMsg("已到期");
                } else {
                    // 在住客户的实时状态跟随床位：外出 / 在住
                    vo.setStatusMsg(bed != null && bed.getBed_status() != null
                            && bed.getBed_status() == BED_STATUS_AWAY ? "外出" : "在住");
                }
                list.add(vo);
            }
        }

        return new PageResult<>(list, doPage.getTotal(), doPage.getCurrent(), doPage.getSize());
    }

    /** 当天 00:00:00，用于按"到期日当天仍算有效"比较 */
    private Date startOfToday() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
    }

    /**
     * 合同到期自动释放床位
     * 需求规约未规定"到期自动释放"，按业务约定实现：生效记录的退住时间（=合同到期日）早于今日时
     * 自动退住——关闭生效入住记录、释放床位、逻辑删除客户，与"正常退住"审核通过的效果一致。
     */
    @Scheduled(initialDelay = 5000, fixedDelay = 60000)
    @Transactional(rollbackFor = Exception.class)
    public void releaseExpiredBeds() {
        Date todayStart = startOfToday();
        List<BedDetails> expiredList = bedDetailsMapper.selectList(new QueryWrapper<BedDetails>()
                .eq("is_deleted", 0)
                .isNotNull("end_date")
                .lt("end_date", todayStart));
        if (expiredList != null && !expiredList.isEmpty()) {
            for (BedDetails details : expiredList) {
                // 1.关闭生效的入住记录（退住时间=合同到期日，转为使用历史）
                details.setIs_deleted(1);
                bedDetailsMapper.updateById(details);

                // 2.释放床位：以入住记录上的床位为准（客户档案缺失/已换床时同样兜底放床），避免漏放产生"孤儿床位"
                releaseBedIfNoLiveDetails(details.getBed_id());

                // 3.逻辑删除客户（合同到期视为自动退住，不再出现在在住客户列表）
                Customer customer = details.getCustomer_id() == null ? null
                        : customerMapper.selectById(details.getCustomer_id());
                if (customer == null || (customer.getIs_deleted() != null && customer.getIs_deleted() == 1)) {
                    continue;
                }
                customer.setIs_deleted(1);
                customerMapper.updateById(customer);
                // 客户档案仍挂着其他床位时一并兜底释放
                if (customer.getBed_id() != null && !customer.getBed_id().equals(details.getBed_id())) {
                    releaseBedIfNoLiveDetails(customer.getBed_id());
                }
            }
            log.info("合同到期自动释放床位：共处理 {} 条", expiredList.size());
        }
        // 4.一致性自愈：无论是否存在到期记录，都清扫"占用/外出但没有在住客户"的孤儿床位
        selfHealOrphanBeds();
    }

    /**
     * 床位处于占用/外出且已无生效入住记录时释放为空闲（幂等）
     */
    private void releaseBedIfNoLiveDetails(Integer bedId) {
        if (bedId == null) {
            return;
        }
        Bed bed = this.getById(bedId);
        if (bed == null || bed.getBed_status() == null || bed.getBed_status() == BED_STATUS_FREE) {
            return;
        }
        Long liveCount = bedDetailsMapper.selectCount(new QueryWrapper<BedDetails>()
                .eq("bed_id", bedId)
                .eq("is_deleted", 0));
        if (liveCount == null || liveCount == 0) {
            bed.setBed_status(BED_STATUS_FREE);
            this.updateById(bed);
            log.info("释放无生效入住记录的床位[{}]", bedId);
        }
    }

    /**
     * 床位一致性自愈：释放"占用/外出但没有在住客户"的孤儿床位。
     * 兜底历史上退住/到期释放时漏放的床位，保证仪表盘统计（按床位状态）
     * 与在住客户列表（按客户）口径一致。
     */
    private void selfHealOrphanBeds() {
        List<Bed> busyBeds = this.list(new QueryWrapper<Bed>()
                .in("bed_status", BED_STATUS_OCCUPIED, BED_STATUS_AWAY));
        if (busyBeds == null || busyBeds.isEmpty()) {
            return;
        }
        for (Bed bed : busyBeds) {
            BedDetails live = bedDetailsMapper.selectOne(new QueryWrapper<BedDetails>()
                    .eq("bed_id", bed.getId())
                    .eq("is_deleted", 0)
                    .orderByDesc("id")
                    .last("LIMIT 1"));
            if (live == null) {
                // 床位上已无任何生效入住记录：直接释放
                bed.setBed_status(BED_STATUS_FREE);
                this.updateById(bed);
                log.info("自愈释放孤儿床位[{}]", bed.getId());
                continue;
            }
            Customer customer = live.getCustomer_id() == null ? null
                    : customerMapper.selectById(live.getCustomer_id());
            if (customer == null || (customer.getIs_deleted() != null && customer.getIs_deleted() == 1)) {
                // 生效记录挂在已退住客户上：关闭记录并释放床位
                live.setEnd_date(new Date());
                live.setIs_deleted(1);
                bedDetailsMapper.updateById(live);
                bed.setBed_status(BED_STATUS_FREE);
                this.updateById(bed);
                log.info("自愈关闭已退住客户的入住记录[{}]并释放床位[{}]", live.getId(), bed.getId());
            }
        }
    }

    /**
     * 修改床位使用详情（仅endDate）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateBedDetailsEndDate(Integer id, Date endDate) {
        BedDetails details = bedDetailsMapper.selectById(id);
        if (details == null) {
            throw new RuntimeException("床位使用详情不存在");
        }
        if (endDate != null && details.getStart_date() != null && endDate.before(details.getStart_date())) {
            throw new RuntimeException("结束日期不能早于入住开始日期");
        }
        details.setEnd_date(endDate);
        bedDetailsMapper.updateById(details);
    }

    /**
     * 床位调换
     * 逻辑：目标床位必须空闲 -> 旧记录失效(逻辑删除+endDate=当天) -> 新建记录 ->
     *      旧床空闲、新床入住 -> 同步更新客户房间/床位/楼栋
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void transferBed(BedTransferDTO dto) {
        // 1. 校验客户
        Customer customer = customerMapper.selectById(dto.getCustomerId());
        if (customer == null) {
            throw new RuntimeException("客户不存在");
        }
        if (customer.getBed_id() == null) {
            throw new RuntimeException("该客户未分配床位");
        }
        // 校验旧床位ID与客户当前床位一致
        if (dto.getOldBedId() != null && !dto.getOldBedId().equals(customer.getBed_id())) {
            throw new RuntimeException("旧床位ID与客户当前床位不一致");
        }

        // 2. 校验目标床位是否空闲
        Bed newBed = this.getById(dto.getNewBedId());
        if (newBed == null) {
            throw new RuntimeException("目标床位不存在");
        }
        if (newBed.getBed_status() == null || newBed.getBed_status() != BED_STATUS_FREE) {
            throw new RuntimeException("目标床位不是空闲状态，无法调换");
        }
        if (newBed.getId().equals(customer.getBed_id())) {
            throw new RuntimeException("目标床位与客户当前床位相同");
        }

        Date today = new Date();

        // 3. 旧 beddetails：逻辑删除 + endDate = 当天
        QueryWrapper<BedDetails> wrapper = new QueryWrapper<>();
        wrapper.eq("customer_id", customer.getId())
                .eq("is_deleted", 0)
                .and(w -> w.isNull("end_date").or().gt("end_date", new Date()))
                .orderByDesc("id")
                .last("LIMIT 1");
        BedDetails oldDetails = bedDetailsMapper.selectOne(wrapper);
        if (oldDetails != null) {
            oldDetails.setEnd_date(today);
            oldDetails.setIs_deleted(1);
            bedDetailsMapper.updateById(oldDetails);
        }

        // 4. 新建 beddetails：startDate = 当天，endDate = 客户合同到期日
        BedDetails newDetails = new BedDetails();
        newDetails.setCustomer_id(customer.getId());
        newDetails.setBed_id(newBed.getId());
        newDetails.setStart_date(today);
        // 退住时间=客户合同到期日（与客户当前生效记录口径一致）
        newDetails.setEnd_date(customer.getExpiration_date());
        newDetails.setBed_details(dto.getBedDetails());
        newDetails.setIs_deleted(0);
        bedDetailsMapper.insert(newDetails);

        // 5. 旧床空闲、新床入住
        Integer oldBedId = customer.getBed_id();
        Bed oldBed = this.getById(oldBedId);
        if (oldBed != null) {
            oldBed.setBed_status(BED_STATUS_FREE);
            this.updateById(oldBed);
        }
        newBed.setBed_status(BED_STATUS_OCCUPIED);
        this.updateById(newBed);

        // 6. 更新客户：房间号、床位ID、楼栋
        customer.setRoom_no(String.valueOf(newBed.getRoom_no()));
        customer.setBed_id(newBed.getId());
        customer.setBuilding_no(BUILDING_NO);
        customerMapper.updateById(customer);

        log.info("客户 [{}] 床位调换成功：床位{} -> 床位{}", customer.getId(), oldBedId, newBed.getId());
    }

    /**
     * 房间列表（按楼层分组）
     */
    @Override
    public List<RoomVO> listRooms() {
        QueryWrapper<Room> wrapper = new QueryWrapper<>();
        wrapper.orderByAsc("room_floor").orderByAsc("room_no");
        List<Room> rooms = roomMapper.selectList(wrapper);
        return rooms.stream().map(room -> {
            RoomVO vo = new RoomVO();
            vo.setId(room.getId());
            vo.setRoomFloor(room.getRoom_floor());
            vo.setRoomNo(room.getRoom_no());
            return vo;
        }).collect(Collectors.toList());
    }

    /**
     * 按房间号查空闲床位
     */
    @Override
    public List<BedVO> listFreeBeds(Integer roomNo) {
        QueryWrapper<Bed> wrapper = new QueryWrapper<>();
        wrapper.eq("room_no", roomNo)
                .eq("bed_status", BED_STATUS_FREE)
                .orderByAsc("bed_no");
        return this.list(wrapper).stream().map(this::toBedVO).collect(Collectors.toList());
    }

    private BedVO toBedVO(Bed bed) {
        BedVO vo = new BedVO();
        vo.setId(bed.getId());
        vo.setBedNo(bed.getBed_no());
        vo.setRoomNo(bed.getRoom_no());
        vo.setBedStatus(bed.getBed_status());
        vo.setRemarks(bed.getRemarks());
        return vo;
    }
}