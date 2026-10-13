package com.it.elderhub.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.it.elderhub.entity.Food;
import com.it.elderhub.mapper.FoodMapper;
import com.it.elderhub.service.FoodService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 食品信息Service实现类
 */
@Service
public class FoodServiceImpl implements FoodService {

    @Autowired
    private FoodMapper foodMapper;

    @Override
    public IPage<Food> listFoods(Integer pageNum, Integer pageSize, String foodName, String foodType, Integer isHalal) {
        LambdaQueryWrapper<Food> wrapper = new LambdaQueryWrapper<>();
        // 按名称模糊查询
        if (foodName != null && !foodName.isEmpty()) {
            wrapper.like(Food::getFood_name, foodName);
        }
        // 按分类筛选
        if (foodType != null && !foodType.isEmpty()) {
            wrapper.eq(Food::getFood_type, foodType);
        }
        // 按清真筛选
        if (isHalal != null) {
            wrapper.eq(Food::getIs_halal, isHalal);
        }
        // 按ID升序
        wrapper.orderByAsc(Food::getId);

        Page<Food> page = new Page<>(pageNum, pageSize);
        return foodMapper.selectPage(page, wrapper);
    }

    @Override
    public Food getFoodById(Integer id) {
        return foodMapper.selectById(id);
    }

    @Override
    public void saveFood(Food food) {
        // 默认非清真
        if (food.getIs_halal() == null) {
            food.setIs_halal(0);
        }
        foodMapper.insert(food);
    }

    @Override
    public void updateFood(Food food) {
        if (food.getId() == null) {
            throw new RuntimeException("食品ID不能为空");
        }
        foodMapper.updateById(food);
    }

    @Override
    public void removeFood(Integer id) {
        // food 表无 is_deleted 字段，采用物理删除
        foodMapper.deleteById(id);
    }
}
