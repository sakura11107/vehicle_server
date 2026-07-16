package com.vehicle.server.infrastructure.security;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * JWT 用户详情，携带 userId。
 */
@Getter
@AllArgsConstructor
public class JwtUserDetails {
    private final Long userId;
    private final String username;
}
