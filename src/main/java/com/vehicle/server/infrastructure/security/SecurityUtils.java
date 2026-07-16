package com.vehicle.server.infrastructure.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 安全上下文工具类，用于获取当前登录用户信息。
 */
public class SecurityUtils {

    private SecurityUtils() {
    }

    /**
     * 获取当前登录用户的 ID。
     */
    public static Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new IllegalStateException("用户未登录");
        }
        Object details = auth.getDetails();
        if (details instanceof JwtUserDetails jwtUserDetails) {
            return jwtUserDetails.getUserId();
        }
        throw new IllegalStateException("无法获取用户ID");
    }

    /**
     * 获取当前登录用户的用户名。
     */
    public static String getCurrentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new IllegalStateException("用户未登录");
        }
        return auth.getName();
    }
}
