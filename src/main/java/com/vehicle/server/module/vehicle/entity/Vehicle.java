package com.vehicle.server.module.vehicle.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
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

    private Integer status = 1;

    private String remark;

    private Integer deleted = 0;

    @TableField("created_time")
    private LocalDateTime createdTime;

    @TableField("updated_time")
    private LocalDateTime updatedTime;
}
