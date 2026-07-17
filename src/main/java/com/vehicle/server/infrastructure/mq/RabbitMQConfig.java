package com.vehicle.server.infrastructure.mq;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE = "message.exchange";
    public static final String QUEUE_SEND = "message.send.queue";
    public static final String ROUTING_KEY = "message.send";

    @Bean
    public DirectExchange messageExchange() {
        return new DirectExchange(EXCHANGE);
    }

    @Bean
    public Queue messageSendQueue() {
        return new Queue(QUEUE_SEND, true);
    }

    @Bean
    public Binding messageBinding(Queue messageSendQueue, DirectExchange messageExchange) {
        return BindingBuilder.bind(messageSendQueue).to(messageExchange).with(ROUTING_KEY);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
