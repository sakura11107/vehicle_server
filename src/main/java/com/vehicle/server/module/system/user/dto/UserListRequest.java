package com.vehicle.server.module.system.user.dto;

/**
 * 用户列表的查询条件。
 */
public record UserListRequest(
        String username,
        Integer status) {
}
