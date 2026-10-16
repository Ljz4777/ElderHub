package com.it.elderhub.dto;

import lombok.Data;

@Data
public class OutwardQuery {
    private String customerName;
    // 按客户ID精确查询（管家端下拉选择客户后使用）
    private Integer customerId;
    private Integer pageNum = 1;
    private Integer pageSize = 10;
}