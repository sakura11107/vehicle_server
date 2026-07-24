package com.vehicle.server.module.message.listener;

import com.vehicle.server.infrastructure.mq.RabbitMQConfig;
import com.vehicle.server.module.message.service.MessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class MessageRabbitListener {

    private final MessageService messageService;

    @RabbitListener(queues = RabbitMQConfig.QUEUE_SEND)
    public void handleMessage(Map<String, String> payload) {
        try {
            Long senderId = Long.parseLong(payload.get("senderId"));
            Long receiverId = Long.parseLong(payload.get("receiverId"));
            String content = payload.get("content");

            log.info("收到MQ消息: sender={}, receiver={}", senderId, receiverId);
            messageService.persistAndPush(senderId, receiverId, content);
        } catch (Exception e) {
            log.error("MQ消息处理失败，消息将进入死信队列: {}", e.getMessage(), e);
            throw new AmqpRejectAndDontRequeueException("消息处理失败", e);
        }
    }

    @RabbitListener(queues = RabbitMQConfig.DLX_QUEUE)
    public void handleDeadLetter(Map<String, String> payload) {
        log.error("死信消息: sender={}, receiver={}, content={}",
                payload != null ? payload.get("senderId") : null,
                payload != null ? payload.get("receiverId") : null,
                payload != null ? payload.get("content") : null);
    }
}
