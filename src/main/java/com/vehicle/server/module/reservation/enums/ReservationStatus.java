package com.vehicle.server.module.reservation.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 预约状态枚举。
 */
public enum ReservationStatus {

    APPLYING(0, "申请中"),
    APPROVED(1, "已审核"),
    CANCELLED(2, "已取消"),
    RETURNED(4, "已还车"),
    REJECTED(5, "已拒绝");

    @EnumValue
    @JsonValue
    private final int code;

    private final String description;

    ReservationStatus(int code, String description) {
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
