package com.it.elderhub.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Date;

/**
 * 客户护理项目VO（含项目信息和状态）
 */
@Data
public class CustomerNurseItemVO {

    private Integer id;

    // 护理项目名称 (关联查询 nurse_content 表获取)
    private String nursingName;

    // 剩余次数
    private Integer nurseNumber;

    // 购买日期
    private LocalDate buyTime;

    // 到期日期
    private LocalDate maturityTime;

    // 状态：1-正常/未到期，2-欠费(次数<=0)，3-已到期
    private Integer status;

    // 状态描述文本
    private String statusMsg;

    private String serialNumber;

    private Integer servicePrice;

    private String statusDesc;
}
