package com.vehicle.server.module.system.contact.dto;

public record ContactGroupResponse(
        String groupName,
        Integer role,
        Integer userCount,
        Integer onlineCount
) {
}
