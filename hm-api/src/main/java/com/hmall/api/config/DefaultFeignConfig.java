package com.hmall.api.config;

import com.hmall.api.fallback.CartClientFallback;
import com.hmall.api.fallback.ItemClientFallback;
import com.hmall.api.fallback.TradeClientFallback;
import com.hmall.api.fallback.UserClientFallback;
import com.hmall.common.utils.UserContext;
import feign.Logger;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.context.annotation.Bean;

public class DefaultFeignConfig {
    //注册关于商品远程调用客户端的fallback
    @Bean
    public ItemClientFallback itemClientFallback() {
        return new ItemClientFallback();
    }
    //注册关于购物车远程调用客户端的fallback
    @Bean
    public CartClientFallback cartClientFallback(){
        return new CartClientFallback();
    }
    //注册关于交易远程调用客户端的fallback
    @Bean
    public TradeClientFallback tradeClientFallback(){
        return new TradeClientFallback();
    }
    //注册关于用户远程调用客户端的fallback
    @Bean
    public UserClientFallback userClientFallback(){
        return new UserClientFallback();
    }

    //注册feign日志记录级别：none->basic-headers->full
    @Bean
    public Logger.Level defaultFeignLoggerLevel() {
        return Logger.Level.FULL;
    }

    //feign请求拦截器
    @Bean
    public RequestInterceptor feignRequestInterceptor(){
        return new RequestInterceptor() {
            @Override
            public void apply(RequestTemplate template) {
                //获取线程变量中的用户id
                Long userId = UserContext.getUser();
                //设置到feign请求头
                if(userId != null){
                    template.header("user-info", userId.toString());
                }
            }
        };
    }

    //配置feign的日志级别
    @Bean
    public Logger.Level feignLoggerLevel(){
        return Logger.Level.FULL;
    }

}
