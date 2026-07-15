package com.vehicle.server.module.vehicle.controller;

import com.vehicle.server.common.api.ApiResponse;
import com.vehicle.server.common.dto.PageRequest;
import com.vehicle.server.common.dto.PageResponse;
import com.vehicle.server.module.vehicle.dto.VehicleCreateRequest;
import com.vehicle.server.module.vehicle.dto.VehicleListRequest;
import com.vehicle.server.module.vehicle.dto.VehicleResponse;
import com.vehicle.server.module.vehicle.dto.VehicleUpdateRequest;
import com.vehicle.server.module.vehicle.service.VehicleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * 车辆基础信息的 HTTP 接口。
 */
@RestController
@RequestMapping("/api/vehicles")
@RequiredArgsConstructor
@Tag(name = "车辆管理", description = "车辆基础信息的新增、查询、修改和逻辑删除")
public class VehicleController {

    private final VehicleService vehicleService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "新增车辆")
    public VehicleResponse create(@Valid @RequestBody VehicleCreateRequest request) {
        return vehicleService.create(request);
    }

    @GetMapping
    @Operation(summary = "查询车辆列表", description = "支持按车牌号(模糊)、品牌(模糊)、车型(模糊)、颜色(模糊)、状态(精确)筛选，支持分页")
    public ApiResponse<PageResponse<VehicleResponse>> list(@Valid PageRequest pageRequest, VehicleListRequest query) {
        return ApiResponse.success(vehicleService.list(pageRequest, query));
    }

    @GetMapping("/{id}")
    @Operation(summary = "按 ID 查询车辆")
    public VehicleResponse getById(@PathVariable Long id) {
        return vehicleService.getById(id);
    }

    @PutMapping("/{id}")
    @Operation(summary = "修改车辆")
    public VehicleResponse update(@PathVariable Long id, @Valid @RequestBody VehicleUpdateRequest request) {
        return vehicleService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "删除车辆", description = "执行逻辑删除，不会物理删除数据库记录")
    public void delete(@PathVariable Long id) {
        vehicleService.delete(id);
    }
}
