package com.vehicle.server.infrastructure.security;

import com.vehicle.server.common.exception.BusinessException;
import com.vehicle.server.common.exception.ErrorCode;
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
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        Object details = auth.getDetails();
        if (details instanceof JwtUserDetails jwtUserDetails) {
            return jwtUserDetails.getUserId();
        }
        throw new BusinessException(ErrorCode.UNAUTHORIZED);
    }

    /**
     * 获取当前登录用户的用户名。
     */
    public static String getCurrentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return auth.getName();
    }

    /**
     * 判断当前登录用户是否拥有指定权限字符串（如 ROLE_ADMIN）。
     */
    public static boolean hasAuthority(String authority) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return false;
        }
        return auth.getAuthorities().stream()
                .anyMatch(granted -> granted.getAuthority().equals(authority));
    }

    /**
     * 当前登录用户是否为车辆管理员或系统管理员。
     */
    public static boolean isManagerOrAdmin() {
        return hasAuthority("ROLE_MANAGER") || hasAuthority("ROLE_ADMIN");
    }

    /**
     * 当前登录用户是否为系统管理员。
     */
    public static boolean isAdmin() {
        return hasAuthority("ROLE_ADMIN");
    }
}
