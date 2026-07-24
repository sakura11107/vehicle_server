package com.vehicle.server.module.vehicle.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 车辆实时状态。使用中由前端按预约时段判断展示。
 */
public enum VehicleStatus {

    IDLE(1, "空闲中"),
    RESERVED(2, "已预约"),
    MAINTENANCE(3, "维保中");

    @EnumValue
    @JsonValue
    private final int code;

    private final String description;

    VehicleStatus(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public int getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }
}
