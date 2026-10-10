package com.it.elderhub.service;

import com.it.elderhub.dto.BedDetailsQuery;
import com.it.elderhub.dto.BedTransferDTO;
import com.it.elderhub.entity.Bed;
import com.baomidou.mybatisplus.extension.service.IService;
import com.it.elderhub.vo.BedDetailsVO;
import com.it.elderhub.vo.BedStatisticsVO;
import com.it.elderhub.vo.BedVO;
import com.it.elderhub.vo.PageResult;
import com.it.elderhub.vo.RoomBedVO;
import com.it.elderhub.vo.RoomVO;

import java.util.Date;
import java.util.List;

/**
* @author Ljz
* @description 针对表【bed(床位信息表)】的数据库操作Service
* @createDate 2026-10-08 09:52:05
*/
public interface BedService extends IService<Bed> {

    /**
     * 床位统计（总数/空闲/已入住/外出）
     */
    BedStatisticsVO getBedStatistics();

    /**
     * 床位示意图（默认第一层）
     */
    List<RoomBedVO> listBedMap(String floor);

    /**
     * 床位使用详情分页
     */
    PageResult<BedDetailsVO> listBedDetails(BedDetailsQuery query);

    /**
     * 修改床位使用详情（仅endDate）
     */
    void updateBedDetailsEndDate(Integer id, Date endDate);

    /**
     * 床位调换
     */
    void transferBed(BedTransferDTO dto);

    /**
     * 房间列表（按楼层分组）
     */
    List<RoomVO> listRooms();

    /**
     * 按房间号查空闲床位
     */
    List<BedVO> listFreeBeds(Integer roomNo);
}
