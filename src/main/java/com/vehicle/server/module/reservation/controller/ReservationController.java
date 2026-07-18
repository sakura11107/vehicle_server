package com.vehicle.server.module.reservation.controller;

import com.vehicle.server.common.api.ApiResponse;
import com.vehicle.server.common.dto.PageRequest;
import com.vehicle.server.common.dto.PageResponse;
import com.vehicle.server.infrastructure.security.SecurityUtils;
import com.vehicle.server.module.reservation.dto.*;
import com.vehicle.server.module.reservation.service.ReservationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reservations")
@RequiredArgsConstructor
@Tag(name = "预约管理", description = "车辆预约的申请、审核、取消和还车")
public class ReservationController {

    private final ReservationService reservationService;

    @PostMapping
    @Operation(summary = "创建预约申请")
    public ApiResponse<ReservationResponse> create(@Valid @RequestBody ReservationCreateRequest request) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        return ApiResponse.success(reservationService.create(currentUserId, request));
    }

    @GetMapping
    @Operation(summary = "查询预约列表", description = "支持按车辆ID、用户ID、状态筛选，支持分页。普通用户强制仅看本人预约。")
    public ApiResponse<PageResponse<ReservationResponse>> list(
            @Valid PageRequest pageRequest,
            ReservationListRequest query) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        return ApiResponse.success(reservationService.list(currentUserId, pageRequest, query));
    }

    @GetMapping("/schedule")
    @Operation(summary = "查询车辆占用视图", description = "返回申请中/已通过/使用中的预约，供甘特图及预约表单冲突校验使用")
    public ApiResponse<List<VehicleScheduleItem>> schedule(
            @RequestParam(required = false) Long vehicleId) {
        return ApiResponse.success(reservationService.schedule(vehicleId));
    }

    @GetMapping("/{id}")
    @Operation(summary = "查询预约详情")
    public ApiResponse<ReservationResponse> getById(@PathVariable Long id) {
        return ApiResponse.success(reservationService.getById(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "修改预约", description = "仅申请中状态可修改")
    public ApiResponse<ReservationResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody ReservationUpdateRequest request) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        return ApiResponse.success(reservationService.update(id, currentUserId, request));
    }

    @PutMapping("/{id}/cancel")
    @Operation(summary = "取消预约", description = "仅申请人可取消，仅申请中状态可取消")
    public ApiResponse<Void> cancel(@PathVariable Long id) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        reservationService.cancel(id, currentUserId);
        return ApiResponse.success(null);
    }

    @PutMapping("/{id}/audit")
    @PreAuthorize("hasRole('MANAGER')")
    @Operation(summary = "审核预约", description = "审核通过或拒绝。需车辆管理员及以上角色，且不能审核自己的申请")
    public ApiResponse<ReservationResponse> audit(
            @PathVariable Long id,
            @Valid @RequestBody AuditRequest request) {
        Long auditorId = SecurityUtils.getCurrentUserId();
        return ApiResponse.success(reservationService.audit(id, auditorId, request));
    }

    @PutMapping("/{id}/return")
    @Operation(summary = "还车登记", description = "登记还车信息并更新车辆状态。申请人本人或车辆管理员及以上角色可操作")
    public ApiResponse<ReservationResponse> returnVehicle(
            @PathVariable Long id,
            @Valid @RequestBody ReturnRequest request) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        return ApiResponse.success(reservationService.returnVehicle(id, currentUserId, request));
    }
}
