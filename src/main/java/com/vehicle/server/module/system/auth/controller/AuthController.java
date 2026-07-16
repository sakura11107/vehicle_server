package com.vehicle.server.module.system.auth.controller;

import com.vehicle.server.common.api.ApiResponse;
import com.vehicle.server.module.system.auth.dto.LoginRequest;
import com.vehicle.server.module.system.auth.dto.LoginResponse;
import com.vehicle.server.module.system.auth.dto.RegisterRequest;
import com.vehicle.server.module.system.auth.service.AuthService;
import com.vehicle.server.module.system.user.dto.UserResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "认证", description = "用户注册和登录")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "用户注册", description = "新注册用户默认角色为普通用户")
    public ApiResponse<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ApiResponse.success(authService.register(request));
    }

    @PostMapping("/login")
    @Operation(summary = "用户登录", description = "校验用户名密码，返回 JWT Token")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.success(authService.login(request));
    }
}
