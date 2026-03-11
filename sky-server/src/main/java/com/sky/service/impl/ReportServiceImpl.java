package com.sky.service.impl;

import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import com.sky.mapper.UserMapper;
import com.sky.service.ReportService;
import com.sky.vo.OrderReportVO;
import com.sky.vo.TurnoverReportVO;
import com.sky.vo.UserReportVO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ReportServiceImpl implements ReportService {

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private UserMapper userMapper;

    public ReportServiceImpl(OrderMapper orderMapper) {
        this.orderMapper = orderMapper;
    }

    /**
     * 统计指定日期区间内的营业额
     * @param begin //"yyyy-MM-dd"
     * @param end //"yyyy-MM-dd"
     * @return
     */
    public TurnoverReportVO getTurnoverStatistics(LocalDate begin, LocalDate end) {
        //当前集合存储从begin到end的所有日期
        List<LocalDate> dates = new ArrayList<>();
        LocalDate date = begin;
        while (!date.isAfter(end)) {
            dates.add(date);
            date = date.plusDays(1);
        }
        List<Double> turnovers =  new ArrayList<>();
        dates.forEach(date1 -> {
            //根据date日期查询对应的营业额数据, 营业额是指订单状态为已完成的订单的金额总和
            LocalDateTime dateTimeMin = LocalDateTime.of(date1, LocalTime.MIN);
            LocalDateTime dateTimeMax = LocalDateTime.of(date1, LocalTime.MAX);
            Map map = new HashMap();
            map.put("begin", dateTimeMin);
            map.put("end", dateTimeMax);
            map.put("status", Orders.COMPLETED);
            Double turnOver = orderMapper.sumByMap(map);
            if( null == turnOver){
                turnOver = 0.0;
            }
            turnovers.add(turnOver);
        });

        String dateList = StringUtils.join(dates, ",");
        String turnoverList = StringUtils.join(turnovers, ",");

        TurnoverReportVO turnoverReportVO = TurnoverReportVO.builder()
                .turnoverList(turnoverList)
                .dateList(dateList)
                .build();

        return turnoverReportVO;
    }

    /**
     * 统计指定日期区间内的用户数据
     * @param begin
     * @param end
     * @return
     */
    public UserReportVO getUserStatistics(LocalDate begin, LocalDate end) {
        //当前集合存储从begin到end的所有日期
        List<LocalDate> dates = new ArrayList<>();
        LocalDate date = begin;
        while (!date.isAfter(end)) {
            dates.add(date);
            date = date.plusDays(1);
        }
        List<Long> totalUserList =  new ArrayList<>();
        List<Long> newUserList =  new ArrayList<>();
        dates.forEach(date1 -> {
            //根据date日期查询对应的用户数据
            LocalDateTime dateTimeMin = LocalDateTime.of(date1, LocalTime.MIN);
            LocalDateTime dateTimeMax = LocalDateTime.of(date1, LocalTime.MAX);
            Map map = new HashMap();
            map.put("end", dateTimeMax);
            Long userTotal = userMapper.sumByMap(map);
            if( null == userTotal){
                userTotal = 0L;
            }
            totalUserList.add(userTotal);
            map.put("begin", dateTimeMin);
            Long userNew = userMapper.sumByMap(map);
            if( null == userNew){
                userNew = 0L;
            }
            newUserList.add(userNew);
        });

        String dateList = StringUtils.join(dates, ",");
        String totalUserListString = StringUtils.join(totalUserList, ",");
        String newUserListString = StringUtils.join(newUserList, ",");

        UserReportVO userReportVO = UserReportVO.builder()
                .dateList(dateList)
                .totalUserList(totalUserListString)
                .newUserList(newUserListString)
                .build();
        return userReportVO;
    }

    /**
     * 统计指定日期区间内的订单数据
     * @param begin
     * @param end
     * @return
     */
    public OrderReportVO getOrdersStatistics(LocalDate begin, LocalDate end) {
        List<LocalDate> dates = new ArrayList<>();
        LocalDate date = begin;
        while (!date.isAfter(end)) {
            dates.add(date);
            date = date.plusDays(1);
        }
        List<Long> orderCountList =  new ArrayList<>();
        List<Long> validOrderCountList =  new ArrayList<>();
        dates.forEach(date1 -> {
            LocalDateTime dateTimeMin = LocalDateTime.of(date1, LocalTime.MIN);
            LocalDateTime dateTimeMax = LocalDateTime.of(date1, LocalTime.MAX);
            Map mapTotal =  new HashMap();
            Map mapValid =  new HashMap();
            mapTotal.put("begin", dateTimeMin);
            mapTotal.put("end", dateTimeMax);
            Long totalOrder = orderMapper.countByMap(mapTotal);
            if( null == totalOrder){
                totalOrder = 0L;
            }
            orderCountList.add(totalOrder);

            mapValid.put("begin", dateTimeMin);
            mapValid.put("end", dateTimeMax);
            mapValid.put("status", Orders.COMPLETED);
            Long validOrder = orderMapper.countByMap(mapValid);
            if( null == validOrder){
                validOrder = 0L;
            }
            validOrderCountList.add(validOrder);
        });

        Integer totalOrderNum = orderMapper.countTotal();
        if( null == totalOrderNum){
            totalOrderNum = 0;
        }

        Integer validOrderNum = orderMapper.countValid();
        if( null == validOrderNum){
            validOrderNum = 0;
        }

        Double orderCompletionRate = Double.longBitsToDouble(validOrderNum) / Double.longBitsToDouble(totalOrderNum);

        String dateListString = StringUtils.join(dates, ",");
        String validOrderCounteListString = StringUtils.join(validOrderCountList, ",");
        String orderCountListString = StringUtils.join(orderCountList, ",");
        OrderReportVO orderReportVO = OrderReportVO.builder()
                .dateList(dateListString)
                .orderCountList(orderCountListString)
                .validOrderCountList(validOrderCounteListString)
                .orderCompletionRate(orderCompletionRate)
                .totalOrderCount(totalOrderNum)
                .validOrderCount(validOrderNum)
                .build();
        return orderReportVO;
    }
}
