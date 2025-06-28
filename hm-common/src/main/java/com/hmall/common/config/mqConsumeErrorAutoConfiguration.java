package com.hmall.common.config;


import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.retry.MessageRecoverer;
import org.springframework.amqp.rabbit.retry.RepublishMessageRecoverer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

@Component
//条件注解：当某个属性值条件成立时，当前配置文件才生效
@ConditionalOnProperty(name = "spring.rabbitmq.listener.simple.retry.enabled", havingValue = "true")
public class mqConsumeErrorAutoConfiguration {
    //获取当前微服务名
    @Value("${spring.application.name}")
    private String serviceName;

    //声明错误消息交换机
    @Bean
    public DirectExchange errorDirectExchange() {
        return new DirectExchange("error.direct");
    }
    //声明错误消息队列
    @Bean
    public Queue errorQueue(){
        return new Queue(serviceName + ".error.queue", true);
    }

    //声明错误消息绑定上述的交换机和队列
    @Bean
    public Binding errorBinding(Queue errorQueue, DirectExchange errorDirectExchange) {
        return BindingBuilder.bind(errorQueue).to(errorDirectExchange).with(serviceName+".error");
    }

    //设置RabbitTemplate在尝试接收最大次数后投递的交换机和路由key
    @Bean
    public MessageRecoverer RepublishMessageRecoverer(RabbitTemplate rabbitTemplate) {
        return new RepublishMessageRecoverer(rabbitTemplate, "error.direct", serviceName+".error");
    }

}
