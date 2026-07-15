package com.vehicle.server.module.system.user.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.vehicle.server.common.dto.PageRequest;
import com.vehicle.server.common.dto.PageResponse;
import com.vehicle.server.common.exception.BusinessException;
import com.vehicle.server.common.exception.ErrorCode;
import com.vehicle.server.common.id.SnowflakeIdGenerator;
import com.vehicle.server.module.system.user.dto.UserCreateRequest;
import com.vehicle.server.module.system.user.dto.UserResponse;
import com.vehicle.server.module.system.user.dto.UserUpdateRequest;
import com.vehicle.server.module.system.user.entity.SysUser;
import com.vehicle.server.module.system.user.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class SysUserService {

    private static final Integer NOT_DELETED = 0;
    private final SysUserMapper userMapper;
    private final SnowflakeIdGenerator idGenerator;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Transactional
    public UserResponse create(UserCreateRequest request) {
        ensureUniqueForCreate(request.username(), request.email());
        SysUser user = new SysUser();
        user.setId(idGenerator.nextId());
        apply(user, request.username(), request.password(), request.email(), request.role(), request.status());
        LocalDateTime now = LocalDateTime.now();
        user.setCreatedTime(now);
        user.setUpdatedTime(now);
        userMapper.insert(user);
        return UserResponse.from(user);
    }

    @Transactional(readOnly = true)
    public PageResponse<UserResponse> list(PageRequest pageRequest) {
        IPage<SysUser> page = userMapper.selectPage(
                new Page<>(pageRequest.page(), pageRequest.size()),
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getDeleted, NOT_DELETED)
        );
        return PageResponse.of(page, page.getRecords().stream().map(UserResponse::from).toList());
    }

    @Transactional(readOnly = true)
    public UserResponse getById(Long id) {
        return UserResponse.from(findActiveUser(id));
    }

    @Transactional
    public UserResponse update(Long id, UserUpdateRequest request) {
        SysUser user = findActiveUser(id);
        if (existsByUsername(request.username(), id)) {
            throw new BusinessException(ErrorCode.USERNAME_EXISTS);
        }
        if (existsByEmail(request.email(), id)) {
            throw new BusinessException(ErrorCode.EMAIL_EXISTS);
        }
        apply(user, request.username(), request.password(), request.email(), request.role(), request.status());
        user.setUpdatedTime(LocalDateTime.now());
        userMapper.updateById(user);
        return UserResponse.from(user);
    }

    @Transactional
    public void delete(Long id) {
        SysUser user = findActiveUser(id);
        user.setDeleted(1);
        user.setUpdatedTime(LocalDateTime.now());
        userMapper.updateById(user);
    }

    private SysUser findActiveUser(Long id) {
        SysUser user = userMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getId, id)
                .eq(SysUser::getDeleted, NOT_DELETED));
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        return user;
    }

    private void ensureUniqueForCreate(String username, String email) {
        if (existsByUsername(username, null)) {
            throw new BusinessException(ErrorCode.USERNAME_EXISTS);
        }
        if (existsByEmail(email, null)) {
            throw new BusinessException(ErrorCode.EMAIL_EXISTS);
        }
    }

    private boolean existsByUsername(String username, Long excludedId) {
        LambdaQueryWrapper<SysUser> query = new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, username)
                .eq(SysUser::getDeleted, NOT_DELETED);
        if (excludedId != null) {
            query.ne(SysUser::getId, excludedId);
        }
        return userMapper.selectCount(query) > 0;
    }

    private boolean existsByEmail(String email, Long excludedId) {
        LambdaQueryWrapper<SysUser> query = new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getEmail, email)
                .eq(SysUser::getDeleted, NOT_DELETED);
        if (excludedId != null) {
            query.ne(SysUser::getId, excludedId);
        }
        return userMapper.selectCount(query) > 0;
    }

    private void apply(SysUser user, String username, String password, String email, Integer role, Integer status) {
        user.setUsername(username);
        user.setEmail(email);
        user.setRole(role == null ? 0 : role);
        user.setStatus(status == null ? 1 : status);
        if (password != null && !password.isBlank()) {
            user.setPassword(passwordEncoder.encode(password));
        }
    }

}
