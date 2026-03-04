package com.sky.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.context.BaseContext;
import com.sky.dto.OrdersPageQueryDTO;
import com.sky.dto.OrdersPaymentDTO;
import com.sky.dto.OrdersSubmitDTO;
import com.sky.entity.*;
import com.sky.exception.AddressBookBusinessException;
import com.sky.exception.OrderBusinessException;
import com.sky.exception.ShoppingCartBusinessException;
import com.sky.mapper.*;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.OrderService;
import com.sky.vo.OrderPaymentVO;
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
}
