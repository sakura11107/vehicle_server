package com.vehicle.server.module.system.auth.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.vehicle.server.common.exception.BusinessException;
import com.vehicle.server.common.exception.ErrorCode;
import com.vehicle.server.infrastructure.security.JwtUtil;
import com.vehicle.server.module.system.auth.dto.LoginRequest;
import com.vehicle.server.module.system.auth.dto.LoginResponse;
import com.vehicle.server.module.system.auth.dto.RegisterRequest;
import com.vehicle.server.module.system.user.dto.UserResponse;
import com.vehicle.server.module.system.user.entity.SysUser;
import com.vehicle.server.module.system.user.mapper.SysUserMapper;
import com.vehicle.server.common.id.SnowflakeIdGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final Integer NOT_DELETED = 0;
    private final SysUserMapper userMapper;
    private final SnowflakeIdGenerator idGenerator;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Transactional
    public UserResponse register(RegisterRequest request) {
        if (userMapper.selectCount(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, request.username())
                .eq(SysUser::getDeleted, NOT_DELETED)) > 0) {
            throw new BusinessException(ErrorCode.USERNAME_EXISTS);
        }
        if (userMapper.selectCount(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getEmail, request.email())
                .eq(SysUser::getDeleted, NOT_DELETED)) > 0) {
            throw new BusinessException(ErrorCode.EMAIL_EXISTS);
        }

        LocalDateTime now = LocalDateTime.now();
        SysUser user = new SysUser();
        user.setId(idGenerator.nextId());
        user.setUsername(request.username());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setEmail(request.email());
        user.setRole(0);
        user.setStatus(1);
        user.setDeleted(NOT_DELETED);
        user.setCreatedTime(now);
        user.setUpdatedTime(now);
        userMapper.insert(user);
        return UserResponse.from(user);
    }

    @Transactional
    public LoginResponse login(LoginRequest request) {
        SysUser user = userMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, request.username())
                .eq(SysUser::getDeleted, NOT_DELETED));
        if (user == null || !passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }
        if (user.getStatus() == 0) {
            throw new BusinessException(ErrorCode.USER_DISABLED);
        }

        LocalDateTime now = LocalDateTime.now();
        user.setLastLoginTime(now);
        user.setUpdatedTime(now);
        userMapper.updateById(user);

        String token = jwtUtil.generateToken(user.getId(), user.getUsername());
        return new LoginResponse(token, UserResponse.from(user));
    }
}
