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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
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
     * 床位示意图（默认第一层）
     */
    @Override
    public List<RoomBedVO> listBedMap(String floor) {
        // 未指定楼层时，默认取第一层
        if (floor == null || floor.isEmpty()) {
            QueryWrapper<Room> firstWrapper = new QueryWrapper<>();
            firstWrapper.orderByAsc("room_floor").last("LIMIT 1");
            Room firstRoom = roomMapper.selectOne(firstWrapper);
            if (firstRoom == null) {
                return List.of();
            }
            floor = firstRoom.getRoom_floor();
        }

        QueryWrapper<Room> roomWrapper = new QueryWrapper<>();
        roomWrapper.eq("room_floor", floor).orderByAsc("room_no");
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
        if (query.getStatus() != null && query.getStatus() == 1) {
            // 正在使用：未删除 且（endDate 为空 或 endDate > 当前日期）
            wrapper.eq("is_deleted", 0)
                    .and(w -> w.isNull("end_date").or().gt("end_date", new Date()));
        } else if (query.getStatus() != null && query.getStatus() == 2) {
            wrapper.and(w -> w.eq("is_deleted", 1).or().le("end_date", new Date()));
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

            Date now = new Date();
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

                boolean inUse = isInUse(details, now);
                vo.setStatus(inUse ? 1 : 2);
                vo.setStatusMsg(inUse ? "正在使用" : "使用历史");
                list.add(vo);
            }
        }

        return new PageResult<>(list, doPage.getTotal(), doPage.getCurrent(), doPage.getSize());
    }

    private boolean isInUse(BedDetails details, Date now) {
        boolean notDeleted = details.getIs_deleted() == null || details.getIs_deleted() == 0;
        boolean notEnded = details.getEnd_date() == null || details.getEnd_date().after(now);
        return notDeleted && notEnded;
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

        // 4. 新建 beddetails：startDate = 当天，endDate = null
        BedDetails newDetails = new BedDetails();
        newDetails.setCustomer_id(customer.getId());
        newDetails.setBed_id(newBed.getId());
        newDetails.setStart_date(today);
        newDetails.setEnd_date(null);
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