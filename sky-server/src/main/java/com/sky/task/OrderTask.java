package com.sky.task;

import com.sky.constant.MessageConstant;
import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

/**
 * 定时任务类, 定时处理订单状态
 */
@Component
@Slf4j
public class OrderTask {

    @Autowired
    private OrderMapper orderMapper;
    /**
     * 处理超时订单
     */
    @Scheduled(cron ="0 * * * * ?")//每分钟触发一次
    public void processTimeoutOrders() {
        log.info("定时处理超时订单: {}", new Date());

        List<Orders> timeoutOrders = orderMapper.getByStatusAndOrderTimeLT(LocalDateTime.now().minusMinutes(15L), Orders.PENDING_PAYMENT);

        timeoutOrders.forEach(order -> {
            Orders newOrder = Orders
                    .builder()
                    .id(order.getId())
                    .status(Orders.CANCELLED)
                    .payStatus(Orders.UN_PAID)
                    .cancelTime(LocalDateTime.now())
                    .cancelReason(MessageConstant.ORDER_TIME_OUT)
                    .build();
            orderMapper.update(newOrder);
        });
    }

    /**
     * 处理未被商家及时处理的一直处于配送中状态的订单
     */
    @Scheduled(cron = "0 0 1 * * ?")//每天凌晨1:00触发一次
    public void processDeliveryOrders() {
        log.info("定时处理派送中的订单: {}", new Date());

        List<Orders> deliveryOrders = orderMapper.getByStatusAndOrderTimeLT(LocalDateTime.now().minusHours(1L),Orders.DELIVERY_IN_PROGRESS);
        deliveryOrders.forEach(order -> {
            Orders newOrder = Orders
                    .builder()
                    .id(order.getId())
                    .status(Orders.COMPLETED)
                    .build();
            orderMapper.update(newOrder);
        });
    }

}
