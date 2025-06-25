package com.hmall.api.fallback;


import com.hmall.api.client.CartClient;
import com.hmall.common.exception.BizIllegalException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;

import java.util.Collection;

@Slf4j
public class CartClientFallback implements FallbackFactory<CartClient> {
    @Override
    public CartClient create(Throwable cause) {
        return new CartClient() {
            @Override
            public void deleteCartItemByIds(Collection<Long> ids) {
                log.error("远程调用CartClient.deleteCartItemByIds 失败，ids={}", ids, cause);
                throw new BizIllegalException("调用购物车清空购物车失败！");
            }
        };
    }
}
