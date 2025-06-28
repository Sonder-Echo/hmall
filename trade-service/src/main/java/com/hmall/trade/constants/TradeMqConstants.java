package com.hmall.trade.constants;

public interface TradeMqConstants {
    // 延时交换机
    String DELAY_EXCHANGE = "trade.delay.topic";
    // 延时队列
    String DELAY_ORDER_QUEUE = "trade.order.delay.queue";
    // 路由key
    String DELAY_ORDER_ROUTING_KEY = "order.query";
}
