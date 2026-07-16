package com.vehicle.server.module.vehicle.dto;

import com.vehicle.server.module.vehicle.entity.Vehicle;
import com.vehicle.server.module.vehicle.enums.VehicleStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 车辆信息的响应数据。
 */
public record VehicleResponse(
        Long id,
        String plateNumber,
        String brand,
        String model,
        String color,
        LocalDate purchaseDate,
        LocalDate rentStartDate,
        LocalDate rentEndDate,
        VehicleStatus status,
        String remark,
        LocalDateTime createdTime,
        LocalDateTime updatedTime) {

    public static VehicleResponse from(Vehicle vehicle) {
        return new VehicleResponse(
                vehicle.getId(),
                vehicle.getPlateNumber(),
                vehicle.getBrand(),
                vehicle.getModel(),
                vehicle.getColor(),
                vehicle.getPurchaseDate(),
                vehicle.getRentStartDate(),
                vehicle.getRentEndDate(),
                vehicle.getStatus(),
                vehicle.getRemark(),
                vehicle.getCreatedTime(),
                vehicle.getUpdatedTime()
        );
    }
}
