package com.vehicle.server.module.system.user.dto;

import com.vehicle.server.module.system.user.entity.SysUser;

import java.time.LocalDateTime;

public record UserResponse(Long id, String username, String email, Integer role, Integer status,
                           LocalDateTime lastLoginTime, LocalDateTime createdTime, LocalDateTime updatedTime) {

    public static UserResponse from(SysUser user) {
        return new UserResponse(user.getId(), user.getUsername(), user.getEmail(), user.getRole(), user.getStatus(),
                user.getLastLoginTime(), user.getCreatedTime(), user.getUpdatedTime());
    }
}
