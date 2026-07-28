package com.vehicle.server.module.system.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 用户修改个人信息接口的请求参数。
 */
public record ProfileUpdateRequest(
        @NotBlank @Size(max = 50) String username,
        @NotBlank @Email @Size(max = 100) String email,
        @Size(min = 6, max = 100) String password,
        @NotBlank String currentPassword) {
}
