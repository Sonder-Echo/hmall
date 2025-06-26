package com.hmall.cart.listener;

import com.hmall.cart.service.ICartService;
import com.hmall.common.constants.MqConstants;
import com.hmall.common.utils.UserContext;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class OrderStatusListener {
    @Autowired
    private ICartService cartService;

    //接受来自 cart.clear.queue 队列的消息，清理用户对应的购物车商品

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "cart.clear.queue", durable = "true"),
            exchange = @Exchange(value = MqConstants.TRADE_EXCHANGE_NAME, type = ExchangeTypes.TOPIC, durable = "true"),
            key = {MqConstants.ROUTING_KEY_ORDER_CREATE}
    ))
    public void listenOrderCreateMsg(List<Long> itemIds, @Header("user-info") Long userId) {
        // 将当前用户id设置到UserContext中
        UserContext.setUser(userId);
        //调用清理购物车商品方法
        cartService.removeByItemIds(itemIds);
        // 删除用户id
        UserContext.removeUser();
    }


}
