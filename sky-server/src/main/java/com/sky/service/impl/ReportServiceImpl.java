package com.sky.service.impl;

import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import com.sky.service.ReportService;
import com.sky.vo.TurnoverReportVO;
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
}
