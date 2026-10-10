package com.it.elderhub.vo;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class BackDownVO {
    private Integer id;
    private Integer customerId;
    private String customerName;     // 冗余字段：关联查出的客户姓名，方便前端展示
    private LocalDateTime retreatTime;
    private Integer retreatType;     // 0:正常退住, 1:死亡退住, 2:保留床位
    private String retreatReason;
    private Integer auditStatus;     // 0:待审核, 1:同意, 2:拒绝
    private String auditPerson;      // 审批人姓名
    private LocalDate auditTime;
    private String remarks;
}