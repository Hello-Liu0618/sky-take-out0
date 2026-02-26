package com.sky.controller.user;

import com.sky.constant.StatusConstant;
import com.sky.entity.Dish;
import com.sky.result.Result;
import com.sky.service.DishService;
import com.sky.vo.DishVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController("userDishController")
@RequestMapping("/user/dish")
@Slf4j
@Api(tags = "C端-菜品浏览接口")
public class DishController {
    @Autowired
    private DishService dishService;
    @Autowired
    private RedisTemplate redisTemplate;

    /**
     * 根据分类id查询菜品
     *
     * @param categoryId
     * @return
     */
    @GetMapping("/list")
    @ApiOperation("根据分类id查询菜品")
    public Result<List<DishVO>> list(Long categoryId) {

        //构造redis 的key
        String key = "dish_" + categoryId;

        //查询redis中是否有菜品数据
        List<DishVO>  dishVOList = (List<DishVO>) redisTemplate.opsForValue().get(key);
        if(dishVOList != null && dishVOList.size()>0){
            log.info("key: {} 缓存命中", key);
            //检查缓存是否标记为脏位
            //若被标记为脏位则缓存失效
            if( redisTemplate.opsForHash().hasKey("dirtyByte", key) ) {
                log.info("key: {} 缓存被标记为脏位", key);
                redisTemplate.opsForValue().set(key, null);
                redisTemplate.opsForHash().delete("dirtyByte", key);
            }
            else {
                //如果有菜品数据且未被污染直接返回，无需查询数据库
                return Result.success(dishVOList);
            }
        } else {
            log.info("key: {} 缓存缺失", key);
        }

        //如果没有，查询数据库并将数据存入redis中
        Dish dish = new Dish();
        dish.setCategoryId(categoryId);
        dish.setStatus(StatusConstant.ENABLE);//查询起售中的菜品

        dishVOList = dishService.listWithFlavor(dish);
        redisTemplate.opsForValue().set(key,dishVOList);

        return Result.success(dishVOList);
    }

}
