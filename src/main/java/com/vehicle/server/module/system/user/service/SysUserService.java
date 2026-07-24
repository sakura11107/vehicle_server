package com.vehicle.server.module.system.user.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.vehicle.server.common.dto.PageRequest;
import com.vehicle.server.common.dto.PageResponse;
import com.vehicle.server.common.exception.BusinessException;
import com.vehicle.server.common.exception.ErrorCode;
import com.vehicle.server.common.id.SnowflakeIdGenerator;
import com.vehicle.server.infrastructure.security.UserDetailsCache;
import com.vehicle.server.module.system.user.dto.UserCreateRequest;
import com.vehicle.server.module.system.user.dto.UserListRequest;
import com.vehicle.server.module.system.user.dto.UserResponse;
import com.vehicle.server.module.system.user.dto.UserUpdateRequest;
import com.vehicle.server.module.system.user.entity.SysUser;
import com.vehicle.server.module.system.user.enums.UserRole;
import com.vehicle.server.module.system.user.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SysUserService {

    private final SysUserMapper userMapper;
    private final SnowflakeIdGenerator idGenerator;
    private final PasswordEncoder passwordEncoder;
    private final UserDetailsCache userDetailsCache;

    @Transactional
    public UserResponse create(UserCreateRequest request) {
        ensureUniqueForCreate(request.username(), request.email());
        SysUser user = new SysUser();
        user.setId(idGenerator.nextId());
        apply(user, request.username(), request.password(), request.email(), request.role(), request.status());
        userMapper.insert(user);
        return UserResponse.from(user);
    }

    @Transactional(readOnly = true)
    public PageResponse<UserResponse> list(PageRequest pageRequest, UserListRequest query) {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<SysUser>()
                .like(query.username() != null && !query.username().isBlank(),
                        SysUser::getUsername, query.username())
                .eq(query.status() != null, SysUser::getStatus, query.status())
                .orderByDesc(SysUser::getCreatedTime);
        IPage<SysUser> page = userMapper.selectPage(
                new Page<>(pageRequest.page(), pageRequest.size()),
                wrapper
        );
        return PageResponse.of(page, page.getRecords().stream().map(UserResponse::from).toList());
    }

    @Transactional(readOnly = true)
    public UserResponse getById(Long id) {
        return UserResponse.from(requireUser(id));
    }

    @Transactional(readOnly = true)
    public SysUser requireUser(Long id) {
        SysUser user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        return user;
    }

    @Transactional(readOnly = true)
    public SysUser findByUsername(String username) {
        return userMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, username));
    }

    @Transactional(readOnly = true)
    public Map<Long, SysUser> mapByIds(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Map.of();
        }
        return userMapper.selectByIds(ids).stream()
                .collect(Collectors.toMap(SysUser::getId, u -> u));
    }

    @Transactional(readOnly = true)
    public List<SysUser> listActiveManagersAndAdmins(Long excludeUserId) {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<SysUser>()
                .in(SysUser::getRole, UserRole.MANAGER.getCode(), UserRole.ADMIN.getCode())
                .eq(SysUser::getStatus, 1);
        if (excludeUserId != null) {
            wrapper.ne(SysUser::getId, excludeUserId);
        }
        return userMapper.selectList(wrapper);
    }

    @Transactional
    public UserResponse update(Long id, UserUpdateRequest request) {
        SysUser user = requireUser(id);
        String oldUsername = user.getUsername();
        if (existsByUsername(request.username(), id)) {
            throw new BusinessException(ErrorCode.USERNAME_EXISTS);
        }
        if (existsByEmail(request.email(), id)) {
            throw new BusinessException(ErrorCode.EMAIL_EXISTS);
        }
        apply(user, request.username(), request.password(), request.email(), request.role(), request.status());
        userMapper.updateById(user);
        userDetailsCache.evict(oldUsername);
        userDetailsCache.evict(user.getUsername());
        return UserResponse.from(user);
    }

    @Transactional
    public void delete(Long id) {
        SysUser user = requireUser(id);
        userMapper.deleteById(id);
        userDetailsCache.evict(user.getUsername());
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
                .eq(SysUser::getUsername, username);
        if (excludedId != null) {
            query.ne(SysUser::getId, excludedId);
        }
        return userMapper.selectCount(query) > 0;
    }

    private boolean existsByEmail(String email, Long excludedId) {
        LambdaQueryWrapper<SysUser> query = new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getEmail, email);
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
