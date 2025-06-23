package com.hmall.api.client;


import com.hmall.api.config.DefaultFeignConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;

//标注是一个Feign客户端，然后指定了微服务名称，这样可以获取该微服务的实例列表:
//并基于负载均衡选择一个服务实例
@FeignClient(value = "user-service", configuration = DefaultFeignConfig.class)
public interface UserClient {
    //在接口内：编写要远程调用的方法，这些方法都可以参考自服务提供者（item-service）对应的接口

    //扣减余额
    @PutMapping("/users/money/deduct")
    public void deductMoney(@RequestParam("pw") String pw, @RequestParam("amount") Integer amount);

}
