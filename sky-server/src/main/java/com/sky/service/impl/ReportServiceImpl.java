package com.sky.service.impl;

import com.sky.dto.GoodsSalesDTO;
import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import com.sky.mapper.UserMapper;
import com.sky.service.ReportService;
import com.sky.service.WorkspaceService;
import com.sky.vo.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
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

    @Autowired
    private WorkspaceService workspaceService;

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
        List<Integer> totalUserList =  new ArrayList<>();
        List<Integer> newUserList =  new ArrayList<>();
        dates.forEach(date1 -> {
            //根据date日期查询对应的用户数据
            LocalDateTime dateTimeMin = LocalDateTime.of(date1, LocalTime.MIN);
            LocalDateTime dateTimeMax = LocalDateTime.of(date1, LocalTime.MAX);
            Map map = new HashMap();
            map.put("end", dateTimeMax);
            Integer userTotal = userMapper.countByMap(map);
            if( null == userTotal){
                userTotal = 0;
            }
            totalUserList.add(userTotal);
            map.put("begin", dateTimeMin);
            Integer userNew = userMapper.countByMap(map);
            if( null == userNew){
                userNew = 0;
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
        List<Integer> orderCountList =  new ArrayList<>();
        List<Integer> validOrderCountList =  new ArrayList<>();
        dates.forEach(date1 -> {
            LocalDateTime dateTimeMin = LocalDateTime.of(date1, LocalTime.MIN);
            LocalDateTime dateTimeMax = LocalDateTime.of(date1, LocalTime.MAX);
            Map mapTotal =  new HashMap();
            Map mapValid =  new HashMap();
            mapTotal.put("begin", dateTimeMin);
            mapTotal.put("end", dateTimeMax);
            Integer totalOrder = orderMapper.countByMap(mapTotal);
            if( null == totalOrder){
                totalOrder = 0;
            }
            orderCountList.add(totalOrder);

            mapValid.put("begin", dateTimeMin);
            mapValid.put("end", dateTimeMax);
            mapValid.put("status", Orders.COMPLETED);
            Integer validOrder = orderMapper.countByMap(mapValid);
            if( null == validOrder){
                validOrder = 0;
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

    /**
     * 统计指定日期区间内销量前十
     * @param begin
     * @param end
     * @return
     */
    public SalesTop10ReportVO getTop10(LocalDate begin, LocalDate end) {
        List<GoodsSalesDTO> goodsSalesDTOList =  orderMapper.getSalesTop10(begin, end);
        List<String> nameList = new ArrayList<>();
        List<Integer> numberList = new ArrayList<>();
        goodsSalesDTOList.forEach(goodsSalesDTO -> {
            nameList.add(goodsSalesDTO.getName());
            numberList.add(goodsSalesDTO.getNumber());
        });
        String nameListString = StringUtils.join(nameList, ",");
        String numberListString = StringUtils.join(numberList, ",");
        SalesTop10ReportVO salesTop10ReportVO = SalesTop10ReportVO.builder()
                .nameList(nameListString)
                .numberList(numberListString)
                .build();
        return salesTop10ReportVO;
    }

    /**
     * 导出运营数据报表
     * @param response
     */
    public void export(HttpServletResponse response) {
        //1. 查询数据库，获取营业数据---查询最近30天的运营数据
        LocalDate dateBegin = LocalDate.now().minusDays(30);
        LocalDate dateEnd = LocalDate.now().minusDays(1);

        //查询概览数据
        BusinessDataVO businessDataVO = workspaceService.getBusinessData(LocalDateTime.of(dateBegin, LocalTime.MIN), LocalDateTime.of(dateEnd, LocalTime.MAX));

        //2. 通过POI将数据写入到Excel文件中
        InputStream in = this.getClass().getClassLoader().getResourceAsStream("template/运营数据报表模板.xlsx");

        try {
            //基于模板文件创建一个新的Excel文件
            XSSFWorkbook excel = new XSSFWorkbook(in);

            //获取表格文件的Sheet页
            XSSFSheet sheet = excel.getSheet("Sheet1");

            //填充数据--时间
            sheet.getRow(1).getCell(1).setCellValue("时间：" + dateBegin + "至" + dateEnd);

            //获得第4行
            XSSFRow row = sheet.getRow(3);
            row.getCell(2).setCellValue(businessDataVO.getTurnover());
            row.getCell(4).setCellValue(businessDataVO.getOrderCompletionRate());
            row.getCell(6).setCellValue(businessDataVO.getNewUsers());

            //获得第5行
            row = sheet.getRow(4);
            row.getCell(2).setCellValue(businessDataVO.getValidOrderCount());
            row.getCell(4).setCellValue(businessDataVO.getUnitPrice());

            //填充明细数据
            for (int i = 0; i < 30; i++) {
                LocalDate date = dateBegin.plusDays(i);
                //查询某一天的营业数据
                BusinessDataVO businessData = workspaceService.getBusinessData(LocalDateTime.of(date, LocalTime.MIN), LocalDateTime.of(date, LocalTime.MAX));

                //获得某一行
                row = sheet.getRow(7 + i);
                row.getCell(1).setCellValue(date.toString());
                row.getCell(2).setCellValue(businessData.getTurnover());
                row.getCell(3).setCellValue(businessData.getValidOrderCount());
                row.getCell(4).setCellValue(businessData.getOrderCompletionRate());
                row.getCell(5).setCellValue(businessData.getUnitPrice());
                row.getCell(6).setCellValue(businessData.getNewUsers());
            }

            //3. 通过输出流将Excel文件下载到客户端浏览器
            ServletOutputStream out = response.getOutputStream();
            excel.write(out);

            //关闭资源
            out.close();
            excel.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
