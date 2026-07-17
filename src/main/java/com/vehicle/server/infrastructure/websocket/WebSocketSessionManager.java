package com.vehicle.server.infrastructure.websocket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class WebSocketSessionManager {

    private final ConcurrentHashMap<Long, String> userSessions = new ConcurrentHashMap<>();

    public void userConnected(Long userId, String sessionId) {
        userSessions.put(userId, sessionId);
        log.info("用户 {} 已连接 WebSocket, sessionId={}", userId, sessionId);
    }

    public void userDisconnected(Long userId) {
        userSessions.remove(userId);
        log.info("用户 {} 已断开 WebSocket", userId);
    }

    public boolean isUserOnline(Long userId) {
        return userSessions.containsKey(userId);
    }

    public Set<Long> getOnlineUsers() {
        return userSessions.keySet();
    }
}
