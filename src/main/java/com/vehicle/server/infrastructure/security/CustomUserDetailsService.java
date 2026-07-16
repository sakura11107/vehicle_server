package com.vehicle.server.infrastructure.security;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.vehicle.server.module.system.user.entity.SysUser;
import com.vehicle.server.module.system.user.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final SysUserMapper userMapper;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        SysUser user = userMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, username)
                .eq(SysUser::getDeleted, 0));

        if (user == null) {
            throw new UsernameNotFoundException("用户不存在: " + username);
        }
        if (user.getStatus() == 0) {
            throw new UsernameNotFoundException("用户已禁用: " + username);
        }

        String role = switch (user.getRole()) {
            case 2 -> "ROLE_ADMIN";
            case 1 -> "ROLE_MANAGER";
            default -> "ROLE_USER";
        };

        return new User(
                user.getUsername(),
                user.getPassword(),
                List.of(new SimpleGrantedAuthority(role))
        );
    }
}
