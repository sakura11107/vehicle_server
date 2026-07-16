package com.vehicle.server.module.reservation.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

/**
 * 创建预约的请求参数。
 */
public record ReservationCreateRequest(
        @NotNull Long vehicleId,
        @NotNull @Future LocalDateTime startTime,
        @NotNull @Future LocalDateTime endTime,
        @NotBlank String purpose) {
}
