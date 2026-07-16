package com.vehicle.server.module.vehicle.dto;

import com.vehicle.server.module.vehicle.enums.VehicleStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * 修改车辆的请求参数。
 */
public record VehicleUpdateRequest(
        @NotBlank @Size(max = 20) String plateNumber,
        @Size(max = 50) String brand,
        @Size(max = 50) String model,
        @Size(max = 20) String color,
        LocalDate purchaseDate,
        LocalDate rentStartDate,
        LocalDate rentEndDate,
        @NotNull VehicleStatus status,
        @Size(max = 500) String remark) {
}
