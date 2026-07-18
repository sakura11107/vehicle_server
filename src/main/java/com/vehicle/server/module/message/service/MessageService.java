package com.vehicle.server.module.message.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.vehicle.server.common.dto.PageRequest;
import com.vehicle.server.common.dto.PageResponse;
import com.vehicle.server.common.exception.BusinessException;
import com.vehicle.server.common.exception.ErrorCode;
import com.vehicle.server.common.id.SnowflakeIdGenerator;
import com.vehicle.server.module.message.dto.ConversationResponse;
import com.vehicle.server.module.message.dto.MessageResponse;
import com.vehicle.server.module.message.entity.Message;
import com.vehicle.server.module.message.mapper.MessageMapper;
import com.vehicle.server.module.system.user.entity.SysUser;
import com.vehicle.server.module.system.user.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MessageService {

    private static final Integer NOT_DELETED = 0;
    private static final String UNREAD_PREFIX = "unread:";

    private final MessageMapper messageMapper;
    private final SysUserMapper userMapper;
    private final StringRedisTemplate redisTemplate;
    private final SnowflakeIdGenerator idGenerator;

    @Transactional
    public Message send(Long senderId, Long receiverId, String content) {
        if (senderId.equals(receiverId)) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED);
        }
        SysUser receiver = userMapper.selectById(receiverId);
        if (receiver == null || receiver.getDeleted() != NOT_DELETED) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        LocalDateTime now = LocalDateTime.now();
        Message message = new Message();
        message.setId(idGenerator.nextId());
        message.setSenderId(senderId);
        message.setReceiverId(receiverId);
        message.setContent(content);
        message.setIsRead(0);
        message.setDeleted(NOT_DELETED);
        message.setCreatedTime(now);
        message.setUpdatedTime(now);
        messageMapper.insert(message);

        redisTemplate.opsForValue().increment(UNREAD_PREFIX + receiverId);

        return message;
    }

    @Transactional(readOnly = true)
    public PageResponse<MessageResponse> getChatHistory(Long currentUserId, Long otherUserId, PageRequest pageRequest) {
        LambdaQueryWrapper<Message> wrapper = new LambdaQueryWrapper<Message>()
                .eq(Message::getDeleted, NOT_DELETED)
                .and(w -> w
                        .eq(Message::getSenderId, currentUserId)
                        .eq(Message::getReceiverId, otherUserId)
                        .or()
                        .eq(Message::getSenderId, otherUserId)
                        .eq(Message::getReceiverId, currentUserId))
                .orderByDesc(Message::getCreatedTime);

        List<Message> allMessages = messageMapper.selectList(wrapper);

        int total = allMessages.size();
        int page = pageRequest.page();
        int size = pageRequest.size();
        int offset = (page - 1) * size;
        List<Message> pagedMessages = (offset < total)
                ? allMessages.subList(offset, Math.min(offset + size, total))
                : List.of();

        Set<Long> userIds = Set.of(currentUserId, otherUserId);
        Map<Long, String> userMap = userMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(SysUser::getId, SysUser::getUsername));

        int totalPages = (total + size - 1) / size;
        List<MessageResponse> records = pagedMessages.stream()
                .map(m -> MessageResponse.from(m, userMap.get(m.getSenderId()), userMap.get(m.getReceiverId())))
                .toList();

        return new PageResponse<>(records, total, page, size, totalPages);
    }

    @Transactional(readOnly = true)
    public List<ConversationResponse> getConversations(Long currentUserId) {
        LambdaQueryWrapper<Message> wrapper = new LambdaQueryWrapper<Message>()
                .eq(Message::getDeleted, NOT_DELETED)
                .and(w -> w
                        .eq(Message::getSenderId, currentUserId)
                        .or(inner -> inner.eq(Message::getReceiverId, currentUserId)))
                .orderByDesc(Message::getCreatedTime);

        List<Message> allMessages = messageMapper.selectList(wrapper);

        Map<Long, Message> lastMessageMap = new LinkedHashMap<>();
        for (Message msg : allMessages) {
            Long otherUserId = msg.getSenderId().equals(currentUserId) ? msg.getReceiverId() : msg.getSenderId();
            lastMessageMap.putIfAbsent(otherUserId, msg);
        }

        if (lastMessageMap.isEmpty()) {
            return List.of();
        }

        Set<Long> otherUserIds = lastMessageMap.keySet();

        Map<Long, String> userMap = userMapper.selectBatchIds(otherUserIds).stream()
                .collect(Collectors.toMap(SysUser::getId, SysUser::getUsername));

        LambdaQueryWrapper<Message> unreadWrapper = new LambdaQueryWrapper<Message>()
                .eq(Message::getDeleted, NOT_DELETED)
                .eq(Message::getReceiverId, currentUserId)
                .eq(Message::getIsRead, 0)
                .in(Message::getSenderId, otherUserIds);
        List<Message> unreadMessages = messageMapper.selectList(unreadWrapper);
        Map<Long, Long> unreadCountMap = unreadMessages.stream()
                .collect(Collectors.groupingBy(Message::getSenderId, Collectors.counting()));

        return lastMessageMap.entrySet().stream()
                .map(entry -> {
                    Long otherUserId = entry.getKey();
                    Message lastMsg = entry.getValue();
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
    }

    @Transactional
    public void markAsRead(Long currentUserId, Long otherUserId) {
        LambdaQueryWrapper<Message> countWrapper = new LambdaQueryWrapper<Message>()
                .eq(Message::getDeleted, NOT_DELETED)
                .eq(Message::getSenderId, otherUserId)
                .eq(Message::getReceiverId, currentUserId)
                .eq(Message::getIsRead, 0);
        long count = messageMapper.selectCount(countWrapper);

        LambdaQueryWrapper<Message> wrapper = new LambdaQueryWrapper<Message>()
                .eq(Message::getDeleted, NOT_DELETED)
                .eq(Message::getSenderId, otherUserId)
                .eq(Message::getReceiverId, currentUserId)
                .eq(Message::getIsRead, 0);

        Message update = new Message();
        update.setIsRead(1);
        update.setUpdatedTime(LocalDateTime.now());
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
        LambdaQueryWrapper<Message> countWrapper = new LambdaQueryWrapper<Message>()
                .eq(Message::getDeleted, NOT_DELETED)
                .eq(Message::getReceiverId, currentUserId)
                .eq(Message::getIsRead, 0);
        long count = messageMapper.selectCount(countWrapper);

        LambdaQueryWrapper<Message> wrapper = new LambdaQueryWrapper<Message>()
                .eq(Message::getDeleted, NOT_DELETED)
                .eq(Message::getReceiverId, currentUserId)
                .eq(Message::getIsRead, 0);

        Message update = new Message();
        update.setIsRead(1);
        update.setUpdatedTime(LocalDateTime.now());
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
                .eq(Message::getDeleted, NOT_DELETED)
                .eq(Message::getReceiverId, currentUserId)
                .eq(Message::getIsRead, 0);
        long count = messageMapper.selectCount(wrapper);
        if (count > 0) {
            redisTemplate.opsForValue().set(UNREAD_PREFIX + currentUserId, String.valueOf(count), 24, TimeUnit.HOURS);
        }
        return count;
    }

    public MessageResponse toResponse(Message message) {
        Set<Long> userIds = Set.of(message.getSenderId(), message.getReceiverId());
        Map<Long, String> userMap = userMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(SysUser::getId, SysUser::getUsername));
        return MessageResponse.from(message, userMap.get(message.getSenderId()), userMap.get(message.getReceiverId()));
    }
}
