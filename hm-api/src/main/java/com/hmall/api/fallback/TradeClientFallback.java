package com.hmall.api.fallback;

import com.hmall.api.client.TradeClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;

@Slf4j
public class TradeClientFallback implements FallbackFactory<TradeClient> {
    @Override
    public TradeClient create(Throwable cause) {
        return new TradeClient() {
            @Override
            public void markOrderPaySuccess(Long orderId) {
                log.error("远程调用TradeClient.markOrderPaySuccess，具体参数为：{}", orderId, cause);
            }
        };
    }
}
