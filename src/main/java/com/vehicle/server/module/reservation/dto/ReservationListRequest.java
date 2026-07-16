package com.vehicle.server.module.reservation.dto;

/**
 * 预约列表的查询条件。
 */
public record ReservationListRequest(
        Long vehicleId,
        Long userId,
        Integer status) {
}
