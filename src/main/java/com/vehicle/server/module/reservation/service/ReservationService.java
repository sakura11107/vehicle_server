package com.vehicle.server.module.reservation.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.vehicle.server.common.dto.PageRequest;
import com.vehicle.server.common.dto.PageResponse;
import com.vehicle.server.common.exception.BusinessException;
import com.vehicle.server.common.exception.ErrorCode;
import com.vehicle.server.common.id.SnowflakeIdGenerator;
import com.vehicle.server.infrastructure.security.SecurityUtils;
import com.vehicle.server.module.message.service.MessageService;
import com.vehicle.server.module.reservation.dto.*;
import com.vehicle.server.module.reservation.entity.VehicleReservation;
import com.vehicle.server.module.reservation.enums.ReservationStatus;
import com.vehicle.server.module.reservation.mapper.VehicleReservationMapper;
import com.vehicle.server.module.system.user.entity.SysUser;
import com.vehicle.server.module.system.user.service.SysUserService;
import com.vehicle.server.module.vehicle.entity.Vehicle;
import com.vehicle.server.module.vehicle.enums.VehicleStatus;
import com.vehicle.server.module.vehicle.service.VehicleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReservationService {

    private final VehicleReservationMapper reservationMapper;
    private final VehicleService vehicleService;
    private final SysUserService userService;
    private final SnowflakeIdGenerator idGenerator;
    private final MessageService messageService;

    @Transactional
    public ReservationResponse create(Long currentUserId, ReservationCreateRequest request) {
        if (!request.endTime().isAfter(request.startTime())) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED);
        }

        Vehicle vehicle = requireBookableVehicle(request.vehicleId());
        lockConflictingReservations(request.vehicleId(), request.startTime(), request.endTime(), null);
        ensureNoConflict(request.vehicleId(), request.startTime(), request.endTime(), null);

        VehicleReservation reservation = new VehicleReservation();
        reservation.setId(idGenerator.nextId());
        reservation.setVehicleId(request.vehicleId());
        reservation.setUserId(currentUserId);
        reservation.setStartTime(request.startTime());
        reservation.setEndTime(request.endTime());
        reservation.setPurpose(request.purpose());
        reservation.setStatus(ReservationStatus.APPLYING);
        reservationMapper.insert(reservation);

        vehicleService.syncOccupationStatus(request.vehicleId());
        vehicle = vehicleService.requireVehicle(request.vehicleId());

        SysUser user = userService.requireUser(currentUserId);
        notifyAdmins(currentUserId, reservation, vehicle);

        return ReservationResponse.from(reservation, vehicle, user, null);
    }

    @Transactional(readOnly = true)
    public boolean hasActiveReservations(Long vehicleId) {
        return reservationMapper.selectCount(new LambdaQueryWrapper<VehicleReservation>()
                .eq(VehicleReservation::getVehicleId, vehicleId)
                .in(VehicleReservation::getStatus,
                        ReservationStatus.APPLYING, ReservationStatus.APPROVED)) > 0;
    }

    private void notifyAdmins(Long applicantId, VehicleReservation reservation, Vehicle vehicle) {
        List<SysUser> admins = userService.listActiveManagersAndAdmins(applicantId);
        if (admins.isEmpty()) {
            log.warn("没有管理员用户，无法发送用车申请通知");
            return;
        }

        SysUser applicant = userService.requireUser(applicantId);
        String content = String.format("用户 %s 申请使用车辆 %s（%s），用车时间：%s ~ %s，请及时审核。",
                applicant.getUsername(),
                vehicle.getPlateNumber(),
                vehicle.getBrand() + " " + vehicle.getModel(),
                reservation.getStartTime(),
                reservation.getEndTime());

        for (SysUser admin : admins) {
            try {
                messageService.enqueue(applicantId, admin.getId(), content);
                log.info("用车申请通知已发送给管理员 {}: {}", admin.getId(), admin.getUsername());
            } catch (Exception e) {
                log.error("发送用车申请通知给管理员 {} 失败: {}", admin.getId(), e.getMessage(), e);
            }
        }
    }

    private void notifyApplicant(Long auditorId, VehicleReservation reservation, Vehicle vehicle, AuditRequest request) {
        String statusText = request.approved() ? "已通过" : "已拒绝";
        String content = String.format("您的用车申请（%s %s）%s，审核备注：%s",
                vehicle.getPlateNumber(),
                vehicle.getBrand() + " " + vehicle.getModel(),
                statusText,
                request.remark() != null ? request.remark() : "无");

        try {
            messageService.enqueue(auditorId, reservation.getUserId(), content);
            log.info("审核结果通知已发送给用户 {}: {}", reservation.getUserId(), statusText);
        } catch (Exception e) {
            log.error("发送审核结果通知给用户 {} 失败: {}", reservation.getUserId(), e.getMessage(), e);
        }
    }

    @Transactional(readOnly = true)
    public PageResponse<ReservationResponse> list(Long currentUserId, PageRequest pageRequest, ReservationListRequest query) {
        boolean isManagerOrAdmin = SecurityUtils.isManagerOrAdmin();
        Long forcedUserId = isManagerOrAdmin ? query.userId() : currentUserId;

        LambdaQueryWrapper<VehicleReservation> wrapper = new LambdaQueryWrapper<VehicleReservation>()
                .eq(query.vehicleId() != null, VehicleReservation::getVehicleId, query.vehicleId())
                .eq(forcedUserId != null, VehicleReservation::getUserId, forcedUserId)
                .eq(query.status() != null, VehicleReservation::getStatus, query.status())
                .orderByDesc(VehicleReservation::getCreatedTime);
        IPage<VehicleReservation> page = reservationMapper.selectPage(
                new Page<>(pageRequest.page(), pageRequest.size()),
                wrapper
        );

        return PageResponse.of(page, toResponses(page.getRecords()));
    }

    @Transactional(readOnly = true)
    public List<VehicleScheduleItem> schedule(Long vehicleId, LocalDateTime from, LocalDateTime to) {
        LocalDateTime rangeStart = from != null ? from : LocalDateTime.now().minusDays(7);
        LocalDateTime rangeEnd = to != null ? to : LocalDateTime.now().plusDays(30);
        if (!rangeEnd.isAfter(rangeStart)) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED);
        }

        LambdaQueryWrapper<VehicleReservation> wrapper = new LambdaQueryWrapper<VehicleReservation>()
                .in(VehicleReservation::getStatus,
                        ReservationStatus.APPLYING,
                        ReservationStatus.APPROVED)
                .eq(vehicleId != null, VehicleReservation::getVehicleId, vehicleId)
                .lt(VehicleReservation::getStartTime, rangeEnd)
                .gt(VehicleReservation::getEndTime, rangeStart)
                .orderByAsc(VehicleReservation::getStartTime);
        List<VehicleReservation> reservations = reservationMapper.selectList(wrapper);

        Set<Long> vehicleIds = reservations.stream()
                .map(VehicleReservation::getVehicleId).collect(Collectors.toSet());
        Set<Long> userIds = reservations.stream()
                .map(VehicleReservation::getUserId).collect(Collectors.toSet());

        Map<Long, Vehicle> vehicleMap = vehicleService.mapByIds(vehicleIds);
        Map<Long, SysUser> userMap = userService.mapByIds(userIds);

        return reservations.stream().map(r -> {
            Vehicle v = vehicleMap.get(r.getVehicleId());
            SysUser u = userMap.get(r.getUserId());
            return new VehicleScheduleItem(
                    r.getId(),
                    r.getVehicleId(),
                    v != null ? v.getPlateNumber() : "",
                    u != null ? u.getUsername() : "",
                    r.getPurpose(),
                    r.getStartTime(),
                    r.getEndTime(),
                    r.getStatus().getCode());
        }).toList();
    }

    @Transactional(readOnly = true)
    public ReservationResponse getById(Long id, Long currentUserId) {
        VehicleReservation reservation = findReservation(id);
        if (!SecurityUtils.isManagerOrAdmin() && !reservation.getUserId().equals(currentUserId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        return toResponse(reservation);
    }

    @Transactional
    public ReservationResponse update(Long id, Long currentUserId, ReservationUpdateRequest request) {
        if (!request.endTime().isAfter(request.startTime())) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED);
        }

        VehicleReservation reservation = findReservation(id);
        if (reservation.getStatus() != ReservationStatus.APPLYING) {
            throw new BusinessException(ErrorCode.INVALID_STATUS_TRANSITION);
        }
        if (!reservation.getUserId().equals(currentUserId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        Long oldVehicleId = reservation.getVehicleId();
        requireBookableVehicle(request.vehicleId());
        lockConflictingReservations(request.vehicleId(), request.startTime(), request.endTime(), id);
        ensureNoConflict(request.vehicleId(), request.startTime(), request.endTime(), id);
        reservation.setVehicleId(request.vehicleId());
        reservation.setStartTime(request.startTime());
        reservation.setEndTime(request.endTime());
        reservation.setPurpose(request.purpose());
        reservationMapper.updateById(reservation);

        vehicleService.syncOccupationStatus(request.vehicleId());
        if (!oldVehicleId.equals(request.vehicleId())) {
            vehicleService.syncOccupationStatus(oldVehicleId);
        }

        return toResponse(reservation);
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
        reservationMapper.updateById(reservation);
        vehicleService.syncOccupationStatus(reservation.getVehicleId());
    }

    @Transactional
    public ReservationResponse audit(Long id, Long auditorId, AuditRequest request) {
        VehicleReservation reservation = findReservation(id);
        if (reservation.getStatus() != ReservationStatus.APPLYING) {
            throw new BusinessException(ErrorCode.INVALID_STATUS_TRANSITION);
        }
        if (reservation.getUserId().equals(auditorId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        reservation.setAuditUserId(auditorId);
        reservation.setAuditTime(LocalDateTime.now());
        reservation.setAuditRemark(request.remark());
        reservation.setStatus(request.approved() ? ReservationStatus.APPROVED : ReservationStatus.REJECTED);
        reservationMapper.updateById(reservation);

        vehicleService.syncOccupationStatus(reservation.getVehicleId());

        Vehicle vehicle = vehicleService.requireVehicle(reservation.getVehicleId());
        SysUser user = userService.requireUser(reservation.getUserId());
        SysUser auditUser = userService.requireUser(auditorId);

        notifyApplicant(auditorId, reservation, vehicle, request);

        return ReservationResponse.from(reservation, vehicle, user, auditUser);
    }

    @Transactional
    public ReservationResponse returnVehicle(Long id, Long currentUserId, ReturnRequest request) {
        VehicleReservation reservation = findReservation(id);
        if (!SecurityUtils.isManagerOrAdmin() && !reservation.getUserId().equals(currentUserId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        if (reservation.getStatus() != ReservationStatus.APPROVED) {
            throw new BusinessException(ErrorCode.INVALID_STATUS_TRANSITION);
        }
        if (reservation.getPickupMileage() != null && request.returnMileage() < reservation.getPickupMileage()) {
            throw new BusinessException(ErrorCode.RETURN_MILEAGE_LESS_THAN_PICKUP);
        }

        reservation.setStatus(ReservationStatus.RETURNED);
        reservation.setReturnTime(LocalDateTime.now());
        reservation.setReturnMileage(request.returnMileage());
        reservation.setReturnFuel(request.returnFuel());
        reservation.setReturnRemark(request.returnRemark());
        reservation.setParkingFee(request.parkingFee() != null ? request.parkingFee() : java.math.BigDecimal.ZERO);
        reservation.setFuelFee(request.fuelFee() != null ? request.fuelFee() : java.math.BigDecimal.ZERO);
        reservation.setOtherFee(request.otherFee() != null ? request.otherFee() : java.math.BigDecimal.ZERO);
        reservationMapper.updateById(reservation);

        vehicleService.syncOccupationStatus(reservation.getVehicleId());

        return toResponse(reservation);
    }

    private Vehicle requireBookableVehicle(Long vehicleId) {
        Vehicle vehicle = vehicleService.requireVehicle(vehicleId);
        if (vehicle.getStatus() == VehicleStatus.MAINTENANCE) {
            throw new BusinessException(ErrorCode.VEHICLE_NOT_AVAILABLE);
        }
        return vehicle;
    }

    private VehicleReservation findReservation(Long id) {
        VehicleReservation reservation = reservationMapper.selectById(id);
        if (reservation == null) {
            throw new BusinessException(ErrorCode.RESERVATION_NOT_FOUND);
        }
        return reservation;
    }

    private void lockConflictingReservations(Long vehicleId, LocalDateTime startTime,
                                              LocalDateTime endTime, Long excludedId) {
        reservationMapper.selectList(activeConflictWrapper(vehicleId, startTime, endTime, excludedId)
                .select(VehicleReservation::getId)
                .last("FOR UPDATE"));
    }

    private void ensureNoConflict(Long vehicleId, LocalDateTime startTime, LocalDateTime endTime, Long excludedId) {
        // 仅申请中/已通过占用档期；已还车/取消/拒绝不参与冲突
        if (reservationMapper.selectCount(activeConflictWrapper(vehicleId, startTime, endTime, excludedId)) > 0) {
            throw new BusinessException(ErrorCode.VEHICLE_RESERVATION_CONFLICT);
        }
    }

    private LambdaQueryWrapper<VehicleReservation> activeConflictWrapper(
            Long vehicleId, LocalDateTime startTime, LocalDateTime endTime, Long excludedId) {
        LambdaQueryWrapper<VehicleReservation> wrapper = new LambdaQueryWrapper<VehicleReservation>()
                .eq(VehicleReservation::getVehicleId, vehicleId)
                .in(VehicleReservation::getStatus, ReservationStatus.APPLYING, ReservationStatus.APPROVED)
                .lt(VehicleReservation::getStartTime, endTime)
                .gt(VehicleReservation::getEndTime, startTime);
        if (excludedId != null) {
            wrapper.ne(VehicleReservation::getId, excludedId);
        }
        return wrapper;
    }

    private List<ReservationResponse> toResponses(List<VehicleReservation> records) {
        Set<Long> vehicleIds = records.stream()
                .map(VehicleReservation::getVehicleId).collect(Collectors.toSet());
        Set<Long> userIds = records.stream()
                .map(VehicleReservation::getUserId).collect(Collectors.toSet());
        records.forEach(r -> {
            if (r.getAuditUserId() != null) {
                userIds.add(r.getAuditUserId());
            }
        });

        Map<Long, Vehicle> vehicleMap = vehicleService.mapByIds(vehicleIds);
        Map<Long, SysUser> userMap = userService.mapByIds(userIds);

        return records.stream()
                .map(r -> ReservationResponse.from(r,
                        vehicleMap.get(r.getVehicleId()),
                        userMap.get(r.getUserId()),
                        userMap.get(r.getAuditUserId())))
                .toList();
    }

    private ReservationResponse toResponse(VehicleReservation reservation) {
        Vehicle vehicle = vehicleService.requireVehicle(reservation.getVehicleId());
        SysUser user = userService.requireUser(reservation.getUserId());
        SysUser auditUser = reservation.getAuditUserId() != null
                ? userService.requireUser(reservation.getAuditUserId()) : null;
        return ReservationResponse.from(reservation, vehicle, user, auditUser);
    }
}
