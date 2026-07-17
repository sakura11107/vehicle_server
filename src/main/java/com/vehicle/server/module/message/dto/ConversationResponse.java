package com.vehicle.server.module.message.dto;

import java.time.LocalDateTime;

public record ConversationResponse(
        Long userId,
        String userName,
        String lastMessage,
        LocalDateTime lastMessageTime,
        Long unreadCount) {
}
