package com.vehicle.server.module.message.dto;

import com.vehicle.server.module.message.entity.Message;

import java.time.LocalDateTime;

public record MessageResponse(
        Long id,
        Long senderId,
        Long receiverId,
        String content,
        Boolean isRead,
        LocalDateTime createdTime,
        String senderName,
        String receiverName) {

    public static MessageResponse from(Message message, String senderName, String receiverName) {
        return new MessageResponse(
                message.getId(),
                message.getSenderId(),
                message.getReceiverId(),
                message.getContent(),
                message.getIsRead() == 1,
                message.getCreatedTime(),
                senderName,
                receiverName
        );
    }
}
