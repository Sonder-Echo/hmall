package com.hmall.trade.listener;

import com.hmall.api.client.PayClient;
import com.hmall.api.dto.PayOrderDTO;
import com.hmall.common.domain.MultiDelayMessage;
import com.hmall.trade.constants.TradeMqConstants;
import com.hmall.trade.domain.po.Order;
import com.hmall.trade.service.IOrderService;
import lombok.RequiredArgsConstructor;
import org.checkerframework.checker.units.qual.A;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OrderStatusListener {

    private final IOrderService orderService;
    private final PayClient payClient;
    private final RabbitTemplate rabbitTemplate;

    /*
    * 监听延时队列中的订单状态
    * 1.查询订单支付状态
    *   -如果已支付，则更新订单状态为已支付
    *   -如果未支付，如果还有剩余延迟时间，则继续发送下一个消息
    *   -如果超时则取消订单
    * 2.取消订单
    *   -查询订单明细order-detail
    *   -根据商品id，退还库存
    *   -修改订单状态为订单取消
     */
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = TradeMqConstants.DELAY_ORDER_QUEUE, durable = "true"),
            exchange = @Exchange(value = TradeMqConstants.DELAY_EXCHANGE, delayed = "true", type = ExchangeTypes.TOPIC, durable = "true"),
            key = TradeMqConstants.DELAY_ORDER_ROUTING_KEY
    ))
    public void ListenOrderCheckDelayMessage(MultiDelayMessage<Long> message){
        //1.查询订单
        Long orderId = message.getData();
        Order order = orderService.getById(orderId);
        //2.判断订单是否为空或订单状态是不是待支付
        if (order == null || !order.getStatus().equals(1)){
            // 订单不存在或者订单状态不是待支付，则不处理
            return;
        }
        //3.未支付订单，查询支付订单信息
        PayOrderDTO payOrderDTO = payClient.queryPayOrderByBizOrderNo(orderId);
        //4.订单如果支付成功，则更新订单状态
        if (payOrderDTO != null && payOrderDTO.getStatus().equals(3)){
            orderService.markOrderPaySuccess(orderId);
            return;
        }
        // 如果还有剩余延迟时间，则发送下一个延迟消息
        if(message.hasNextDelay()){
            //获取下一个延迟消息
            Integer delayTime = message.removeNextDelay();

            rabbitTemplate.convertAndSend(TradeMqConstants.DELAY_EXCHANGE, TradeMqConstants.DELAY_ORDER_ROUTING_KEY, message, new MessagePostProcessor() {
                @Override
                public Message postProcessMessage(Message message) throws AmqpException {
                    message.getMessageProperties().setDelay(delayTime);
                    return message;
                }
            });
            return;
        }

        //5.订单如果超时未支付，则取消订单
        orderService.cancelOrder(orderId);
    }
}
