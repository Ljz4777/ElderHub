package com.it.elderhub.vo;

import lombok.Data;

/**
 * 客户膳食配置VO（联表带出客户姓名与食品名称）
 */
@Data
public class CustomerMealVO {
    private Integer id;
    private Integer customerId;
    private String customerName;
    private String weekDay;
    private Integer mealType;
    private Integer foodId;
    private String foodName;
}
