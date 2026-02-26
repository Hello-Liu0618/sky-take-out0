package com.sky.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.constant.StatusConstant;
import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.entity.DishFlavor;
import com.sky.entity.Setmeal;
import com.sky.exception.DeletionNotAllowedException;
import com.sky.mapper.DishFlavorMapper;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealDishMapper;
import com.sky.mapper.SetmealMapper;
import com.sky.result.PageResult;
import com.sky.service.DishService;
import com.sky.vo.DishVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class DishServiceImpl implements DishService {

    @Autowired
    private DishMapper dishMapper;

    @Autowired
    private DishFlavorMapper dishFlavorMapper;

    @Autowired
    private SetmealDishMapper setmealDishMapper;

    @Autowired
    private SetmealMapper setmealMapper;

    @Autowired
    private RedisTemplate redisTemplate;

    /**
     * 新增菜品的对应的口味
     * @param dishDTO
     */
    @Transactional//注解事务管理，确保整个方法实现的原子化
    public void saveWithFlavor(DishDTO dishDTO) {

        Dish dish = new Dish();
        BeanUtils.copyProperties(dishDTO, dish);
        //向菜品表插入一条数据
        dishMapper.insert(dish);

        //获取insert 语句生成的主键值
        Long id = dish.getId();

        //向口味表插入n条数据.
        List<DishFlavor> flavors = dishDTO.getFlavors();
        if (flavors != null && flavors.size() > 0) {
            flavors.forEach(flavor -> {
                flavor.setDishId(dish.getId());
            });
            dishFlavorMapper.insertBatch(flavors);
        }
    }

    /**
     * 菜品分页查询
     * @param dishPageQueryDTO
     * @return
     */
    public PageResult pageQuery(DishPageQueryDTO dishPageQueryDTO) {
        PageHelper.startPage(dishPageQueryDTO.getPage(), dishPageQueryDTO.getPageSize());
        Page<DishVO> page = dishMapper.pageQuery(dishPageQueryDTO);
        return new PageResult(page.getTotal(), page.getResult());
    }

    /**
     * 菜品批量删除
     * @param ids
     */
    public void deleteBatch(List<Long> ids) {
        //判断当前菜品是否能够删除(即判断是否存在起售中的菜品)
        for (Long id : ids) {
            Dish dish =  dishMapper.getById(id);
            //标记为脏位
            String key = "dish_" + dish.getCategoryId();
            log.info("key: {} 标记缓存脏位", key);
            redisTemplate.opsForHash().put("dirtyByte", key, "1");

            if( dish.getStatus() == StatusConstant.ENABLE) {
                //当前菜品处于起售中状态, 不能删除
                throw new DeletionNotAllowedException(MessageConstant.DISH_ON_SALE);
            }
        }

        //判断当前菜品是否能够删除(即判断菜品是否被套餐关联)
        List<Long> setmealIds = setmealDishMapper.getSetmealIdsByDishIds(ids);
        if(setmealIds != null && !setmealIds.isEmpty()) {
            throw new DeletionNotAllowedException(MessageConstant.DISH_BE_RELATED_BY_SETMEAL);
        }

        //删除菜品表中的菜品数据
        /*
        for(Long id : ids) {
            dishMapper.deleteByIds(id);

            //删除菜品关联的口味数据
            //无需查找，无关有没有直接尝试一次删除
            dishFlavorMapper.deleteByDishId(id);
        }
         */
        //根据菜品id集合批量删除菜品
        dishMapper.deleteByIds(ids);

        //根据菜品id集合批量删除菜品口味
        dishFlavorMapper.deleteByDishIds(ids);
    }

    /**
     * 根据菜品Id查询菜品数据并带有口味数据
     * @param id
     * @return
     */
    public DishVO getByIdWithFlavor(Long id) {
        //根据Id查询菜品数据
        Dish dish = dishMapper.getById(id);

        //根据菜品Id查询口味数据
        List<DishFlavor> dishFlavors = dishFlavorMapper.getByDishId(id);

        //将查询到的数据封装到DishVO中
        DishVO dishVO = new DishVO();
        BeanUtils.copyProperties(dish, dishVO);
        dishVO.setFlavors(dishFlavors);

        return dishVO;
    }

    /**
     * 更新菜品信息并带有口味数据
     * @param dishDTO
     */
    public void updateWithFlavor(DishDTO dishDTO) {
        //修改菜品表基本信息
        Dish dish = new Dish();
        Long originCategoryId = dishMapper.getById(dishDTO.getId()).getCategoryId();
        BeanUtils.copyProperties(dishDTO, dish);
        dishMapper.update(dish);

        //标记缓存脏位
        String key = "dish_" + dish.getCategoryId();
        String key1 = "dish_" + originCategoryId;
        log.info("key: {} 标记缓存脏位", key);
        log.info("key: {} 标记缓存脏位", key1);
        redisTemplate.opsForHash().put("dirtyByte", key, "1");
        redisTemplate.opsForHash().put("dirtyByte", key1, "1");

        //删除原有口味数据，插入新的口味数据
        dishFlavorMapper.deleteByDishId(dishDTO.getId());
        List<DishFlavor> flavors = dishDTO.getFlavors();
        if (flavors != null && flavors.size() > 0) {
            flavors.forEach(flavor -> {
                flavor.setDishId(dishDTO.getId());
            });
            dishFlavorMapper.insertBatch(flavors);
        }
    }

    /**
     * 启用禁用菜品
     * @param status
     * @param id
     */
    public void startOrStop(Integer status, Long id) {
        //判断是否关联了起售中的套餐，若是则不能停售
        if (status == StatusConstant.DISABLE) {
            List<Long> ids = new ArrayList<>();
            ids.add(id);
            List<Long> setmealIds = setmealDishMapper.getSetmealIdsByDishIds(ids);
            setmealIds.forEach(setmealId -> {
                Setmeal setmeal = setmealMapper.getById(setmealId);
                if(setmeal.getStatus() == StatusConstant.ENABLE) {
                    throw new DeletionNotAllowedException(MessageConstant.DISH_BE_RELATED_BY_ON_SALE_SETMEAL);
                }
            });
        }
        Dish dish = Dish.builder().status(status).id(id).build();

        //标记缓存脏位
        String key = "dish_" + dishMapper.getById(id).getCategoryId();
        log.info("key: {} 标记缓存脏位", key);
        redisTemplate.opsForHash().put("dirtyByte", key, "1");

        dishMapper.update(dish);
    }

    public List<Dish> list(Long categoryId) {
        Dish dish = Dish.builder().status(StatusConstant.ENABLE).categoryId(categoryId).build();
        List<Dish> list = dishMapper.list(dish);
        return list;
    }

    /**
     * 条件查询菜品和口味
     * @param dish
     * @return
     */
    public List<DishVO> listWithFlavor(Dish dish) {
        List<Dish> dishList = dishMapper.list(dish);

        List<DishVO> dishVOList = new ArrayList<>();

        for (Dish d : dishList) {
            DishVO dishVO = new DishVO();
            BeanUtils.copyProperties(d,dishVO);

            //根据菜品id查询对应的口味
            List<DishFlavor> flavors = dishFlavorMapper.getByDishId(d.getId());

            dishVO.setFlavors(flavors);
            dishVOList.add(dishVO);
        }

        return dishVOList;
    }


}
