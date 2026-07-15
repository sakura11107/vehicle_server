package com.vehicle.server.module.vehicle.dto;

/**
 * 车辆列表的查询条件。
 */
public record VehicleListRequest(
        String plateNumber,
        String brand,
        String model,
        String color,
        Integer status) {
}
