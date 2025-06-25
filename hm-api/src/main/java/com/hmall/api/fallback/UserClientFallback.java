package com.hmall.api.fallback;

import com.hmall.api.client.UserClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;

@Slf4j
public class UserClientFallback implements FallbackFactory<UserClient> {
    @Override
    public UserClient create(Throwable cause) {
        return new UserClient() {
            @Override
            public void deductMoney(String pw, Integer amount) {
                log.info("远程调用UserClient.deductMoney失败，具体参数为：pw={},amount={}", pw, amount,cause);
            }
        };
    }
}
