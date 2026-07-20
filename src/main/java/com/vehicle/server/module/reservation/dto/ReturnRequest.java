package com.vehicle.server.module.reservation.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

/**
 * 还车登记的请求参数。
 */
public record ReturnRequest(
        @NotNull Integer returnMileage,
        @NotNull @PositiveOrZero BigDecimal returnFuel,
        @PositiveOrZero BigDecimal parkingFee,
        @PositiveOrZero BigDecimal fuelFee,
        @PositiveOrZero BigDecimal otherFee,
        String returnRemark) {
}
