package com.it.elderhub.vo;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class OutwardVO {
    private Integer id;          // 外出记录ID（审批/回院操作需要）
    private Integer customerId;
    private String customerName; // 关联查询出的客户名
    private String outgoingReason;
    private LocalDateTime outgoingTime;
    private LocalDateTime expectedReturnTime;
    private LocalDateTime actualReturnTime;
    private String escorted;
    private String relation;
    private String escortedTel;
    private Integer auditStatus;
    private String auditPerson;
    private LocalDateTime auditTime;
    private String remarks;
}