package com.it.elderhub.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.it.elderhub.entity.Customer;
import com.it.elderhub.entity.CustomerMeal;
import com.it.elderhub.entity.Food;
import com.it.elderhub.mapper.CustomerMapper;
import com.it.elderhub.mapper.CustomerMealMapper;
import com.it.elderhub.mapper.FoodMapper;
import com.it.elderhub.service.CustomerMealService;
import com.it.elderhub.vo.CustomerMealVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 客户膳食配置Service实现类
 */
@Service
public class CustomerMealServiceImpl implements CustomerMealService {

    @Autowired
    private CustomerMealMapper customerMealMapper;

    @Autowired
    private CustomerMapper customerMapper;

    @Autowired
    private FoodMapper foodMapper;

    @Override
    public IPage<CustomerMealVO> listCustomerMeals(Integer pageNum, Integer pageSize, Integer customerId, String customerName) {
        QueryWrapper<CustomerMeal> wrapper = new QueryWrapper<>();
        wrapper.eq("is_deleted", 0);
        if (customerId != null) {
            wrapper.eq("customer_id", customerId);
        }
        if (customerName != null && !customerName.isEmpty()) {
            wrapper.apply("customer_id IN (SELECT id FROM customer WHERE customer_name LIKE {0})",
                    "%" + customerName + "%");
        }
        wrapper.orderByAsc("customer_id").orderByAsc("week_day").orderByAsc("meal_type");

        Page<CustomerMeal> page = new Page<>(pageNum, pageSize);
        IPage<CustomerMeal> doPage = customerMealMapper.selectPage(page, wrapper);

        List<CustomerMeal> records = doPage.getRecords();
        Map<Integer, String> customerNameMap = records.isEmpty() ? Map.of()
                : customerMapper.selectBatchIds(records.stream().map(CustomerMeal::getCustomer_id).distinct().collect(Collectors.toList()))
                .stream().collect(Collectors.toMap(Customer::getId, Customer::getCustomer_name, (v1, v2) -> v1));
        Map<Integer, String> foodNameMap = records.isEmpty() ? Map.of()
                : foodMapper.selectBatchIds(records.stream().map(CustomerMeal::getFood_id).distinct().collect(Collectors.toList()))
                .stream().collect(Collectors.toMap(Food::getId, Food::getFood_name, (v1, v2) -> v1));

        return doPage.convert(item -> {
            CustomerMealVO vo = new CustomerMealVO();
            vo.setId(item.getId());
            vo.setCustomerId(item.getCustomer_id());
            vo.setCustomerName(customerNameMap.get(item.getCustomer_id()));
            vo.setWeekDay(item.getWeek_day());
            vo.setMealType(item.getMeal_type());
            vo.setFoodId(item.getFood_id());
            vo.setFoodName(foodNameMap.get(item.getFood_id()));
            return vo;
        });
    }

    @Override
    public void saveCustomerMeal(CustomerMeal customerMeal) {
        customerMeal.setId(null);
        customerMeal.setIs_deleted(0);
        customerMealMapper.insert(customerMeal);
    }

    @Override
    public void updateCustomerMeal(CustomerMeal customerMeal) {
        if (customerMeal.getId() == null) {
            throw new RuntimeException("配置ID不能为空");
        }
        customerMealMapper.updateById(customerMeal);
    }

    @Override
    public void removeCustomerMeal(Integer id) {
        CustomerMeal customerMeal = new CustomerMeal();
        customerMeal.setId(id);
        customerMeal.setIs_deleted(1);
        customerMealMapper.updateById(customerMeal);
    }
}
