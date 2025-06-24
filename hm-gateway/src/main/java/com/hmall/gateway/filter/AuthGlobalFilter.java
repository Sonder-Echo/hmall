package com.hmall.gateway.filter;

import cn.hutool.core.text.AntPathMatcher;
import com.hmall.gateway.config.AuthProperties;
import com.hmall.gateway.config.JwtProperties;
import com.hmall.gateway.utils.JwtTool;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
@EnableConfigurationProperties(AuthProperties.class)
public class AuthGlobalFilter implements GlobalFilter, Ordered {

    private final AuthProperties authProperties;

    //路径判断
    private final AntPathMatcher antPathMatcher = new AntPathMatcher();
    //校验token工具
    private final JwtTool jwtTool;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {

        //1、判断是否要进行登录的请求
        ServerHttpRequest request = exchange.getRequest();
        if(isExclude(request.getPath().toString())){
            // 如果不是需要登录的地址则放行;比如:登录、搜索商品
            return chain.filter(exchange);
        }
        //2、如果要登录;获取请求头的属性 authorization 的值就是令牌
        String token = request.getHeaders().getFirst("authorization");

        try {
            //3、校验令牌;获得用户信息
            Long userId = jwtTool.parseToken(token);

            //4、将用户信息传递到后端微服务
            System.out.println("userId:"+userId);

        } catch (Exception e) {
            // 校验不通过则返回，没有授权;401
            ServerHttpResponse response = exchange.getResponse();
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return response.setComplete();
        }
        //校验通过则放行
        return chain.filter(exchange);
    }

    /**
     * 判断请求路径是否需要登录
     * @param path
     * @return
     */
    private boolean isExclude(String path) {
        for (String excludePath : authProperties.getExcludePaths()) {
            if(antPathMatcher.match(excludePath, path)){
                return true;
            }
        }
        return false;
    }

    @Override
    public int getOrder() {
        return 0;
    }
}
