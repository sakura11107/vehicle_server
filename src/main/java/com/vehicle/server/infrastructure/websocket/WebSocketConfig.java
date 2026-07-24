package com.vehicle.server.infrastructure.websocket;

import com.vehicle.server.infrastructure.security.JwtUtil;
import com.vehicle.server.module.system.user.entity.SysUser;
import com.vehicle.server.module.system.user.service.SysUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import java.security.Principal;

@Slf4j
@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final JwtUtil jwtUtil;
    private final WebSocketSessionManager sessionManager;
    private final SysUserService userService;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/queue");
        registry.setApplicationDestinationPrefixes("/app");
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("http://localhost:*", "http://127.0.0.1:*")
                .withSockJS();
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
                if (accessor == null) {
                    return message;
                }

                if (StompCommand.CONNECT.equals(accessor.getCommand())) {
                    handleConnect(accessor);
                } else if (StompCommand.DISCONNECT.equals(accessor.getCommand())) {
                    handleDisconnect(accessor);
                }

                return message;
            }
        });
    }

    private void handleConnect(StompHeaderAccessor accessor) {
        String token = accessor.getFirstNativeHeader("Authorization");
        if (token == null || !token.startsWith("Bearer ")) {
            throw new MessageDeliveryException("WebSocket 未提供有效 Authorization");
        }
        try {
            String jwt = token.substring(7);
            if (!jwtUtil.isTokenValid(jwt)) {
                throw new MessageDeliveryException("WebSocket Token 无效或已过期");
            }
            String username = jwtUtil.extractUsername(jwt);
            SysUser user = userService.findByUsername(username);
            if (user == null || user.getStatus() == null || user.getStatus() == 0) {
                throw new MessageDeliveryException("WebSocket 用户不存在或已禁用");
            }

            accessor.setUser(new Principal() {
                @Override
                public String getName() {
                    return username;
                }
            });
            sessionManager.userConnected(user.getId(), accessor.getSessionId());
            log.info("WebSocket STOMP 连接认证成功: {}", username);
        } catch (MessageDeliveryException e) {
            throw e;
        } catch (Exception e) {
            log.warn("WebSocket STOMP 认证失败: {}", e.getMessage());
            throw new MessageDeliveryException("WebSocket 认证失败");
        }
    }

    private void handleDisconnect(StompHeaderAccessor accessor) {
        Principal principal = accessor.getUser();
        if (principal != null) {
            SysUser user = userService.findByUsername(principal.getName());
            if (user != null) {
                sessionManager.userDisconnected(user.getId());
                log.info("WebSocket STOMP 断开连接: {}", principal.getName());
            }
        }
    }
}
