package com.it.elderhub.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.it.elderhub.common.Result;
import com.it.elderhub.entity.Meal;
import com.it.elderhub.service.MealService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 膳食日历Controller
 */
@RestController
@RequestMapping("/admin/meal")
public class MealController {

    @Autowired
    private MealService mealService;

    /**
     * 分页查询膳食日历
     * @param pageNum 页码（默认1）
     * @param pageSize 每页条数（默认10）
     * @param weekDay 星期几筛选（可空）
     * @param mealType 餐次类型筛选（1:早餐 2:午餐 3:晚餐，可空）
     * @param status 状态筛选（1:启用 2:停用，可空，默认启用）
     */
    @GetMapping("/list")
    public Result<IPage<Meal>> list(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            String weekDay,
            Integer mealType,
            Integer status) {
        return Result.success(mealService.listMeals(pageNum, pageSize, weekDay, mealType, status));
    }

    /**
     * 修改膳食状态（启用/停用）
     * PUT /admin/meal/update-status?id=1&status=2
     */
    @PutMapping("/update-status")
    public Result<Void> updateStatus(@RequestParam Integer id, @RequestParam Integer status) {
        mealService.updateMealStatus(id, status);
        return Result.success();
    }

    /**
     * 查询膳食详情
     * @param id 菜单ID
     */
    @GetMapping("/{id}")
    public Result<Meal> detail(@PathVariable Integer id) {
        return Result.success(mealService.getMealById(id));
    }

    /**
     * 新增膳食
     */
    @PostMapping("/save")
    public Result<Void> save(@RequestBody Meal meal) {
        mealService.saveMeal(meal);
        return Result.success();
    }

    /**
     * 修改膳食
     */
    @PutMapping("/update")
    public Result<Void> update(@RequestBody Meal meal) {
        mealService.updateMeal(meal);
        return Result.success();
    }

    /**
     * 删除膳食（逻辑删除）
     * @param id 菜单ID
     */
    @DeleteMapping("/remove/{id}")
    public Result<Void> remove(@PathVariable Integer id) {
        mealService.removeMeal(id);
        return Result.success();
    }
}
