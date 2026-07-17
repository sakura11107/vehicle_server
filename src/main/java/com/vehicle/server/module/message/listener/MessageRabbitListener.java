package com.vehicle.server.module.message.listener;

import com.vehicle.server.module.message.entity.Message;
import com.vehicle.server.module.message.service.MessageService;
import com.vehicle.server.module.message.dto.MessageResponse;
import com.vehicle.server.infrastructure.websocket.WebSocketSessionManager;
import com.vehicle.server.module.system.user.entity.SysUser;
import com.vehicle.server.module.system.user.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class MessageRabbitListener {

    private final MessageService messageService;
    private final SimpMessagingTemplate messagingTemplate;
    private final WebSocketSessionManager sessionManager;
    private final SysUserMapper userMapper;

    @RabbitListener(queues = "message.send.queue")
    public void handleMessage(Map<String, String> payload) {
        Long senderId = Long.parseLong(payload.get("senderId"));
        Long receiverId = Long.parseLong(payload.get("receiverId"));
        String content = payload.get("content");

        log.info("收到MQ消息: sender={}, receiver={}", senderId, receiverId);

        Message message = messageService.send(senderId, receiverId, content);
        MessageResponse response = messageService.toResponse(message);

        if (sessionManager.isUserOnline(receiverId)) {
            SysUser receiver = userMapper.selectById(receiverId);
            if (receiver != null) {
                messagingTemplate.convertAndSendToUser(
                        receiver.getUsername(),
                        "/queue/messages",
                        response
                );
            }
        }
    }
}
