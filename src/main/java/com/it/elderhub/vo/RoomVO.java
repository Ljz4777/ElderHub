package com.it.elderhub.vo;

import lombok.Data;

/**
 * 房间 VO
 */
@Data
public class RoomVO {

    private Integer id;

    // 所在楼层
    private String roomFloor;

    // 房间编号
    private Integer roomNo;
}
