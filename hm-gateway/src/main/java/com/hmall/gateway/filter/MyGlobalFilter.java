package com.hmall.gateway.filter;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class MyGlobalFilter implements GlobalFilter, Ordered {
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        //获取请求对象
        ServerHttpRequest request = exchange.getRequest();
        System.out.println("执行 MyGlobalFilter 过滤器 pre...");
        return chain.filter(exchange).then(Mono.fromRunnable(()->{
            System.out.println("执行 MyGlobalFilter 过滤器 post...");
        }));
    }

    @Override
    public int getOrder() {
        //优先级，数值越小优先级越高
        return 0;
    }
}
