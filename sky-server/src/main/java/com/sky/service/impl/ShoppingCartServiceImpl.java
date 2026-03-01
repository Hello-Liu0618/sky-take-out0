package com.sky.service.impl;

import com.sky.context.BaseContext;
import com.sky.dto.ShoppingCartDTO;
import com.sky.entity.Dish;
import com.sky.entity.Setmeal;
import com.sky.entity.ShoppingCart;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealMapper;
import com.sky.mapper.ShoppingCartMapper;
import com.sky.result.Result;
import com.sky.service.DishService;
import com.sky.service.ShoppingCartService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
public class ShoppingCartServiceImpl implements ShoppingCartService {

    @Autowired
    private ShoppingCartMapper shoppingCartMapper;

    @Autowired
    private DishMapper dishMapper;
    @Autowired
    private SetmealMapper setmealMapper;

    /**
     * 添加购物车
     * @param shoppingCartDTO
     * @return
     */
    public Result add(ShoppingCartDTO shoppingCartDTO) {
        //首先判断购物车中是否已存在该商品
        ShoppingCart shoppingCart =  new ShoppingCart();
        BeanUtils.copyProperties(shoppingCartDTO, shoppingCart);
        shoppingCart.setUserId(BaseContext.getCurrentId());
        List<ShoppingCart> list = shoppingCartMapper.list(shoppingCart);
        //若存在则执行修改数据库操作, 将商品数量加一
        if(list != null && list.size()>0){
            ShoppingCart cart = list.get(0);
            cart.setNumber(cart.getNumber() + 1);
            shoppingCartMapper.updateNumberById(cart);
        }else {//否则在数据库中加入商品数据
             Long dishId = shoppingCart.getDishId();
             if ( dishId != null ) {//dishId非空则认为shoppingCart为dish类型
                 Dish dish = dishMapper.getById(dishId);
                 shoppingCart.setName(dish.getName());
                 shoppingCart.setImage(dish.getImage());
                 shoppingCart.setAmount(dish.getPrice());
             } else{//认为为setmeal类型
                 Long setmealId = shoppingCart.getSetmealId();
                 Setmeal setmeal = setmealMapper.getById(setmealId);
                 shoppingCart.setName(setmeal.getName());
                 shoppingCart.setImage(setmeal.getImage());
                 shoppingCart.setAmount(setmeal.getPrice());
             }
             shoppingCart.setNumber(1);
             shoppingCart.setCreateTime(LocalDateTime.now());
             shoppingCartMapper.insert(shoppingCart);
        }
        return Result.success();
    }

    public List<ShoppingCart> list() {
        Long userId = BaseContext.getCurrentId();
        List<ShoppingCart> list = shoppingCartMapper.getByUserId(userId);
        return list;
    }
}
