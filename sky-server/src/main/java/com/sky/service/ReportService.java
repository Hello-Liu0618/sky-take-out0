package com.sky.service;

import com.sky.vo.TurnoverReportVO;

import java.time.LocalDate;

public interface ReportService {

    /**
     * 统计指定日期区间内的营业额
     * @param begin //"yyyy-MM-dd"
     * @param end //"yyyy-MM-dd"
     * @return
     */
    TurnoverReportVO getTurnoverStatistics(LocalDate begin, LocalDate end);
}
