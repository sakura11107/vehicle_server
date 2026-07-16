package com.vehicle.server.module.system.auth.dto;

import com.vehicle.server.module.system.user.dto.UserResponse;

/**
 * 登录成功的响应数据，包含 Token 和用户信息。
 */
public record LoginResponse(String token, UserResponse user) {
}
