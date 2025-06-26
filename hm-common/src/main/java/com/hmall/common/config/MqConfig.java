package com.hmall.common.config;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
//如果类加载器中存在RabbitTemplate类，则加载该配置类
@ConditionalOnClass(RabbitTemplate.class)
public class MqConfig {
    //注册json消息转换器
    @Bean
    public MessageConverter jsonMessageConverter() {
        Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter();
        //设置每个消息都携带一个id
        converter.setCreateMessageIds(true);
        return converter;
    }

}
