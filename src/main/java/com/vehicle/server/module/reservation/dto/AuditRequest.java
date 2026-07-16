package com.vehicle.server.module.reservation.dto;

import jakarta.validation.constraints.NotNull;

/**
 * 审核预约的请求参数。
 */
public record AuditRequest(
        @NotNull Boolean approved,
        String remark) {
}
