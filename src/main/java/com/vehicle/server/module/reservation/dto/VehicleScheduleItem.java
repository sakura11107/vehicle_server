package com.vehicle.server.module.reservation.dto;

import java.time.LocalDateTime;

/**
 * 车辆占用视图（轻量），供甘特图和预约表单展示车辆已有预约情况。
 * 字段精简，不含费用/里程等敏感信息。
 */
public record VehicleScheduleItem(
        Long id,
        Long vehicleId,
        String vehiclePlateNumber,
        String userName,
        String purpose,
        LocalDateTime startTime,
        LocalDateTime endTime,
        int status
) {
}
