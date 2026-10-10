package com.it.elderhub.vo;

import lombok.Data;

import java.util.Date;

/**
 * 床位使用详情 VO
 */
@Data
public class BedDetailsVO {

    private Integer id;

    private Integer customerId;

    // 客户姓名（关联查询）
    private String customerName;

    private Integer bedId;

    // 床位编号（关联查询）
    private String bedNo;

    // 房间编号（关联查询）
    private Integer roomNo;

    // 入住开始日期
    private Date startDate;

    // 退住结束日期
    private Date endDate;

    // 床位详情说明
    private String bedDetails;

    // 状态：1-正在使用 2-使用历史
    private Integer status;

    // 状态描述文本
    private String statusMsg;
}
