package com.vehicle.server.module.message.controller;

import com.vehicle.server.common.api.ApiResponse;
import com.vehicle.server.common.dto.PageRequest;
import com.vehicle.server.common.dto.PageResponse;
import com.vehicle.server.infrastructure.security.SecurityUtils;
import com.vehicle.server.module.message.dto.ConversationResponse;
import com.vehicle.server.module.message.dto.MessageCreateRequest;
import com.vehicle.server.module.message.dto.MessageResponse;
import com.vehicle.server.module.message.entity.Message;
import com.vehicle.server.module.message.service.MessageService;
import com.vehicle.server.module.system.user.entity.SysUser;
import com.vehicle.server.module.system.user.mapper.SysUserMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/messages")
@RequiredArgsConstructor
@Tag(name = "站内信", description = "用户之间的私信功能")
public class MessageController {

    private final MessageService messageService;
    private final SimpMessagingTemplate messagingTemplate;
    private final SysUserMapper userMapper;

    @PostMapping
    @Operation(summary = "发送私信", description = "同步保存并返回消息，同时推送给接收方")
    public ApiResponse<MessageResponse> send(@Valid @RequestBody MessageCreateRequest request) {
        Long senderId = SecurityUtils.getCurrentUserId();
        Message message = messageService.send(senderId, request.receiverId(), request.content());
        MessageResponse response = messageService.toResponse(message);

        SysUser receiver = userMapper.selectById(request.receiverId());
        if (receiver != null) {
            messagingTemplate.convertAndSendToUser(
                    receiver.getUsername(), "/queue/messages", response);
        }

        return ApiResponse.success(response);
    }

    @GetMapping("/conversations")
    @Operation(summary = "会话列表", description = "每个联系人的最后一条消息，支持分页")
    public ApiResponse<PageResponse<ConversationResponse>> conversations(@Valid PageRequest pageRequest) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        return ApiResponse.success(messageService.getConversations(currentUserId, pageRequest));
    }

    @GetMapping("/{userId}")
    @Operation(summary = "聊天记录", description = "与指定用户的聊天记录，支持分页")
    public ApiResponse<PageResponse<MessageResponse>> chatHistory(
            @PathVariable Long userId,
            @Valid PageRequest pageRequest) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        return ApiResponse.success(messageService.getChatHistory(currentUserId, userId, pageRequest));
    }

    @PutMapping("/{id}/read")
    @Operation(summary = "标记已读", description = "标记与某用户的聊天为已读")
    public ApiResponse<Void> markAsRead(@PathVariable Long id) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        messageService.markAsRead(currentUserId, id);
        return ApiResponse.success(null);
    }

    @PutMapping("/read-all")
    @Operation(summary = "全部标记已读")
    public ApiResponse<Void> markAllAsRead() {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        messageService.markAllAsRead(currentUserId);
        return ApiResponse.success(null);
    }

    @GetMapping("/unread-count")
    @Operation(summary = "未读消息数", description = "从 Redis 获取未读总数")
    public ApiResponse<Long> unreadCount() {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        return ApiResponse.success(messageService.getUnreadCount(currentUserId));
    }
}
