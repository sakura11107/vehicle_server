package com.vehicle.server.module.vehicle.dto;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

@Data
public class VehicleExcelDTO {

    @ExcelProperty("车牌号")
    private String plateNumber;

    @ExcelProperty("品牌")
    private String brand;

    @ExcelProperty("型号")
    private String model;

    @ExcelProperty("颜色")
    private String color;

    @ExcelProperty("购买日期")
    private String purchaseDate;

    @ExcelProperty("租赁开始日期")
    private String rentStartDate;

    @ExcelProperty("租赁结束日期")
    private String rentEndDate;

    @ExcelProperty("状态")
    private String status;

    @ExcelProperty("备注")
    private String remark;
}
