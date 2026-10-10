package com.it.elderhub.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;
import lombok.Data;
import org.springframework.cglib.core.Local;

/**
 * 退住登记表
 * @TableName backdown
 */
@TableName(value ="backdown")
@Data
public class Backdown {
    /**
     * 登记ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    /**
     * 客户ID
     */
    @TableField(value = "customer_id")
    private Integer customer_id;

    /**
     * 退住时间
     */
    @TableField(value = "retreattime")
    private LocalDateTime retreattime;

    /**
     * 退住类型（0:正常退住 1:死亡退住 2:保留床位）
     */
    @TableField(value = "retreattype")
    private Integer retreattype;

    /**
     * 退住原因
     */
    @TableField(value = "retreatreason")
    private String retreatreason;

    /**
     * 审批状态（0:已提交 1:同意 2:拒绝）
     */
    @TableField(value = "auditstatus")
    private Integer auditstatus;

    /**
     * 审批人姓名
     */
    @TableField(value = "auditperson")
    private String auditperson;

    /**
     * 审批时间
     */
    @TableField(value = "audittime")
    private LocalDateTime audittime;

    /**
     * 备注
     */
    @TableField(value = "remarks")
    private String remarks;

    /**
     * 逻辑删除（0:正常 1:删除）
     */
    @TableField(value = "is_deleted")
    private Integer is_deleted;
}