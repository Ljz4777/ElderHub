package com.it.elderhub.vo;

import lombok.Data;

/**
 * 床位 VO
 */
@Data
public class BedVO {

    private Integer id;

    // 床位编号
    private String bedNo;

    // 所属房间编号
    private Integer roomNo;

    // 床位状态（1:空闲 2:已入住 3:外出）
    private Integer bedStatus;

    // 备注
    private String remarks;
}
