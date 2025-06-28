package com.hmall.api.client;

import com.hmall.api.config.DefaultFeignConfig;
import com.hmall.api.dto.PayOrderDTO;
import com.hmall.api.fallback.PayClientFallback;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(value = "pay-service", fallbackFactory = PayClientFallback.class, configuration = DefaultFeignConfig.class)
public interface PayClient {

    //根据订单id查询支付订单信息
    @GetMapping("/pay-orders/biz/{id}")
    public PayOrderDTO queryPayOrderByBizOrderNo(@PathVariable Long id);

}
