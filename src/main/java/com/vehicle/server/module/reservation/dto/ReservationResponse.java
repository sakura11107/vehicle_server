package com.vehicle.server.module.reservation.dto;

import com.vehicle.server.module.reservation.entity.VehicleReservation;
import com.vehicle.server.module.reservation.enums.ReservationStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 预约信息的响应数据。
 */
public record ReservationResponse(
        Long id,
        Long vehicleId,
        Long userId,
        LocalDateTime startTime,
        LocalDateTime endTime,
        String purpose,
        ReservationStatus status,
        Long auditUserId,
        LocalDateTime auditTime,
        String auditRemark,
        LocalDateTime pickupTime,
        Integer pickupMileage,
        BigDecimal pickupFuel,
        LocalDateTime returnTime,
        Integer returnMileage,
        BigDecimal returnFuel,
        String returnRemark,
        BigDecimal parkingFee,
        BigDecimal fuelFee,
        BigDecimal otherFee,
        LocalDateTime createdTime,
        LocalDateTime updatedTime) {

    public static ReservationResponse from(VehicleReservation r) {
        return new ReservationResponse(
                r.getId(),
                r.getVehicleId(),
                r.getUserId(),
                r.getStartTime(),
                r.getEndTime(),
                r.getPurpose(),
                r.getStatus(),
                r.getAuditUserId(),
                r.getAuditTime(),
                r.getAuditRemark(),
                r.getPickupTime(),
                r.getPickupMileage(),
                r.getPickupFuel(),
                r.getReturnTime(),
                r.getReturnMileage(),
                r.getReturnFuel(),
                r.getReturnRemark(),
                r.getParkingFee(),
                r.getFuelFee(),
                r.getOtherFee(),
                r.getCreatedTime(),
                r.getUpdatedTime()
        );
    }
}
