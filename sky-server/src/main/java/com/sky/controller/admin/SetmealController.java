package com.sky.controller.admin;


import com.sky.dto.SetmealDTO;
import com.sky.dto.SetmealPageQueryDTO;
import com.sky.entity.Setmeal;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.SetmealService;
import com.sky.vo.SetmealVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 套餐管理
 */
@RequestMapping("/admin/setmeal")
@RestController
@Api(tags="套餐相关接口")
@Slf4j
public class SetmealController {

    @Autowired
    private SetmealService setmealService;
    /**
     * 新增套餐
     * @param setmealDTO
     * @return
     */
    @PostMapping
    @ApiOperation("新增套餐")
    @CacheEvict(cacheNames="setmealCache", key="#setmealDTO.categoryId")
    public Result save(@RequestBody SetmealDTO setmealDTO){
        log.info("新增套餐: {}", setmealDTO);
        setmealService.saveWithDish(setmealDTO);
        return Result.success();
    }

    /**
     * 套餐分页查询
     * @param setmealPageQueryDTO
     * @return
     */
    @GetMapping("/page")
    @ApiOperation("套餐分页查询")
    public Result<PageResult> page(SetmealPageQueryDTO setmealPageQueryDTO){
        log.info("套餐分页查询: {}", setmealPageQueryDTO);

        PageResult pageResult = setmealService.pageQuery(setmealPageQueryDTO);
        return Result.success(pageResult);
    }

    /**
     * 批量删除套餐
     * @param ids
     */
    @DeleteMapping
    @ApiOperation("批量删除菜品")
    @CacheEvict(cacheNames="setmealCache", allEntries = true)
    public Result deleteBatch(@RequestParam List<Long> ids){
        log.info("批量删除菜品: {}", ids);

        setmealService.deleteBatch(ids);
        return Result.success();
    }

    /**
     * 根据Id获取套餐信息并带有菜品数据
     * @return
     */
    @GetMapping("/{id}")
    @ApiOperation("根据Id获取套餐信息")
    public Result<SetmealVO> getById(@PathVariable Long id){
        log.info("根据id查询套餐: {}", id);
        SetmealVO setmealVO = setmealService.getByIdWithDish(id);
        return Result.success(setmealVO);
    }

    /**
     * 修改套餐并带有菜品信息
     * @param setmealDTO
     * @return
     */
    @PutMapping
    @ApiOperation("修改套餐")
    @CacheEvict(cacheNames="setmealCache", allEntries = true)
    public Result update(@RequestBody SetmealDTO setmealDTO){
        log.info("修改套餐: {}", setmealDTO);
        setmealService.updateWithDish(setmealDTO);
        return Result.success();
    }

    /**
     * 启用禁用套餐
     * @param status
     * @param id
     * @return
     */
    @PostMapping("/status/{status}")
    @ApiOperation("启用禁用套餐")
    @CacheEvict(cacheNames="setmealCache", allEntries = true)
    public Result startOrStop(@PathVariable Integer status, Long id){
        log.info("启用禁用套餐: {}, {}", status, id);
        setmealService.startOrStop(status, id);
        return Result.success();
    }
}
