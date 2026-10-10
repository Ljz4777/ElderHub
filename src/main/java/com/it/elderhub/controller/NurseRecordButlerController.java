package com.it.elderhub.controller;

import com.it.elderhub.common.Result;
import com.it.elderhub.dto.NurseRecordAddDTO;
import com.it.elderhub.service.NurseRecordService;
import com.it.elderhub.vo.NurseRecordVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

/**
 * 管家：录入护理记录
 * POST /nurse/nurse-record-mine/save
 */
@RestController
@RequestMapping("/nurse/nurse-record-mine")
public class NurseRecordButlerController {

    @Autowired
    private NurseRecordService nurseRecordService;

    /**
     * 管家：录入护理记录
     * POST /nurse/nurse-record/save
     */
    @PostMapping("/save")
    public Result<Void> save(@RequestBody NurseRecordAddDTO dto) {
        nurseRecordService.saveNurseRecord(dto);
        return Result.success(null);
    }

    /**
     * 管家：查看自己客户的护理记录
     * GET /nurse/nurse-record/list/{customerId}
     */
    @GetMapping("/list/{customerId}")
    public Result<List<NurseRecordVO>> getButlerList(@PathVariable Integer customerId,
                                                     @RequestParam Integer userId) {
        return Result.success(nurseRecordService.listNurseRecordsByUserId(userId));
    }
}