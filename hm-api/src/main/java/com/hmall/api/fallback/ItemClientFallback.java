package com.hmall.api.fallback;

import com.hmall.api.client.ItemClient;
import com.hmall.api.dto.ItemDTO;
import com.hmall.api.dto.OrderDetailDTO;
import com.hmall.common.exception.BizIllegalException;
import com.hmall.common.utils.CollUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;

import java.util.Collection;
import java.util.List;

@Slf4j
public class ItemClientFallback implements FallbackFactory<ItemClient> {
    @Override
    public ItemClient create(Throwable cause) {
        return new ItemClient() {
            @Override
            public List<ItemDTO> queryItemByIds(Collection<Long> ids) {
                log.error("远程调用ItemClient.queryItemByIds失败，具体参数为：{}", ids, cause);
                return CollUtils.emptyList();
            }

            @Override
            public void deductStock(List<OrderDetailDTO> items) {
                // 这个扣减商品库存的接口：与下单的业务有关联，如果出错了需要将异常跑出来
                log.error("远程调用ItemClient.deductStock失败，具体参数为：{}", items, cause);
                throw new BizIllegalException("扣减商品库存失败！");
            }

            @Override
            public ItemDTO queryItemById(Long id) {
                log.error("远程调用ItemClient.queryItemById失败，具体参数为：{}", id, cause);
                return null;
            }
        };
    }
}
