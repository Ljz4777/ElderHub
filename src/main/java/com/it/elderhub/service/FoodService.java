package com.it.elderhub.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.it.elderhub.entity.Food;

/**
 * 食品信息Service接口
 */
public interface FoodService {

    /**
     * 分页查询食品
     * @param pageNum 页码（默认1）
     * @param pageSize 每页条数（默认10）
     * @param foodName 食品名称模糊查询（可空）
     * @param foodType 食品分类筛选（可空）
     * @param isHalal 是否清真筛选（0:否 1:是，可空）
     * @return 分页结果
     */
    IPage<Food> listFoods(Integer pageNum, Integer pageSize, String foodName, String foodType, Integer isHalal);

    /**
     * 查询食品详情
     * @param id 食品ID
     * @return 食品信息
     */
    Food getFoodById(Integer id);

    /**
     * 新增食品
     * @param food 食品信息
     */
    void saveFood(Food food);

    /**
     * 修改食品
     * @param food 食品信息（必须带ID）
     */
    void updateFood(Food food);

    /**
     * 删除食品（food表无is_deleted字段，采用物理删除）
     * @param id 食品ID
     */
    void removeFood(Integer id);
}
