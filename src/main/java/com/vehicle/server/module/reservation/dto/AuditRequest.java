package com.vehicle.server.module.reservation.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 审核预约的请求参数。
 */
public record AuditRequest(
        @NotNull Boolean approved,
        @Size(max = 500) String remark) {
}
