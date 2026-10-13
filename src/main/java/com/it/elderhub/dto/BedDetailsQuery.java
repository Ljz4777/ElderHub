package com.it.elderhub.dto;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.Date;

/**
 * 床位使用详情查询对象
 */
@Data
public class BedDetailsQuery {

    // 客户姓名（模糊查询）
    private String customerName;

    // 房间编号
    private Integer roomNo;

    // 入住日期
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private Date startDate;

    // 状态：1-正在使用 2-使用历史（默认查询正在使用）
    private Integer status = 1;

    // 老人类型：1-自理老人（未配置护理项目，默认） 2-护理老人（已配置护理项目） 0/null-全部
    private Integer customerType = 1;

    private Integer pageNum = 1;

    private Integer pageSize = 10;
}
