package com.it.elderhub.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.it.elderhub.entity.Meal;
import com.it.elderhub.mapper.MealMapper;
import com.it.elderhub.service.MealService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 膳食日历Service实现类
 */
@Service
public class MealServiceImpl implements MealService {

    @Autowired
    private MealMapper mealMapper;

    @Override
    public IPage<Meal> listMeals(Integer pageNum, Integer pageSize, String weekDay, Integer mealType, Integer status) {
        LambdaQueryWrapper<Meal> wrapper = new LambdaQueryWrapper<>();
        // 只查未删除的
        wrapper.eq(Meal::getIs_deleted, 0);
        // 默认查询启用状态；status=0 表示查全部
        if (status != null && status != 0) {
            wrapper.eq(Meal::getStatus, status);
        } else if (status == null) {
            wrapper.eq(Meal::getStatus, 1);
        }
        // 按星期筛选
        if (weekDay != null && !weekDay.isEmpty()) {
            wrapper.eq(Meal::getWeek_day, weekDay);
        }
        // 按餐次筛选
        if (mealType != null) {
            wrapper.eq(Meal::getMeal_type, mealType);
        }
        // 按星期、餐次排序
        wrapper.orderByAsc(Meal::getWeek_day).orderByAsc(Meal::getMeal_type);

        Page<Meal> page = new Page<>(pageNum, pageSize);
        return mealMapper.selectPage(page, wrapper);
    }

    @Override
    public Meal getMealById(Integer id) {
        return mealMapper.selectById(id);
    }

    @Override
    public void saveMeal(Meal meal) {
        meal.setIs_deleted(0);
        if (meal.getStatus() == null) {
            meal.setStatus(1); // 默认启用
        }
        mealMapper.insert(meal);
    }

    /**
     * 修改膳食状态（启用/停用）
     */
    @Override
    public void updateMealStatus(Integer id, Integer status) {
        Meal meal = new Meal();
        meal.setId(id);
        meal.setStatus(status);
        mealMapper.updateById(meal);
    }

    @Override
    public void updateMeal(Meal meal) {
        if (meal.getId() == null) {
            throw new RuntimeException("菜单ID不能为空");
        }
        mealMapper.updateById(meal);
    }

    @Override
    public void removeMeal(Integer id) {
        // 逻辑删除
        Meal meal = new Meal();
        meal.setId(id);
        meal.setIs_deleted(1);
        mealMapper.updateById(meal);
    }
}
