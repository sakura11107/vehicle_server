package com.vehicle.server.module.vehicle.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.vehicle.server.module.vehicle.enums.VehicleStatus;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@TableName("vehicle")
public class Vehicle {

    @TableId(value = "id", type = IdType.INPUT)
    private Long id;

    @TableField("plate_number")
    private String plateNumber;

    private String brand;

    private String model;

    private String color;

    @TableField("purchase_date")
    private LocalDate purchaseDate;

    @TableField("rent_start_date")
    private LocalDate rentStartDate;

    @TableField("rent_end_date")
    private LocalDate rentEndDate;

    private VehicleStatus status = VehicleStatus.IDLE;

    private String remark;

    @TableLogic
    @TableField(fill = FieldFill.INSERT)
    private Integer deleted;

    @TableField(value = "created_time", fill = FieldFill.INSERT)
    private LocalDateTime createdTime;

    @TableField(value = "updated_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedTime;
}
