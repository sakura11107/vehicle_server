package com.vehicle.server.module.reservation.dto;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * 还车登记的请求参数。
 */
public record ReturnRequest(
        @NotNull Integer returnMileage,
        @NotNull BigDecimal returnFuel,
        BigDecimal parkingFee,
        BigDecimal fuelFee,
        BigDecimal otherFee,
        String returnRemark) {
}
