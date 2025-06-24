package com.hmall.api.client;

import com.hmall.api.config.DefaultFeignConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Collection;


//标注是一个Feign客户端，然后指定了微服务名称，这样可以获取该微服务的实例列表:
//并基于负载均衡选择一个服务实例
@FeignClient(value = "cart-service", configuration = DefaultFeignConfig.class)
public interface CartClient {
    //在接口内：编写要远程调用的方法，这些方法都可以参考自服务提供者（item-service）对应的接口

    //删除购物车 对应的方法名可以不同，但是建议和服务提供者的方法名一致
    @DeleteMapping("/carts")
    public void deleteCartItemByIds(@RequestParam("ids") Collection<Long> ids);
}
