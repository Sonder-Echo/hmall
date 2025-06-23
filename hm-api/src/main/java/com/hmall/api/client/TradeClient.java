package com.hmall.api.client;

import com.hmall.api.config.DefaultFeignConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;


//标注是一个Feign客户端，然后指定了微服务名称，这样可以获取该微服务的实例列表:
//并基于负载均衡选择一个服务实例
@FeignClient(value = "trade-service", configuration = DefaultFeignConfig.class)
public interface TradeClient {
    //在接口内：编写要远程调用的方法，这些方法都可以参考自服务提供者（item-service）对应的接口

    //修改订单状态为已支付
    @PutMapping("/orders/{orderId}")
    public void markOrderPaySuccess(@PathVariable("orderId") Long orderId);

}
