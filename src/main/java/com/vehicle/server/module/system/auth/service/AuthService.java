package com.vehicle.server.module.system.auth.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.vehicle.server.common.exception.BusinessException;
import com.vehicle.server.common.exception.ErrorCode;
import com.vehicle.server.common.id.SnowflakeIdGenerator;
import com.vehicle.server.infrastructure.security.JwtUtil;
import com.vehicle.server.infrastructure.security.SecurityUtils;
import com.vehicle.server.module.system.auth.dto.LoginRequest;
import com.vehicle.server.module.system.auth.dto.LoginResponse;
import com.vehicle.server.module.system.auth.dto.ProfileUpdateRequest;
import com.vehicle.server.module.system.auth.dto.RegisterRequest;
import com.vehicle.server.module.system.user.dto.UserResponse;
import com.vehicle.server.module.system.user.entity.SysUser;
import com.vehicle.server.module.system.user.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final SysUserMapper userMapper;
    private final SnowflakeIdGenerator idGenerator;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Transactional
    public UserResponse register(RegisterRequest request) {
        if (userMapper.selectCount(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, request.username())) > 0) {
            throw new BusinessException(ErrorCode.USERNAME_EXISTS);
        }
        if (userMapper.selectCount(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getEmail, request.email())) > 0) {
            throw new BusinessException(ErrorCode.EMAIL_EXISTS);
        }

        SysUser user = new SysUser();
        user.setId(idGenerator.nextId());
        user.setUsername(request.username());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setEmail(request.email());
        user.setRole(0);
        user.setStatus(1);
        userMapper.insert(user);
        return UserResponse.from(user);
    }

    @Transactional
    public LoginResponse login(LoginRequest request) {
        SysUser user = userMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, request.username()));
        if (user == null || !passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }
        if (user.getStatus() == 0) {
            throw new BusinessException(ErrorCode.USER_DISABLED);
        }

        user.setLastLoginTime(java.time.LocalDateTime.now());
        userMapper.updateById(user);

        String token = jwtUtil.generateToken(user.getId(), user.getUsername());
        return new LoginResponse(token, UserResponse.from(user));
    }

    public UserResponse getCurrentUser() {
        Long userId = SecurityUtils.getCurrentUserId();
        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        return UserResponse.from(user);
    }

    @Transactional
    public UserResponse updateCurrentUser(ProfileUpdateRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        if (!passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
            throw new BusinessException(ErrorCode.CURRENT_PASSWORD_INCORRECT);
        }

        if (!user.getUsername().equals(request.username())
                && userMapper.selectCount(new LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getUsername, request.username())) > 0) {
            throw new BusinessException(ErrorCode.USERNAME_EXISTS);
        }

        if (!user.getEmail().equals(request.email())
                && userMapper.selectCount(new LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getEmail, request.email())) > 0) {
            throw new BusinessException(ErrorCode.EMAIL_EXISTS);
        }

        user.setUsername(request.username());
        user.setEmail(request.email());
        if (request.password() != null && !request.password().isBlank()) {
            user.setPassword(passwordEncoder.encode(request.password()));
        }
        userMapper.updateById(user);
        return UserResponse.from(user);
    }
}
