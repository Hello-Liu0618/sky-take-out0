package com.sky.mapper;

import com.sky.entity.SetmealDish;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface SetmealDishMapper {

    /**
     * 根据菜品Id查出对应的套餐Id
     * @param dishIds
     * @return
     */
    List<Long> getSetmealIdsByDishIds(List<Long> dishIds);

    /**
     * 在套餐菜品表中插入套餐中的菜品们
     * @param setmealDishes
     */
    void insertBatch(List<SetmealDish> setmealDishes);
}
