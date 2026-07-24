package com.vehicle.server.module.reservation.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.vehicle.server.module.reservation.enums.ReservationStatus;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@TableName("vehicle_reservation")
public class VehicleReservation {

    @TableId(value = "id", type = IdType.INPUT)
    private Long id;

    @TableField("vehicle_id")
    private Long vehicleId;

    @TableField("user_id")
    private Long userId;

    @TableField("start_time")
    private LocalDateTime startTime;

    @TableField("end_time")
    private LocalDateTime endTime;

    private String purpose;

    private ReservationStatus status;

    @TableField("audit_user_id")
    private Long auditUserId;

    @TableField("audit_time")
    private LocalDateTime auditTime;

    @TableField("audit_remark")
    private String auditRemark;

    @TableField("pickup_time")
    private LocalDateTime pickupTime;

    @TableField("pickup_mileage")
    private Integer pickupMileage;

    @TableField("pickup_fuel")
    private BigDecimal pickupFuel;

    @TableField("return_time")
    private LocalDateTime returnTime;

    @TableField("return_mileage")
    private Integer returnMileage;

    @TableField("return_fuel")
    private BigDecimal returnFuel;

    @TableField("return_remark")
    private String returnRemark;

    @TableField("parking_fee")
    private BigDecimal parkingFee = BigDecimal.ZERO;

    @TableField("fuel_fee")
    private BigDecimal fuelFee = BigDecimal.ZERO;

    @TableField("other_fee")
    private BigDecimal otherFee = BigDecimal.ZERO;

    @TableLogic
    @TableField(fill = FieldFill.INSERT)
    private Integer deleted;

    @TableField(value = "created_time", fill = FieldFill.INSERT)
    private LocalDateTime createdTime;

    @TableField(value = "updated_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedTime;
}
