package com.vehicle.server.module.system.auth.controller;

import com.vehicle.server.common.api.ApiResponse;
import com.vehicle.server.module.system.auth.dto.LoginRequest;
import com.vehicle.server.module.system.auth.dto.LoginResponse;
import com.vehicle.server.module.system.auth.dto.ProfileUpdateRequest;
import com.vehicle.server.module.system.auth.dto.RegisterRequest;
import com.vehicle.server.module.system.auth.service.AuthService;
import com.vehicle.server.module.system.user.dto.UserResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

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

    @GetMapping("/me")
    @Operation(summary = "获取当前用户信息", description = "返回当前登录用户的详细信息")
    public ApiResponse<UserResponse> getCurrentUser() {
        return ApiResponse.success(authService.getCurrentUser());
    }

    @PutMapping("/me")
    @Operation(summary = "修改当前用户信息", description = "更新当前登录用户的个人信息，修改用户名后需重新登录")
    public ApiResponse<UserResponse> updateCurrentUser(@Valid @RequestBody ProfileUpdateRequest request) {
        return ApiResponse.success(authService.updateCurrentUser(request));
    }
}
