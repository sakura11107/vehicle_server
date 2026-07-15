package com.vehicle.server.module.system.auth.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 用户登录接口的请求参数。
 */
public record LoginRequest(
        @NotBlank String username,
        @NotBlank String password) {
}
