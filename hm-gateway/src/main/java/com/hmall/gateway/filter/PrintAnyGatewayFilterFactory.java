package com.hmall.gateway.filter;

import lombok.Data;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.OrderedGatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

@Component
public class PrintAnyGatewayFilterFactory extends AbstractGatewayFilterFactory<PrintAnyGatewayFilterFactory.Config> {
    @Override
    public GatewayFilter apply(Config config) {
        /*return new GatewayFilter() {
            @Override
            public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
                System.out.println("执行了 PrintAnyGatewayFilterFactory 的pre...");
                return chain.filter(exchange).then(Mono.fromRunnable(()->{
                    System.out.println("执行了 PrintAnyGatewayFilterFactory 的post...");
                }));
            }
        };*/

        return new OrderedGatewayFilter(new GatewayFilter() {
            @Override
            public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
                //接收来自配置文件中的参数
                String a = config.getA();
                System.out.println("接收来自配置文件参数 a = " + a);
                String b = config.getB();
                System.out.println("接收来自配置文件参数 b = " + b);
                String c = config.getC();
                System.out.println("接收来自配置文件参数 c = " + c);

                return chain.filter(exchange).then(Mono.fromRunnable(()->{
                    System.out.println("执行了 PrintAnyGatewayFilterFactory 的post...");
                }));
            }
        }, 100);
    }

    //接受参数的静态内部类
    @Data
    public static class Config{
        private String a;
        private String b;
        private String c;
    }

    //配置接受来自配置文件中参数与上述属性对应的关系
    @Override
    public List<String> shortcutFieldOrder() {
        return List.of("a","b","c");
    }

    //将config传递给父类：接受配置文件的参数
    public PrintAnyGatewayFilterFactory(){
        super(Config.class);
    }
}
