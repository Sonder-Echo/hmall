package com.hmall.api.client;

import com.hmall.api.config.DefaultFeignConfig;
import com.hmall.api.dto.ItemDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Collection;
import java.util.List;

//标注是一个Feign客户端，然后指定了微服务名称，这样可以获取该微服务的实例列表:
//并基于负载均衡选择一个服务实例
//@FeignClient("item-service")
@FeignClient(value = "item-service", configuration = DefaultFeignConfig.class)
public interface ItemClient {

    //在接口内：编写要远程调用的方法，这些方法都可以参考自服务提供者（item-service）对应的接口

    //根据商品id集合获取商品dto列表
    @GetMapping("/items")
    public List<ItemDTO> queryItemByIds(@RequestParam("ids") Collection<Long> ids);

}
