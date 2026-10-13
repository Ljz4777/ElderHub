package com.it.elderhub.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 膳食日历表
 * @TableName meal
 */
@TableName(value ="meal")
@Data
public class Meal {
    /**
     * 菜单ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    /**
     * 星期几
     */
    @TableField(value = "week_day")
    private String week_day;

    /**
     * 餐次类型（1:早餐 2:午餐 3:晚餐）
     */
    @TableField(value = "meal_type")
    private Integer meal_type;

    /**
     * 食品ID
     */
    @TableField(value = "food_id")
    private Integer food_id;

    /**
     * 口味要求（少糖/少盐等）
     */
    @TableField(value = "taste")
    private String taste;

    /**
     * 状态（1:启用 2:停用）
     */
    @TableField(value = "status")
    private Integer status;

    /**
     * 逻辑删除（0:正常 1:删除）
     */
    @TableField(value = "is_deleted")
    private Integer is_deleted;
}