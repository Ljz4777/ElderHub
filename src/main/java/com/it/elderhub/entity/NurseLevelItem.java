package com.it.elderhub.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 护理级别与护理项目关联表
 * @TableName nurselevelitem
 */
@Data
@TableName(value = "nurse_level_item")
public class NurseLevelItem {

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    /**
     * 护理级别ID（关联nurselevel表）
     */
    @TableField(value = "level_id")
    private Integer level_id;

    /**
     * 护理项目ID（关联nursecontent表）
     */
    @TableField(value = "item_id")
    private Integer item_id;
}
