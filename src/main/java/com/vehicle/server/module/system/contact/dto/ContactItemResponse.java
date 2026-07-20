package com.vehicle.server.module.system.contact.dto;

public record ContactItemResponse(
        Long id,
        String username,
        Integer role,
        String roleName,
        Boolean online
) {
}
