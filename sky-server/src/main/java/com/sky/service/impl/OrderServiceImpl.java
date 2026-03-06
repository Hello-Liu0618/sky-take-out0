package com.sky.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.constant.StatusConstant;
import com.sky.context.BaseContext;
import com.sky.dto.*;
import com.sky.entity.*;
import com.sky.exception.AddressBookBusinessException;
import com.sky.exception.OrderBusinessException;
import com.sky.exception.ShoppingCartBusinessException;
import com.sky.mapper.*;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.OrderService;
import com.sky.vo.OrderPaymentVO;
import com.sky.vo.OrderStatisticsVO;
import com.sky.vo.OrderSubmitVO;
import com.sky.vo.OrderVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class OrderServiceImpl implements OrderService {

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private OrderDetailMapper orderDetailMapper;

    @Autowired
    private AddressBookMapper addressBookMapper;

    @Autowired
    private ShoppingCartMapper shoppingCartMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private SetmealMapper setmealMapper;

    @Autowired
    private DishMapper dishMapper;

    public OrderSubmitVO submitOrder(OrdersSubmitDTO orderSubmitDTO) {

        //处理业务异常(地址簿为空, 购物车为空)
        AddressBook addressBook = addressBookMapper.getById(orderSubmitDTO.getAddressBookId());
        if ( addressBook == null ) {
            throw new AddressBookBusinessException(MessageConstant.ADDRESS_BOOK_IS_NULL);
        }

        Long userId = BaseContext.getCurrentId();

        ShoppingCart shoppingCart = new ShoppingCart();
        shoppingCart.setUserId(userId);
        List<ShoppingCart> list = shoppingCartMapper.list(shoppingCart);
        if ( list == null || list.isEmpty() ) {
            throw new ShoppingCartBusinessException(MessageConstant.SHOPPING_CART_IS_NULL);
        }

        //向订单表插入一条数据
        Orders orders = new Orders();
        BeanUtils.copyProperties(orderSubmitDTO, orders);
        orders.setOrderTime(LocalDateTime.now());
        orders.setPayStatus(Orders.UN_PAID);
        orders.setStatus(Orders.PENDING_PAYMENT);
        orders.setNumber(String.valueOf(System.currentTimeMillis()));
        orders.setPhone(addressBook.getPhone());
        orders.setConsignee(addressBook.getConsignee());
        orders.setUserId(userId);

        orderMapper.insert(orders);


        //向订单细节表插入一条或多条数据

        List<OrderDetail> orderDetailList = new ArrayList<>();
        list.forEach(item -> {
            OrderDetail orderDetail = new OrderDetail();//订单明细
            BeanUtils.copyProperties(item, orderDetail);
            orderDetail.setOrderId(orders.getId());//设置订单明细对象关联的订单对象Id
            orderDetailList.add(orderDetail);
        });
        orderDetailMapper.insertBatch(orderDetailList);

        //清空购物车数据
        shoppingCartMapper.cleanByUserId(userId);

        //封装VO
        OrderSubmitVO orderSubmitVO = new OrderSubmitVO();
        orderSubmitVO.setId(orders.getId());
        orderSubmitVO.setOrderNumber(orders.getNumber());
        orderSubmitVO.setOrderTime(orders.getOrderTime());
        orderSubmitVO.setOrderAmount(orders.getAmount());
        return orderSubmitVO;
    }

    /**
     * 订单支付
     *
     * @param ordersPaymentDTO
     * @return
     */
    public OrderPaymentVO payment(OrdersPaymentDTO ordersPaymentDTO) throws Exception {
        // 当前登录用户id
        Long userId = BaseContext.getCurrentId();
        User user = userMapper.getById(userId);

        //调用微信支付接口，生成预支付交易单
        /*
        JSONObject jsonObject = weChatPayUtil.pay(
                ordersPaymentDTO.getOrderNumber(), //商户订单号
                new BigDecimal(0.01), //支付金额，单位 元
                "苍穹外卖订单", //商品描述
                user.getOpenid() //微信用户的openid
        );
         */
        JSONObject jsonObject = new JSONObject();
        if (jsonObject.getString("code") != null && jsonObject.getString("code").equals("ORDERPAID")) {
            throw new OrderBusinessException("该订单已支付");
        }

        OrderPaymentVO vo = jsonObject.toJavaObject(OrderPaymentVO.class);
        vo.setPackageStr(jsonObject.getString("package"));

        return vo;
    }

    /**
     * 支付成功，修改订单状态
     *
     * @param outTradeNo
     */
    public void paySuccess(String outTradeNo) {

        // 根据订单号查询订单
        Orders ordersDB = orderMapper.getByNumber(outTradeNo);

        // 根据订单id更新订单的状态、支付方式、支付状态、结账时间
        Orders orders = Orders.builder()
                .id(ordersDB.getId())
                .status(Orders.TO_BE_CONFIRMED)
                .payStatus(Orders.PAID)
                .checkoutTime(LocalDateTime.now())
                .build();

        orderMapper.update(orders);
    }

    /**
     * 查询历史订单
     * @param pageNum
     * @param pageSize
     * @param status
     * @return
     */
    public PageResult pageQueryByUser(int pageNum, int pageSize, Integer status) {
        //进行分页
        PageHelper.startPage(pageNum, pageSize);

        //构造ordersPageQuertDTO来传递参数
        OrdersPageQueryDTO ordersPageQueryDTO = new OrdersPageQueryDTO();
        ordersPageQueryDTO.setStatus(status);
        ordersPageQueryDTO.setUserId(BaseContext.getCurrentId());

        //查询得到Page<Orders>类型的页
        Page<Orders> page = orderMapper.pageQuery(ordersPageQueryDTO);

        //此处需要将Page<Orders>类型转换为Page<OrderVO>类型
        List<OrderVO> orderVOList = new ArrayList<>();
        if ( page != null && !page.isEmpty() ) {
            page.getResult().forEach(item -> {
                OrderVO orderVO = new OrderVO();
                BeanUtils.copyProperties(item, orderVO);
                //查询得到orderDetail数据
                Long orderId = item.getId();
                List<OrderDetail> orderDetailList = orderDetailMapper.getByOrderId(orderId);
                orderVO.setOrderDetailList(orderDetailList);

                orderVOList.add(orderVO);
            });
        }
        return new PageResult(page.getTotal(), orderVOList);
    }

    /**
     * 查询订单详情
     * @param orderId
     * @return
     */
    public OrderVO details(Long orderId) {
        OrderVO orderVO = new OrderVO();
        //获取订单
        Orders order = orderMapper.getById(orderId);
        BeanUtils.copyProperties(order, orderVO);

        //获取订单详情
        List<OrderDetail> orderDetailList = orderDetailMapper.getByOrderId(orderId);
        orderVO.setOrderDetailList(orderDetailList);
        List<String> orderDishList = new ArrayList<>();
        orderDetailList.forEach(item -> {
            String dish = item.getName() + "*" + item.getNumber();
            orderDishList.add(dish);
        });
        String orderDishes = String.join(", ", orderDishList);
        orderVO.setOrderDishes(orderDishes);

        AddressBook addressBook = addressBookMapper.getById(order.getAddressBookId());
        String address = addressBook.getProvinceName()+addressBook.getCityName()+addressBook.getDistrictName()+addressBook.getDetail();
        orderVO.setAddress(address);

        return orderVO;
    }

    /**
     * 取消订单
     * @param id
     */
    public void cancel(Long id){
        Orders ordersDB = orderMapper.getById(id);
        if (ordersDB == null) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }

        Integer status = ordersDB.getStatus();
        if (status > 2) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        Orders orders = new Orders();
        orders.setId(ordersDB.getId());
        if (ordersDB.getStatus().equals(Orders.TO_BE_CONFIRMED)) {
            //微信退款，此处不处理
            orders.setPayStatus(Orders.REFUND);
        }
        orders.setStatus(Orders.CANCELLED);
        orders.setCancelReason("用户取消");
        orders.setOrderTime(LocalDateTime.now());
        orders.setCancelTime(LocalDateTime.now());
        orderMapper.update(orders);
    }

    /**
     * 再来一单
     * @param id
     */
    public void repetition(Long id){//此方法将指定订单中的商品加入到购物车中
        //查找得到订单信息
        Long userId = BaseContext.getCurrentId();
        List<OrderDetail> orderDetailList = orderDetailMapper.getByOrderId(id);
        if (orderDetailList == null || orderDetailList.isEmpty()) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }

        List<ShoppingCart> shoppingCartList = new ArrayList<>();
        orderDetailList.forEach(item -> {
            //检查是否能够加入购物车
            Long dishId = item.getDishId();
            if (dishId == null) {
                Long setmealId = item.getSetmealId();
                if ( setmealMapper.getById(setmealId).getStatus() == StatusConstant.DISABLE ) {
                    throw new ShoppingCartBusinessException(MessageConstant.SHOPPING_CART_ADD_FAILED);
                }
            } else {
                if (dishMapper.getById(dishId).getStatus() == StatusConstant.DISABLE) {
                    throw new ShoppingCartBusinessException(MessageConstant.SHOPPING_CART_ADD_FAILED);
                }
            }

            ShoppingCart shoppingCart = new ShoppingCart();
            BeanUtils.copyProperties(item, shoppingCart);

            shoppingCart.setUserId(userId);
            shoppingCart.setCreateTime(LocalDateTime.now());

            shoppingCartList.add(shoppingCart);
        });
        //插入之前先清空购物车
        shoppingCartMapper.cleanByUserId(userId);

        shoppingCartMapper.insertBatch(shoppingCartList);
    }

    /**
     * 订单搜索
     * @param ordersPageQueryDTO
     * @return
     */
    public PageResult conditionSearch(OrdersPageQueryDTO ordersPageQueryDTO){
        PageHelper.startPage(ordersPageQueryDTO.getPage(), ordersPageQueryDTO.getPageSize());
        Page<Orders> page = orderMapper.pageQuery(ordersPageQueryDTO);
        List<OrderVO>  orderVOList = new ArrayList<>();
        page.forEach(item -> {
            OrderVO orderVO = new OrderVO();
            BeanUtils.copyProperties(item, orderVO);

            //查询得到orderDetail数据
            Long orderId = item.getId();
            List<OrderDetail> orderDetailList = orderDetailMapper.getByOrderId(orderId);
            orderVO.setOrderDetailList(orderDetailList);

            //拼接得到订单菜品字符串
            List<String> orderDishList = new ArrayList<>();
            orderDetailList.forEach(item1 -> {
                String str = item1.getName() + "*" + item1.getNumber();
                orderDishList.add(str);
            });
            orderVO.setOrderDishes(String.join(", ", orderDishList));

            //拼接得到地址字符串
            AddressBook addressBook = addressBookMapper.getById(item.getAddressBookId());
            String address = addressBook.getProvinceName()+addressBook.getCityName()+addressBook.getDistrictName()+addressBook.getDetail();
            orderVO.setAddress(address);

            orderVOList.add(orderVO);
        });
        return new PageResult(page.getTotal(), orderVOList);
    }

    /**
     * 各个状态的订单数量统计
     * @return
     */
    public OrderStatisticsVO statistics() {
        OrderStatisticsVO orderStatisticsVO = new OrderStatisticsVO();

        Integer confirmed = (orderMapper.getByStatus(Orders.CONFIRMED)).size();
        orderStatisticsVO.setConfirmed(confirmed);

        Integer toBeConfirmed = (orderMapper.getByStatus(Orders.TO_BE_CONFIRMED)).size();
        orderStatisticsVO.setToBeConfirmed(toBeConfirmed);

        Integer deliveryInProgress = (orderMapper.getByStatus(Orders.DELIVERY_IN_PROGRESS)).size();
        orderStatisticsVO.setDeliveryInProgress(deliveryInProgress);
        return orderStatisticsVO;
    }

    /**
     * 接单
     * @param ordersConfirmDTO
     */
    public void confirm(OrdersConfirmDTO ordersConfirmDTO) {
        Long orderId = ordersConfirmDTO.getId();
        Orders orders = Orders.builder().id(orderId).status(Orders.CONFIRMED).build();
        orderMapper.update(orders);
    }

    /**
     * 拒单
     * @param ordersRejectionDTO
     */
    public void rejection(OrdersRejectionDTO ordersRejectionDTO) {
        Long  orderId = ordersRejectionDTO.getId();
        String rejectionReason = ordersRejectionDTO.getRejectionReason();
        Orders orders = orderMapper.getById(orderId);
        if ( orders.getStatus() != Orders.TO_BE_CONFIRMED ) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
        if ( orders.getPayStatus() == Orders.PAID ) {
            //微信退款, 此处不处理
        }
        Orders newOrders = Orders.builder().id(orderId).status(Orders.CANCELLED).rejectionReason(rejectionReason).cancelReason("商家拒单: " + rejectionReason).cancelTime(LocalDateTime.now()).build();
        orderMapper.update(newOrders);
    }
}
