package com.it.elderhub.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDate;
import java.util.Date;
import lombok.Data;
import org.springframework.cglib.core.Local;

/**
 * 在住客户/老人表
 * @TableName customer
 */
@TableName(value ="customer")
@Data
public class Customer {
    /**
     * 客户ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    /**
     * 老人姓名
     */
    @TableField(value = "customer_name")
    private String customer_name;

    /**
     * 年龄
     */
    @TableField(value = "customer_age")
    private Integer customer_age;

    /**
     * 性别（0:男 1:女）
     */
    @TableField(value = "customer_sex")
    private Integer customer_sex;

    /**
     * 身份证号
     */
    @TableField(value = "idcard")
    private String idcard;

    /**
     * 房间号
     */
    @TableField(value = "room_no")
    private String room_no;

    /**
     * 所属楼栋
     */
    @TableField(value = "building_no")
    private String building_no;

    /**
     * 床位ID
     */
    @TableField(value = "bed_id")
    private Integer bed_id;

    /**
     * 入住日期
     */
    @TableField(value = "checkin_date")
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    private Date checkin_date;

    /**
     * 合同到期日期
     */
    @TableField(value = "expiration_date")
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    private Date expiration_date;

    /**
     * 联系电话
     */
    @TableField(value = "contact_tel")
    private String contact_tel;

    /**
     * 出生日期
     */
    @TableField(value = "birthday")
    private LocalDate birthday;

    /**
     * 身高
     */
    @TableField(value = "height")
    private String height;

    /**
     * 体重
     */
    @TableField(value = "weight")
    private String weight;

    /**
     * 血型
     */
    @TableField(value = "blood_type")
    private String blood_type;

    /**
     * 身心状况描述
     */
    @TableField(value = "psychosomatic_state")
    private String psychosomatic_state;

    /**
     * 护理注意事项
     */
    @TableField(value = "attention")
    private String attention;

    /**
     * 家属
     */
    @TableField(value = "family_member")
    private String family_member;

    /**
     * 头像文件路径
     */
    @TableField(value = "filepath")
    private String filepath;

    /**
     * 逻辑删除（0:正常 1:删除）
     */
    @TableField(value = "is_deleted")
    private Integer is_deleted;
}