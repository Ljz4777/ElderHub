package com.it.elderhub.vo;

import lombok.Data;

import java.util.List;

/**
 * 床位示意图 VO（房间 + 床位列表）
 */
@Data
public class RoomBedVO {

    private Integer id;

    // 所在楼层
    private String roomFloor;

    // 房间编号
    private Integer roomNo;

    // 该房间下的床位列表
    private List<BedVO> beds;
}
