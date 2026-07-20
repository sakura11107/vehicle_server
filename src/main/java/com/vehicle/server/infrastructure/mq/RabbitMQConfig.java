package com.vehicle.server.infrastructure.mq;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class RabbitMQConfig {

    // 主交换机和队列
    public static final String EXCHANGE = "message.exchange";
    public static final String QUEUE_SEND = "message.send.queue";
    public static final String ROUTING_KEY = "message.send";

    // 死信交换机和队列
    public static final String DLX_EXCHANGE = "message.dlx.exchange";
    public static final String DLX_QUEUE = "message.dlx.queue";
    public static final String DLX_ROUTING_KEY = "message.dlx";

    @Bean
    public DirectExchange messageExchange() {
        return new DirectExchange(EXCHANGE);
    }

    @Bean
    public DirectExchange dlxExchange() {
        return new DirectExchange(DLX_EXCHANGE);
    }

    @Bean
    public Queue messageSendQueue() {
        Map<String, Object> args = new HashMap<>();
        // 设置死信交换机
        args.put("x-dead-letter-exchange", DLX_EXCHANGE);
        // 设置死信路由键
        args.put("x-dead-letter-routing-key", DLX_ROUTING_KEY);
        return new Queue(QUEUE_SEND, true, false, false, args);
    }

    @Bean
    public Queue dlxQueue() {
        return new Queue(DLX_QUEUE, true);
    }

    @Bean
    public Binding messageBinding(Queue messageSendQueue, DirectExchange messageExchange) {
        return BindingBuilder.bind(messageSendQueue).to(messageExchange).with(ROUTING_KEY);
    }

    @Bean
    public Binding dlxBinding(Queue dlxQueue, DirectExchange dlxExchange) {
        return BindingBuilder.bind(dlxQueue).to(dlxExchange).with(DLX_ROUTING_KEY);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
