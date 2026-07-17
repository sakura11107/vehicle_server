package com.vehicle.server.module.reservation.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.vehicle.server.common.dto.PageRequest;
import com.vehicle.server.common.dto.PageResponse;
import com.vehicle.server.common.exception.BusinessException;
import com.vehicle.server.common.exception.ErrorCode;
import com.vehicle.server.common.id.SnowflakeIdGenerator;
import com.vehicle.server.module.reservation.dto.*;
import com.vehicle.server.module.reservation.entity.VehicleReservation;
import com.vehicle.server.module.reservation.enums.ReservationStatus;
import com.vehicle.server.module.reservation.mapper.VehicleReservationMapper;
import com.vehicle.server.module.system.user.entity.SysUser;
import com.vehicle.server.module.system.user.mapper.SysUserMapper;
import com.vehicle.server.module.vehicle.entity.Vehicle;
import com.vehicle.server.module.vehicle.enums.VehicleStatus;
import com.vehicle.server.module.vehicle.mapper.VehicleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReservationService {

    private static final Integer NOT_DELETED = 0;
    private final VehicleReservationMapper reservationMapper;
    private final VehicleMapper vehicleMapper;
    private final SysUserMapper userMapper;
    private final SnowflakeIdGenerator idGenerator;

    @Transactional
    public ReservationResponse create(Long currentUserId, ReservationCreateRequest request) {
        Vehicle vehicle = findAvailableVehicle(request.vehicleId());
        LocalDateTime now = LocalDateTime.now();
        ensureNoConflict(request.vehicleId(), request.startTime(), request.endTime(), null);

        VehicleReservation reservation = new VehicleReservation();
        reservation.setId(idGenerator.nextId());
        reservation.setVehicleId(request.vehicleId());
        reservation.setUserId(currentUserId);
        reservation.setStartTime(request.startTime());
        reservation.setEndTime(request.endTime());
        reservation.setPurpose(request.purpose());
        reservation.setStatus(ReservationStatus.APPLYING);
        reservation.setDeleted(NOT_DELETED);
        reservation.setCreatedTime(now);
        reservation.setUpdatedTime(now);
        reservationMapper.insert(reservation);

        SysUser user = userMapper.selectById(currentUserId);
        return ReservationResponse.from(reservation, vehicle, user, null);
    }

    @Transactional(readOnly = true)
    public PageResponse<ReservationResponse> list(PageRequest pageRequest, ReservationListRequest query) {
        LambdaQueryWrapper<VehicleReservation> wrapper = new LambdaQueryWrapper<VehicleReservation>()
                .eq(VehicleReservation::getDeleted, NOT_DELETED)
                .eq(query.vehicleId() != null, VehicleReservation::getVehicleId, query.vehicleId())
                .eq(query.userId() != null, VehicleReservation::getUserId, query.userId())
                .eq(query.status() != null, VehicleReservation::getStatus, query.status())
                .orderByDesc(VehicleReservation::getCreatedTime);
        IPage<VehicleReservation> page = reservationMapper.selectPage(
                new Page<>(pageRequest.page(), pageRequest.size()),
                wrapper
        );

        Set<Long> vehicleIds = page.getRecords().stream()
                .map(VehicleReservation::getVehicleId).collect(Collectors.toSet());
        Set<Long> userIds = page.getRecords().stream()
                .map(VehicleReservation::getUserId).collect(Collectors.toSet());
        page.getRecords().forEach(r -> {
            if (r.getAuditUserId() != null) userIds.add(r.getAuditUserId());
        });

        Map<Long, Vehicle> vehicleMap = vehicleIds.isEmpty() ? Map.of()
                : vehicleMapper.selectBatchIds(vehicleIds).stream()
                .collect(Collectors.toMap(Vehicle::getId, v -> v));
        Map<Long, SysUser> userMap = userIds.isEmpty() ? Map.of()
                : userMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(SysUser::getId, u -> u));

        return PageResponse.of(page, page.getRecords().stream()
                .map(r -> ReservationResponse.from(r,
                        vehicleMap.get(r.getVehicleId()),
                        userMap.get(r.getUserId()),
                        userMap.get(r.getAuditUserId())))
                .toList());
    }

    @Transactional(readOnly = true)
    public ReservationResponse getById(Long id) {
        VehicleReservation reservation = findReservation(id);
        Vehicle vehicle = vehicleMapper.selectById(reservation.getVehicleId());
        SysUser user = userMapper.selectById(reservation.getUserId());
        SysUser auditUser = reservation.getAuditUserId() != null
                ? userMapper.selectById(reservation.getAuditUserId()) : null;
        return ReservationResponse.from(reservation, vehicle, user, auditUser);
    }

    @Transactional
    public ReservationResponse update(Long id, Long currentUserId, ReservationUpdateRequest request) {
        VehicleReservation reservation = findReservation(id);
        if (reservation.getStatus() != ReservationStatus.APPLYING) {
            throw new BusinessException(ErrorCode.INVALID_STATUS_TRANSITION);
        }
        if (!reservation.getUserId().equals(currentUserId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        ensureNoConflict(request.vehicleId(), request.startTime(), request.endTime(), id);
        reservation.setVehicleId(request.vehicleId());
        reservation.setStartTime(request.startTime());
        reservation.setEndTime(request.endTime());
        reservation.setPurpose(request.purpose());
        reservation.setUpdatedTime(LocalDateTime.now());
        reservationMapper.updateById(reservation);

        Vehicle vehicle = vehicleMapper.selectById(reservation.getVehicleId());
        SysUser user = userMapper.selectById(reservation.getUserId());
        return ReservationResponse.from(reservation, vehicle, user, null);
    }

    @Transactional
    public void cancel(Long id, Long currentUserId) {
        VehicleReservation reservation = findReservation(id);
        if (reservation.getStatus() != ReservationStatus.APPLYING) {
            throw new BusinessException(ErrorCode.INVALID_STATUS_TRANSITION);
        }
        if (!reservation.getUserId().equals(currentUserId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        reservation.setStatus(ReservationStatus.CANCELLED);
        reservation.setUpdatedTime(LocalDateTime.now());
        reservationMapper.updateById(reservation);
    }

    @Transactional
    public ReservationResponse audit(Long id, Long auditorId, AuditRequest request) {
        VehicleReservation reservation = findReservation(id);
        if (reservation.getStatus() != ReservationStatus.APPLYING) {
            throw new BusinessException(ErrorCode.INVALID_STATUS_TRANSITION);
        }

        reservation.setAuditUserId(auditorId);
        reservation.setAuditTime(LocalDateTime.now());
        reservation.setAuditRemark(request.remark());
        reservation.setStatus(request.approved() ? ReservationStatus.APPROVED : ReservationStatus.REJECTED);
        reservation.setUpdatedTime(LocalDateTime.now());
        reservationMapper.updateById(reservation);

        if (request.approved()) {
            Vehicle vehicle = vehicleMapper.selectById(reservation.getVehicleId());
            if (vehicle != null && vehicle.getStatus() != VehicleStatus.IN_USE) {
                vehicle.setStatus(VehicleStatus.IN_USE);
                vehicle.setUpdatedTime(LocalDateTime.now());
                vehicleMapper.updateById(vehicle);
            }
        }

        Vehicle vehicle = vehicleMapper.selectById(reservation.getVehicleId());
        SysUser user = userMapper.selectById(reservation.getUserId());
        SysUser auditUser = userMapper.selectById(auditorId);
        return ReservationResponse.from(reservation, vehicle, user, auditUser);
    }

    @Transactional
    public ReservationResponse returnVehicle(Long id, ReturnRequest request) {
        VehicleReservation reservation = findReservation(id);
        if (reservation.getStatus() != ReservationStatus.APPROVED
                && reservation.getStatus() != ReservationStatus.IN_USE) {
            throw new BusinessException(ErrorCode.INVALID_STATUS_TRANSITION);
        }

        reservation.setStatus(ReservationStatus.RETURNED);
        reservation.setReturnTime(LocalDateTime.now());
        reservation.setReturnMileage(request.returnMileage());
        reservation.setReturnFuel(request.returnFuel());
        reservation.setReturnRemark(request.returnRemark());
        reservation.setParkingFee(request.parkingFee() != null ? request.parkingFee() : java.math.BigDecimal.ZERO);
        reservation.setFuelFee(request.fuelFee() != null ? request.fuelFee() : java.math.BigDecimal.ZERO);
        reservation.setOtherFee(request.otherFee() != null ? request.otherFee() : java.math.BigDecimal.ZERO);
        reservation.setUpdatedTime(LocalDateTime.now());
        reservationMapper.updateById(reservation);

        Vehicle vehicle = vehicleMapper.selectById(reservation.getVehicleId());
        if (vehicle != null) {
            vehicle.setStatus(VehicleStatus.IDLE);
            vehicle.setUpdatedTime(LocalDateTime.now());
            vehicleMapper.updateById(vehicle);
        }

        SysUser user = userMapper.selectById(reservation.getUserId());
        return ReservationResponse.from(reservation, vehicle, user, null);
    }

    private Vehicle findAvailableVehicle(Long vehicleId) {
        Vehicle vehicle = vehicleMapper.selectById(vehicleId);
        if (vehicle == null || vehicle.getDeleted() != NOT_DELETED) {
            throw new BusinessException(ErrorCode.VEHICLE_NOT_FOUND);
        }
        if (vehicle.getStatus() == VehicleStatus.DISABLED || vehicle.getStatus() == VehicleStatus.MAINTENANCE) {
            throw new BusinessException(ErrorCode.VEHICLE_NOT_AVAILABLE);
        }
        return vehicle;
    }

    private VehicleReservation findReservation(Long id) {
        VehicleReservation reservation = reservationMapper.selectById(id);
        if (reservation == null || reservation.getDeleted() != NOT_DELETED) {
            throw new BusinessException(ErrorCode.RESERVATION_NOT_FOUND);
        }
        return reservation;
    }

    private void ensureNoConflict(Long vehicleId, LocalDateTime startTime, LocalDateTime endTime, Long excludedId) {
        LambdaQueryWrapper<VehicleReservation> conflict = new LambdaQueryWrapper<VehicleReservation>()
                .eq(VehicleReservation::getVehicleId, vehicleId)
                .eq(VehicleReservation::getDeleted, NOT_DELETED)
                .notIn(VehicleReservation::getStatus, ReservationStatus.CANCELLED, ReservationStatus.REJECTED)
                .lt(VehicleReservation::getStartTime, endTime)
                .gt(VehicleReservation::getEndTime, startTime);
        if (excludedId != null) {
            conflict.ne(VehicleReservation::getId, excludedId);
        }
        if (reservationMapper.selectCount(conflict) > 0) {
            throw new BusinessException(ErrorCode.VEHICLE_RESERVATION_CONFLICT);
        }
    }
}
