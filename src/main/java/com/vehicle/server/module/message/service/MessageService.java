package com.vehicle.server.module.message.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
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

        IPage<Message> page = messageMapper.selectPage(
                new Page<>(pageRequest.page(), pageRequest.size()), wrapper);
        List<Message> pagedMessages = page.getRecords();
        int total = (int) page.getTotal();
        int totalPages = (int) page.getPages();

        Set<Long> userIds = Set.of(currentUserId, otherUserId);
        Map<Long, String> userMap = userMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(SysUser::getId, SysUser::getUsername));

        List<MessageResponse> records = pagedMessages.stream()
                .map(m -> MessageResponse.from(m, userMap.get(m.getSenderId()), userMap.get(m.getReceiverId())))
                .toList();

        return new PageResponse<>(records, total, pageRequest.page(), pageRequest.size(), totalPages);
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
