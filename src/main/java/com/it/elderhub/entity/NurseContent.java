package com.it.elderhub.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 护理项目表
 * @TableName nursecontent
 */
@Data
@TableName(value = "nurse_content")
public class NurseContent {

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    /**
     * 项目编号（如：HL001）
     */
    @TableField(value = "serial_number")
    private String serial_number;

    /**
     * 护理项目名称（如：晨间护理、口腔护理）
     */
    @TableField(value = "nursing_name")
    private String nursing_name;

    /**
     * 服务价格
     */
    @TableField(value = "service_price")
    private Integer service_price;

    /**
     * 项目描述
     */
    @TableField(value = "message")
    private String message;

    /**
     * 项目状态（1:启用 2:停用）
     */
    @TableField(value = "status")
    private Integer status;

    /**
     * 执行周期（如：每日、每周）
     */
    @TableField(value = "execution_cycle")
    private String execution_cycle;

    /**
     * 执行次数（如：1次/天）
     */
    @TableField(value = "execution_times")
    private String execution_times;

    /**
     * 逻辑删除（0:正常 1:删除）
     */
    @TableField(value = "is_deleted")
    private Integer is_deleted;
}
