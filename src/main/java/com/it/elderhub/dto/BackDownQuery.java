package com.it.elderhub.dto;

import lombok.Data;

@Data
public class BackDownQuery {
    private Integer pageNum = 1;     // 页码，默认第1页
    private Integer pageSize = 10;   // 每页条数，默认10条
    private String customerName;     // 客户姓名模糊查询
    private Integer customerId;      // 客户ID精确查询（管家端下拉选择客户后使用）
}