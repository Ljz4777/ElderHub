package com.it.elderhub.controller;

import com.it.elderhub.common.Result;
import com.it.elderhub.dto.BedDetailsQuery;
import com.it.elderhub.dto.BedTransferDTO;
import com.it.elderhub.service.BedService;
import com.it.elderhub.vo.BedDetailsVO;
import com.it.elderhub.vo.BedStatisticsVO;
import com.it.elderhub.vo.BedVO;
import com.it.elderhub.vo.PageResult;
import com.it.elderhub.vo.RoomBedVO;
import com.it.elderhub.vo.RoomVO;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;
import java.util.List;

/**
 * 床位管理（管理员）
 */
@RestController
@RequestMapping("/admin/bed")
public class BedController {

    private final BedService bedService;

    public BedController(BedService bedService) {
        this.bedService = bedService;
    }

    /**
     * 床位统计
     * GET /admin/bed/statistics
     */
    @GetMapping("/statistics")
    public Result<BedStatisticsVO> statistics() {
        return Result.success(bedService.getBedStatistics());
    }

    /**
     * 床位示意图（默认第一层）
     * GET /admin/bed/map
     */
    @GetMapping("/map")
    public Result<List<RoomBedVO>> map(@RequestParam(required = false) String floor) {
        return Result.success(bedService.listBedMap(floor));
    }

    /**
     * 床位使用详情分页
     * GET /admin/bed/details/list
     */
    @GetMapping("/details/list")
    public Result<PageResult<BedDetailsVO>> detailsList(BedDetailsQuery query) {
        return Result.success(bedService.listBedDetails(query));
    }

    /**
     * 修改床位使用详情（仅endDate）
     * PUT /admin/bed/details/update
     */
    @PutMapping("/details/update")
    public Result<Void> updateDetails(@RequestParam Integer id,
                                      @RequestParam(required = false)
                                      @DateTimeFormat(pattern = "yyyy-MM-dd") Date endDate) {
        bedService.updateBedDetailsEndDate(id, endDate);
        return Result.success();
    }

    /**
     * 床位调换
     * POST /admin/bed/transfer
     */
    @PostMapping("/transfer")
    public Result<Void> transfer(@RequestBody BedTransferDTO dto) {
        bedService.transferBed(dto);
        return Result.success();
    }

    /**
     * 房间列表（按楼层分组）
     * GET /admin/bed/room/list
     */
    @GetMapping("/room/list")
    public Result<List<RoomVO>> roomList() {
        return Result.success(bedService.listRooms());
    }

    /**
     * 按房间号查空闲床位
     * GET /admin/bed/free/{roomNo}
     */
    @GetMapping("/free/{roomNo}")
    public Result<List<BedVO>> freeBeds(@PathVariable Integer roomNo) {
        return Result.success(bedService.listFreeBeds(roomNo));
    }
}
