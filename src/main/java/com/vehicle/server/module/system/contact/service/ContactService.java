package com.vehicle.server.module.system.contact.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.vehicle.server.infrastructure.websocket.WebSocketSessionManager;
import com.vehicle.server.module.system.contact.dto.ContactGroupResponse;
import com.vehicle.server.module.system.contact.dto.ContactItemResponse;
import com.vehicle.server.module.system.user.entity.SysUser;
import com.vehicle.server.module.system.user.enums.UserRole;
import com.vehicle.server.module.system.user.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ContactService {

    private final SysUserMapper userMapper;
    private final WebSocketSessionManager sessionManager;

    public List<ContactGroupResponse> getTree() {
        List<SysUser> users = userMapper.selectList(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getStatus, 1));

        Set<Long> onlineUserIds = sessionManager.getOnlineUsers();

        Map<Integer, List<SysUser>> grouped = users.stream()
                .collect(Collectors.groupingBy(SysUser::getRole));

        List<ContactGroupResponse> groups = new ArrayList<>();

        List<SysUser> admins = new ArrayList<>();
        admins.addAll(grouped.getOrDefault(2, List.of()));
        admins.addAll(grouped.getOrDefault(1, List.of()));
        if (!admins.isEmpty()) {
            int totalCount = admins.size();
            int onlineCount = (int) admins.stream()
                    .filter(u -> onlineUserIds.contains(u.getId()))
                    .count();
            groups.add(new ContactGroupResponse("管理员", 2, totalCount, onlineCount));
        }

        List<SysUser> normalUsers = grouped.getOrDefault(0, List.of());
        if (!normalUsers.isEmpty()) {
            int totalCount = normalUsers.size();
            int onlineCount = (int) normalUsers.stream()
                    .filter(u -> onlineUserIds.contains(u.getId()))
                    .count();
            groups.add(new ContactGroupResponse("普通用户", 0, totalCount, onlineCount));
        }

        return groups;
    }

    public List<ContactItemResponse> getUsersByRole(Integer role) {
        Set<Long> onlineUserIds = sessionManager.getOnlineUsers();

        List<SysUser> users;
        if (role == 2) {
            users = userMapper.selectList(new LambdaQueryWrapper<SysUser>()
                    .eq(SysUser::getStatus, 1)
                    .in(SysUser::getRole, 1, 2)
                    .orderByDesc(SysUser::getRole)
                    .orderByAsc(SysUser::getUsername));
        } else {
            users = userMapper.selectList(new LambdaQueryWrapper<SysUser>()
                    .eq(SysUser::getStatus, 1)
                    .eq(SysUser::getRole, 0)
                    .orderByAsc(SysUser::getUsername));
        }

        return users.stream()
                .map(u -> toContactItem(u, onlineUserIds))
                .toList();
    }

    public List<ContactItemResponse> search(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return List.of();
        }

        Set<Long> onlineUserIds = sessionManager.getOnlineUsers();

        List<SysUser> users = userMapper.selectList(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getStatus, 1)
                .like(SysUser::getUsername, keyword.trim())
                .orderByDesc(SysUser::getRole)
                .orderByAsc(SysUser::getUsername));

        return users.stream()
                .map(u -> toContactItem(u, onlineUserIds))
                .toList();
    }

    private ContactItemResponse toContactItem(SysUser user, Set<Long> onlineUserIds) {
        UserRole role = UserRole.fromCode(user.getRole());
        return new ContactItemResponse(
                user.getId(),
                user.getUsername(),
                user.getRole(),
                role.getDescription(),
                onlineUserIds.contains(user.getId())
        );
    }
}
