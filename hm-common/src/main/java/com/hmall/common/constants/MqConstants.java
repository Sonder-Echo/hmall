package com.hmall.common.constants;

public class MqConstants {
    // 订单交换机名称
    public static final String TRADE_EXCHANGE_NAME = "trade.topic";
    // 消息队列名称
    public static final String ROUTING_KEY_ORDER_CREATE = "order.create";

    //商品上下架
    public static final String ITEM_EXCHANGE_NAME = "items.topic";
    public static final String ITEM_UP_KEY = "item.up";
    public static final String ITEM_DOWN_KEY = "item.down";
}
