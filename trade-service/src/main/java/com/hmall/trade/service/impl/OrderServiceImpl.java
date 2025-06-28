package com.hmall.trade.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmall.api.client.CartClient;
import com.hmall.api.client.ItemClient;
import com.hmall.common.constants.MqConstants;
import com.hmall.common.domain.MultiDelayMessage;
import com.hmall.common.exception.BadRequestException;
import com.hmall.common.exception.BizIllegalException;
import com.hmall.common.utils.BeanUtils;
import com.hmall.common.utils.UserContext;
import com.hmall.api.dto.ItemDTO;
import com.hmall.api.dto.OrderDetailDTO;
import com.hmall.trade.constants.TradeMqConstants;
import com.hmall.trade.domain.dto.OrderFormDTO;
import com.hmall.trade.domain.po.Order;
import com.hmall.trade.domain.po.OrderDetail;
import com.hmall.trade.mapper.OrderMapper;
//import com.hmall.trade.service.ICartService;
//import com.hmall.trade.service.IItemService;
import com.hmall.trade.service.IOrderDetailService;
import com.hmall.trade.service.IOrderService;
import io.seata.spring.annotation.GlobalTransactional;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author itheima
 * @since 2023-05-05
 */
@Service
@RequiredArgsConstructor
public class OrderServiceImpl extends ServiceImpl<OrderMapper, Order> implements IOrderService {

    private final ItemClient itemClient;
    private final CartClient cartClient;
//    private final IItemService itemService;
    private final IOrderDetailService detailService;
//    private final ICartService cartService;

    private final RabbitTemplate rabbitTemplate;

    @Override
//    @Transactional
    @GlobalTransactional // 使用分布式事务控制
    public Long createOrder(OrderFormDTO orderFormDTO) {
        // 1.订单数据
        Order order = new Order();
        // 1.1.查询商品
        List<OrderDetailDTO> detailDTOS = orderFormDTO.getDetails();
        // 1.2.获取商品id和数量的Map
        Map<Long, Integer> itemNumMap = detailDTOS.stream()
                .collect(Collectors.toMap(OrderDetailDTO::getItemId, OrderDetailDTO::getNum));
        Set<Long> itemIds = itemNumMap.keySet();
        // 1.3.查询商品
        List<ItemDTO> items = itemClient.queryItemByIds(itemIds);
        if (items == null || items.size() < itemIds.size()) {
            throw new BadRequestException("商品不存在");
        }
        // 1.4.基于商品价格、购买数量计算商品总价：totalFee
        int total = 0;
        for (ItemDTO item : items) {
            total += item.getPrice() * itemNumMap.get(item.getId());
        }
        order.setTotalFee(total);
        // 1.5.其它属性
        order.setPaymentType(orderFormDTO.getPaymentType());
        order.setUserId(UserContext.getUser());
        order.setStatus(1);
        // 1.6.将Order写入数据库order表中
        save(order);

        // 2.保存订单详情
        List<OrderDetail> details = buildDetails(order.getId(), items, itemNumMap);
        detailService.saveBatch(details);

        // 3.清理购物车商品
//        cartClient.deleteCartItemByIds(itemIds);
        rabbitTemplate.convertAndSend(MqConstants.TRADE_EXCHANGE_NAME,
                MqConstants.ROUTING_KEY_ORDER_CREATE, itemIds, new MessagePostProcessor() {
                    //在发送消息之前最后队消息进行处理
                    @Override
                    public Message postProcessMessage(Message message) throws AmqpException {
                        //将当前登录用户的id设置到消息头部中
                        message.getMessageProperties().setHeader("user-info", UserContext.getUser());
                        return message;
                    }
                });

        // 4.扣减库存
        try {
            itemClient.deductStock(detailDTOS);
        } catch (Exception e) {
            throw new RuntimeException("库存不足！");
        }

//        int i = 1/0;

        // 发送延迟消息,确认订单支付状态来修改订单状态
        //共延迟12s，分别延迟2s,4s,7s,12s后查询订单状态
        try {
            MultiDelayMessage<Long> msg = MultiDelayMessage.of(order.getId(), 2000,3000,5000);
            rabbitTemplate.convertAndSend(TradeMqConstants.DELAY_EXCHANGE, TradeMqConstants.DELAY_ORDER_ROUTING_KEY, msg, new MessagePostProcessor() {
                @Override
                public Message postProcessMessage(Message message) throws AmqpException {
                    message.getMessageProperties().setDelay(2000);
                    return message;
                }
            });
        }catch (AmqpException e){
            System.out.println("发送查询订单状态的延迟消息失败");
        }

        return order.getId();
    }

    @Override
    public void markOrderPaySuccess(Long orderId) {
//        //查询订单
//        Order oldOrder = getById(orderId);
//        //如果订单为空或不是未支付状态，则不更新
//        if (oldOrder == null || !oldOrder.getStatus().equals(1)){
//            return;
//        }
//        Order order = new Order();
//        order.setId(orderId);
//        order.setStatus(2);
//        order.setPayTime(LocalDateTime.now());
//        updateById(order);


        lambdaUpdate().set(Order::getStatus, 2)
                .set(Order::getPayTime, LocalDateTime.now())
                .eq(Order::getStatus, 1)
                .eq(Order::getId, orderId)
                .update();

    }

    private List<OrderDetail> buildDetails(Long orderId, List<ItemDTO> items, Map<Long, Integer> numMap) {
        List<OrderDetail> details = new ArrayList<>(items.size());
        for (ItemDTO item : items) {
            OrderDetail detail = new OrderDetail();
            detail.setName(item.getName());
            detail.setSpec(item.getSpec());
            detail.setPrice(item.getPrice());
            detail.setNum(numMap.get(item.getId()));
            detail.setItemId(item.getId());
            detail.setImage(item.getImage());
            detail.setOrderId(orderId);
            details.add(detail);
        }
        return details;
    }

    @Override
    @GlobalTransactional
    public void cancelOrder(Long id) {
        //修改订单状态为已取消(状态为5)
        lambdaUpdate().set(Order::getStatus, 5)
                .set(Order::getCloseTime, LocalDateTime.now())
                .set(Order::getUpdateTime, LocalDateTime.now())
                .eq(Order::getId, id)
                .eq(Order::getStatus, 1) //幂等校验
                .update();

        //1.根据订单id查询商品列表
        List<OrderDetail> orderDetails = detailService.lambdaQuery().eq(OrderDetail::getOrderId, id).list();
        // 如果商品列表为空，则退出
        if (CollUtil.isEmpty(orderDetails)) {
            throw new BizIllegalException("该订单没有商品！");
        }
        // 得到商品id列表
        List<OrderDetailDTO> orderDetailDTOS = BeanUtils.copyList(orderDetails, OrderDetailDTO.class);

        for (OrderDetailDTO orderDetailDTO : orderDetailDTOS) {
            //将商品库存改为负数
            orderDetailDTO.setNum(-orderDetailDTO.getNum());
        }
        // 退还商品库存
        itemClient.deductStock(orderDetailDTOS);
    }
}
