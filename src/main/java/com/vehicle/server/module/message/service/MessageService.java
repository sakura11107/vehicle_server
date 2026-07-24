package com.vehicle.server.module.message.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.vehicle.server.common.dto.PageRequest;
import com.vehicle.server.common.dto.PageResponse;
import com.vehicle.server.common.exception.BusinessException;
import com.vehicle.server.common.exception.ErrorCode;
import com.vehicle.server.common.id.SnowflakeIdGenerator;
import com.vehicle.server.infrastructure.mq.RabbitMQConfig;
import com.vehicle.server.infrastructure.websocket.WebSocketSessionManager;
import com.vehicle.server.module.message.dto.ConversationResponse;
import com.vehicle.server.module.message.dto.MessageResponse;
import com.vehicle.server.module.message.entity.Message;
import com.vehicle.server.module.message.mapper.MessageMapper;
import com.vehicle.server.module.system.user.entity.SysUser;
import com.vehicle.server.module.system.user.service.SysUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MessageService {

    private static final String UNREAD_PREFIX = "unread:";

    private final MessageMapper messageMapper;
    private final SysUserService userService;
    private final StringRedisTemplate redisTemplate;
    private final SnowflakeIdGenerator idGenerator;
    private final RabbitTemplate rabbitTemplate;
    private final SimpMessagingTemplate messagingTemplate;
    private final WebSocketSessionManager sessionManager;

    /**
     * 统一入口：投递到 MQ，由监听器落库并推送。
     */
    public void enqueue(Long senderId, Long receiverId, String content) {
        if (senderId.equals(receiverId)) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED);
        }
        userService.requireUser(receiverId);
        rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE, RabbitMQConfig.ROUTING_KEY, Map.of(
                "senderId", senderId.toString(),
                "receiverId", receiverId.toString(),
                "content", content
        ));
    }

    /**
     * 落库 + 未读计数 + WebSocket 推送（仅 MQ 消费者调用）。
     */
    @Transactional
    public MessageResponse persistAndPush(Long senderId, Long receiverId, String content) {
        if (senderId.equals(receiverId)) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED);
        }
        SysUser receiver = userService.requireUser(receiverId);
        userService.requireUser(senderId);

        Message message = new Message();
        message.setId(idGenerator.nextId());
        message.setSenderId(senderId);
        message.setReceiverId(receiverId);
        message.setContent(content);
        message.setIsRead(0);
        messageMapper.insert(message);

        redisTemplate.opsForValue().increment(UNREAD_PREFIX + receiverId);

        MessageResponse response = toResponse(message);
        if (sessionManager.isUserOnline(receiverId)) {
            messagingTemplate.convertAndSendToUser(
                    receiver.getUsername(),
                    "/queue/messages",
                    response
            );
        }
        return response;
    }

    @Transactional(readOnly = true)
    public PageResponse<MessageResponse> getChatHistory(Long currentUserId, Long otherUserId, PageRequest pageRequest) {
        LambdaQueryWrapper<Message> wrapper = new LambdaQueryWrapper<Message>()
                .and(w -> w
                        .eq(Message::getSenderId, currentUserId)
                        .eq(Message::getReceiverId, otherUserId)
                        .or()
                        .eq(Message::getSenderId, otherUserId)
                        .eq(Message::getReceiverId, currentUserId))
                .orderByDesc(Message::getCreatedTime);

        IPage<Message> page = messageMapper.selectPage(
                new Page<>(pageRequest.page(), pageRequest.size()), wrapper);

        Map<Long, String> userMap = userService.mapByIds(Set.of(currentUserId, otherUserId)).entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().getUsername()));

        List<MessageResponse> records = page.getRecords().stream()
                .map(m -> MessageResponse.from(m, userMap.get(m.getSenderId()), userMap.get(m.getReceiverId())))
                .toList();

        return new PageResponse<>(records, page.getTotal(), pageRequest.page(), pageRequest.size(), (int) page.getPages());
    }

    @Transactional(readOnly = true)
    public PageResponse<ConversationResponse> getConversations(Long currentUserId, PageRequest pageRequest) {
        long total = messageMapper.countConversations(currentUserId);
        if (total == 0) {
            return new PageResponse<>(List.of(), 0, pageRequest.page(), pageRequest.size(), 0);
        }

        int offset = (pageRequest.page() - 1) * pageRequest.size();
        int totalPages = (int) Math.ceil((double) total / pageRequest.size());

        List<Message> latestMessages = messageMapper.selectLatestMessagesPerContact(
                currentUserId, pageRequest.size(), offset);

        Set<Long> otherUserIds = latestMessages.stream()
                .map(m -> m.getSenderId().equals(currentUserId) ? m.getReceiverId() : m.getSenderId())
                .collect(Collectors.toSet());

        Map<Long, String> userMap = userService.mapByIds(otherUserIds).entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().getUsername()));

        LambdaQueryWrapper<Message> unreadWrapper = new LambdaQueryWrapper<Message>()
                .eq(Message::getReceiverId, currentUserId)
                .eq(Message::getIsRead, 0)
                .in(Message::getSenderId, otherUserIds);
        List<Message> unreadMessages = messageMapper.selectList(unreadWrapper);
        Map<Long, Long> unreadCountMap = unreadMessages.stream()
                .collect(Collectors.groupingBy(Message::getSenderId, Collectors.counting()));

        List<ConversationResponse> records = latestMessages.stream()
                .map(lastMsg -> {
                    Long otherUserId = lastMsg.getSenderId().equals(currentUserId)
                            ? lastMsg.getReceiverId() : lastMsg.getSenderId();
                    long unreadCount = unreadCountMap.getOrDefault(otherUserId, 0L);
                    return new ConversationResponse(
                            otherUserId,
                            userMap.getOrDefault(otherUserId, "未知用户"),
                            lastMsg.getContent(),
                            lastMsg.getCreatedTime(),
                            unreadCount
                    );
                })
                .toList();

        return new PageResponse<>(records, total, pageRequest.page(), pageRequest.size(), totalPages);
    }

    @Transactional
    public void markAsRead(Long currentUserId, Long otherUserId) {
        LambdaQueryWrapper<Message> countWrapper = new LambdaQueryWrapper<Message>()
                .eq(Message::getSenderId, otherUserId)
                .eq(Message::getReceiverId, currentUserId)
                .eq(Message::getIsRead, 0);
        long count = messageMapper.selectCount(countWrapper);

        LambdaQueryWrapper<Message> wrapper = new LambdaQueryWrapper<Message>()
                .eq(Message::getSenderId, otherUserId)
                .eq(Message::getReceiverId, currentUserId)
                .eq(Message::getIsRead, 0);

        Message update = new Message();
        update.setIsRead(1);
        messageMapper.update(update, wrapper);

        if (count > 0) {
            Long remaining = redisTemplate.opsForValue().decrement(UNREAD_PREFIX + currentUserId, count);
            if (remaining != null && remaining < 0) {
                redisTemplate.delete(UNREAD_PREFIX + currentUserId);
            }
        }
    }

    @Transactional
    public void markAllAsRead(Long currentUserId) {
        LambdaQueryWrapper<Message> wrapper = new LambdaQueryWrapper<Message>()
                .eq(Message::getReceiverId, currentUserId)
                .eq(Message::getIsRead, 0);

        Message update = new Message();
        update.setIsRead(1);
        messageMapper.update(update, wrapper);

        redisTemplate.delete(UNREAD_PREFIX + currentUserId);
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(Long currentUserId) {
        String countStr = redisTemplate.opsForValue().get(UNREAD_PREFIX + currentUserId);
        if (countStr != null) {
            return Long.parseLong(countStr);
        }
        LambdaQueryWrapper<Message> wrapper = new LambdaQueryWrapper<Message>()
                .eq(Message::getReceiverId, currentUserId)
                .eq(Message::getIsRead, 0);
        long count = messageMapper.selectCount(wrapper);
        if (count > 0) {
            redisTemplate.opsForValue().set(UNREAD_PREFIX + currentUserId, String.valueOf(count), 24, TimeUnit.HOURS);
        }
        return count;
    }

    public MessageResponse toResponse(Message message) {
        Map<Long, SysUser> userMap = userService.mapByIds(Set.of(message.getSenderId(), message.getReceiverId()));
        SysUser sender = userMap.get(message.getSenderId());
        SysUser receiver = userMap.get(message.getReceiverId());
        return MessageResponse.from(
                message,
                sender != null ? sender.getUsername() : null,
                receiver != null ? receiver.getUsername() : null);
    }
}
