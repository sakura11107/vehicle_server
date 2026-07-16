package com.vehicle.server.module.vehicle.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 车辆状态枚举。
 */
public enum VehicleStatus {

    DISABLED(0, "停用"),
    IDLE(1, "空闲"),
    IN_USE(2, "使用中"),
    MAINTENANCE(3, "维修中");

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
