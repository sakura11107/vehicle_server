package com.vehicle.server.module.system.user.controller;

import com.vehicle.server.common.api.ApiResponse;
import com.vehicle.server.module.system.user.dto.UserCreateRequest;
import com.vehicle.server.module.system.user.dto.UserResponse;
import com.vehicle.server.module.system.user.dto.UserUpdateRequest;
import com.vehicle.server.module.system.user.service.SysUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "用户管理", description = "系统用户的新增、查询、修改和逻辑删除")
public class SysUserController {

    private final SysUserService userService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "新增用户")
    public ApiResponse<UserResponse> create(@Valid @RequestBody UserCreateRequest request) {
        return ApiResponse.success(userService.create(request));
    }

    @GetMapping
    @Operation(summary = "查询用户列表", description = "仅返回未逻辑删除的用户")
    public ApiResponse<List<UserResponse>> list() {
        return ApiResponse.success(userService.list());
    }

    @GetMapping("/{id}")
    @Operation(summary = "按 ID 查询用户")
    public ApiResponse<UserResponse> getById(@PathVariable Long id) {
        return ApiResponse.success(userService.getById(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "修改用户", description = "密码为空时不更新密码")
    public ApiResponse<UserResponse> update(@PathVariable Long id, @Valid @RequestBody UserUpdateRequest request) {
        return ApiResponse.success(userService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除用户", description = "执行逻辑删除，不会物理删除数据库记录")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        userService.delete(id);
        return ApiResponse.success(null);
    }
}
