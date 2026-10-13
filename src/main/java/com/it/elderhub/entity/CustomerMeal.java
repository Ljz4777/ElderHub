package com.it.elderhub.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 客户膳食配置表
 * @TableName customer_meal
 */
@TableName(value = "customer_meal")
@Data
public class CustomerMeal {
    /**
     * 配置ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    /**
     * 客户ID
     */
    @TableField(value = "customer_id")
    private Integer customer_id;

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
     * 逻辑删除（0:正常 1:删除）
     */
    @TableField(value = "is_deleted")
    private Integer is_deleted;
}
