package com.it.elderhub.vo;

import lombok.Data;

/**
 * 床位统计 VO
 */
@Data
public class BedStatisticsVO {

    // 床位总数
    private Long totalBeds;

    // 空闲床位数量
    private Long freeBeds;

    // 已入住床位数量
    private Long occupiedBeds;

    // 外出床位数量
    private Long awayBeds;
}
