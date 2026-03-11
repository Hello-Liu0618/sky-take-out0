package com.sky.mapper;

import com.github.pagehelper.Page;
import com.sky.dto.GoodsSalesDTO;
import com.sky.dto.OrdersPageQueryDTO;
import com.sky.entity.Orders;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface OrderMapper {

    /**
     * 插入订单数据
     * @param orders
     */
    void insert(Orders orders);


    /**
     * 根据订单号查询订单
     * @param orderNumber
     */
    @Select("select * from orders where number = #{orderNumber}")
    Orders getByNumber(String orderNumber);

    /**
     * 修改订单信息
     * @param orders
     */
    void update(Orders orders);

    /**
     * 分页查询订单
     * @param ordersPageQueryDTO
     * @return
     */
    Page<Orders> pageQuery(OrdersPageQueryDTO ordersPageQueryDTO);

    /**
     * 根据id查询数据
     * @param id
     * @return
     */
    @Select("select * from orders where id=#{id}")
    Orders getById(Long id);

    /**
     * 根据订单状态查询订单
     * @param status
     * @return
     */
    @Select("select * from orders where status = #{status}")
    List<Orders> getByStatus(Integer status);

    /**
     * 根据订单状态和订单时间上限查找得到订单
     * @param localDateTime
     * @param status
     * @return
     */
    @Select("select * from orders where status=#{status} and order_time < #{localDateTime}")
    List<Orders> getByStatusAndOrderTimeLT(LocalDateTime localDateTime, Integer status);

    /**
     * 统计订单总金额
     * @param map
     * @return
     */
    Double sumByMap(Map map);

    /**
     * 根据Map统计订单数
     * @param map
     * @return
     */
    Long countByMap(Map map);

    /**
     * 统计订单总数
     * @return
     */
    @Select("select count(id) from orders")
    Integer countTotal();

    /**
     * 统计有效订单数
     * @return
     */
    @Select("select count(id) from orders where status=5")
    Integer countValid();

    List<GoodsSalesDTO> getSalesTop10(LocalDate begin, LocalDate end);
}
