package com.it.elderhub.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.it.elderhub.entity.Meal;

/**
 * 膳食日历Service接口
 */
public interface MealService {

    /**
     * 分页查询膳食日历
     * @param pageNum 页码（默认1）
     * @param pageSize 每页条数（默认10）
     * @param weekDay 星期几筛选（可空）
     * @param mealType 餐次类型筛选（1:早餐 2:午餐 3:晚餐，可空）
     * @return 分页结果
     */
    IPage<Meal> listMeals(Integer pageNum, Integer pageSize, String weekDay, Integer mealType);

    /**
     * 查询膳食详情
     * @param id 菜单ID
     * @return 膳食信息
     */
    Meal getMealById(Integer id);

    /**
     * 新增膳食
     * @param meal 膳食信息
     */
    void saveMeal(Meal meal);

    /**
     * 修改膳食
     * @param meal 膳食信息（必须带ID）
     */
    void updateMeal(Meal meal);

    /**
     * 删除膳食（逻辑删除）
     * @param id 菜单ID
     */
    void removeMeal(Integer id);
}
