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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

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
    @PreAuthorize("hasRole('MANAGER')")
    @Operation(summary = "新增车辆", description = "需车辆管理员及以上角色")
    public ApiResponse<VehicleResponse> create(@Valid @RequestBody VehicleCreateRequest request) {
        return ApiResponse.success(vehicleService.create(request));
    }

    @GetMapping
    @Operation(summary = "查询车辆列表", description = "支持按车牌号(模糊)、品牌(模糊)、车型(模糊)、颜色(模糊)、状态(精确)筛选，支持分页")
    public ApiResponse<PageResponse<VehicleResponse>> list(@Valid PageRequest pageRequest, VehicleListRequest query) {
        return ApiResponse.success(vehicleService.list(pageRequest, query));
    }

    @GetMapping("/{id}")
    @Operation(summary = "按 ID 查询车辆")
    public ApiResponse<VehicleResponse> getById(@PathVariable Long id) {
        return ApiResponse.success(vehicleService.getById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('MANAGER')")
    @Operation(summary = "修改车辆", description = "需车辆管理员及以上角色")
    public ApiResponse<VehicleResponse> update(@PathVariable Long id, @Valid @RequestBody VehicleUpdateRequest request) {
        return ApiResponse.success(vehicleService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('MANAGER')")
    @Operation(summary = "删除车辆", description = "执行逻辑删除，不会物理删除数据库记录。需车辆管理员及以上角色")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        vehicleService.delete(id);
        return ApiResponse.success(null);
    }
}
