package com.vehicle.server.infrastructure.security;

import com.vehicle.server.common.exception.BusinessException;
import com.vehicle.server.common.exception.ErrorCode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Collection;

/**
 * 安全上下文工具类，用于获取当前登录用户信息。
 */
@Component
public class SecurityUtils {

    private static RoleHierarchy roleHierarchy;

    @Autowired
    public void setRoleHierarchy(RoleHierarchy roleHierarchy) {
        SecurityUtils.roleHierarchy = roleHierarchy;
    }

    private SecurityUtils() {
    }

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

    public static String getCurrentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return auth.getName();
    }

    public static boolean hasAuthority(String authority) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return false;
        }
        Collection<? extends GrantedAuthority> authorities = auth.getAuthorities();
        if (roleHierarchy != null) {
            authorities = roleHierarchy.getReachableGrantedAuthorities(authorities);
        }
        return authorities.stream()
                .anyMatch(granted -> granted.getAuthority().equals(authority));
    }

    public static boolean isManagerOrAdmin() {
        return hasAuthority("ROLE_MANAGER");
    }

    public static boolean isAdmin() {
        return hasAuthority("ROLE_ADMIN");
    }
}
