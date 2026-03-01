package com.sky.controller.admin;

import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.entity.DishFlavor;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.DishService;
import com.sky.vo.DishVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 菜品管理
 */
@RequestMapping
@RestController
@Api(tags = "菜品相关接口")
@Slf4j
public class DishController {

    @Autowired
    private DishService dishService;
    @Autowired
    private RedisTemplate redisTemplate;

    /**
     * 新增菜品
     * @param dishDTO
     * @return
     */
    @PostMapping("/admin/dish")
    @ApiOperation("新增菜品")
    public Result save(@RequestBody DishDTO dishDTO) {
        log.info("新增菜品 {}", dishDTO);
        dishService.saveWithFlavor(dishDTO);
        //标记缓存脏位
        String key = "dish_" + dishDTO.getCategoryId();
        log.info("key: {} 标记缓存脏位", key);
        redisTemplate.opsForHash().put("dirtyByte", key, "1");//标记为缓存脏位

        return Result.success();
    }

    /**
     * 菜品分页查询
     * @param dishPageQueryDTO
     * @return
     */
    @GetMapping("/admin/dish/page")
    @ApiOperation("菜品分页查询")
    public Result<PageResult> page(DishPageQueryDTO dishPageQueryDTO) {
        log.info("菜品分页查询: {}", dishPageQueryDTO);

        PageResult pageResult =  dishService.pageQuery(dishPageQueryDTO);
        return Result.success(pageResult);
    }

    /**
     * 批量删除菜品
     * @param ids
     * @return
     */
    @DeleteMapping("/admin/dish")
    @ApiOperation("菜品批量删除")
    public Result delete(@RequestParam List<Long> ids) {
        log.info("菜品批量删除: {}", ids);
        dishService.deleteBatch(ids);//函数内部标记缓存脏位
        return Result.success();
    }

    /**
     * 根据Id获取菜品信息
     * @param id
     * @return
     */
    @GetMapping("admin/dish/{id}")
    @ApiOperation("根据Id获取菜品信息")
    public Result<DishVO> getById(@PathVariable Long id) {
        log.info("根据id查询菜品: {}", id);
        DishVO dishVO = dishService.getByIdWithFlavor(id);
        return Result.success(dishVO);
    }

    /**
     * 修改菜品
     * @param dishDTO
     * @return
     */
    @PutMapping("admin/dish")
    @ApiOperation("修改菜品")
    @CacheEvict(cacheNames="setmealCache" , allEntries = true)
    public Result update(@RequestBody DishDTO dishDTO) {
        log.info("修改菜品: {}", dishDTO);
        dishService.updateWithFlavor(dishDTO);//函数内部标记缓存脏位
        return Result.success();
    }

    /**
     * 启用禁用菜品
     * @param status
     * @param id
     * @return
     */
    @PostMapping("/admin/dish/status/{status}")
    @ApiOperation("启用禁用菜品")
    public Result startOrStop(@PathVariable Integer status, Long id) {
        log.info("启用禁用菜品: {}, {}", status, id);
        dishService.startOrStop(status, id);//函数内部标记缓存脏位
        return Result.success();
    }

    /**
     * 根据分类Id查询菜品
     * @param categoryId
     * @return
     */
    @GetMapping("/admin/dish/list")
    @ApiOperation("根据分类Id查询菜品")
    public Result<List<Dish>> list(Long categoryId) {
        log.info("根据分类Id查询菜品: {}", categoryId);
        List<Dish> dishes = dishService.list(categoryId);
        return Result.success(dishes);
    }
}
