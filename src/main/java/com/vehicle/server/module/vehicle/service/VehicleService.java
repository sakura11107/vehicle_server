package com.vehicle.server.module.vehicle.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.vehicle.server.common.dto.PageRequest;
import com.vehicle.server.common.dto.PageResponse;
import com.vehicle.server.common.exception.BusinessException;
import com.vehicle.server.common.exception.ErrorCode;
import com.vehicle.server.common.id.SnowflakeIdGenerator;
import com.vehicle.server.module.reservation.entity.VehicleReservation;
import com.vehicle.server.module.reservation.enums.ReservationStatus;
import com.vehicle.server.module.reservation.mapper.VehicleReservationMapper;
import com.vehicle.server.module.vehicle.dto.VehicleCreateRequest;
import com.vehicle.server.module.vehicle.dto.VehicleListRequest;
import com.vehicle.server.module.vehicle.dto.VehicleResponse;
import com.vehicle.server.module.vehicle.dto.VehicleUpdateRequest;
import com.vehicle.server.module.vehicle.entity.Vehicle;
import com.vehicle.server.module.vehicle.mapper.VehicleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 车辆基础信息的业务服务。
 */
@Service
@RequiredArgsConstructor
public class VehicleService {

    private static final Integer NOT_DELETED = 0;
    private final VehicleMapper vehicleMapper;
    private final VehicleReservationMapper reservationMapper;
    private final SnowflakeIdGenerator idGenerator;

    @Transactional
    public VehicleResponse create(VehicleCreateRequest request) {
        ensurePlateNumberUnique(request.plateNumber(), null);
        Vehicle vehicle = new Vehicle();
        vehicle.setId(idGenerator.nextId());
        apply(vehicle, request);
        LocalDateTime now = LocalDateTime.now();
        vehicle.setCreatedTime(now);
        vehicle.setUpdatedTime(now);
        vehicle.setDeleted(NOT_DELETED);
        vehicleMapper.insert(vehicle);
        return VehicleResponse.from(vehicle);
    }

    @Transactional(readOnly = true)
    public PageResponse<VehicleResponse> list(PageRequest pageRequest, VehicleListRequest query) {
        LambdaQueryWrapper<Vehicle> wrapper = new LambdaQueryWrapper<Vehicle>()
                .eq(Vehicle::getDeleted, NOT_DELETED)
                .like(query.plateNumber() != null && !query.plateNumber().isBlank(),
                        Vehicle::getPlateNumber, query.plateNumber())
                .like(query.brand() != null && !query.brand().isBlank(),
                        Vehicle::getBrand, query.brand())
                .like(query.model() != null && !query.model().isBlank(),
                        Vehicle::getModel, query.model())
                .like(query.color() != null && !query.color().isBlank(),
                        Vehicle::getColor, query.color())
                .eq(query.status() != null, Vehicle::getStatus, query.status())
                .orderByDesc(Vehicle::getCreatedTime);
        IPage<Vehicle> page = vehicleMapper.selectPage(
                new Page<>(pageRequest.page(), pageRequest.size()),
                wrapper
        );
        return PageResponse.of(page, page.getRecords().stream().map(VehicleResponse::from).toList());
    }

    @Transactional(readOnly = true)
    public VehicleResponse getById(Long id) {
        return VehicleResponse.from(findActiveVehicle(id));
    }

    @Transactional
    public VehicleResponse update(Long id, VehicleUpdateRequest request) {
        Vehicle vehicle = findActiveVehicle(id);
        if (!vehicle.getPlateNumber().equals(request.plateNumber())) {
            ensurePlateNumberUnique(request.plateNumber(), id);
        }
        apply(vehicle, request);
        vehicle.setUpdatedTime(LocalDateTime.now());
        vehicleMapper.updateById(vehicle);
        return VehicleResponse.from(vehicle);
    }

    @Transactional
    public void delete(Long id) {
        Vehicle vehicle = findActiveVehicle(id);

        long activeCount = reservationMapper.selectCount(new LambdaQueryWrapper<VehicleReservation>()
                .eq(VehicleReservation::getVehicleId, id)
                .eq(VehicleReservation::getDeleted, 0)
                .in(VehicleReservation::getStatus,
                        ReservationStatus.APPLYING, ReservationStatus.APPROVED));
        if (activeCount > 0) {
            throw new BusinessException(ErrorCode.VEHICLE_HAS_ACTIVE_RESERVATIONS);
        }

        vehicle.setDeleted(1);
        vehicle.setUpdatedTime(LocalDateTime.now());
        vehicleMapper.updateById(vehicle);
    }

    private Vehicle findActiveVehicle(Long id) {
        Vehicle vehicle = vehicleMapper.selectById(id);
        if (vehicle == null || vehicle.getDeleted() != NOT_DELETED) {
            throw new BusinessException(ErrorCode.VEHICLE_NOT_FOUND);
        }
        return vehicle;
    }

    private void ensurePlateNumberUnique(String plateNumber, Long excludedId) {
        LambdaQueryWrapper<Vehicle> query = new LambdaQueryWrapper<Vehicle>()
                .eq(Vehicle::getPlateNumber, plateNumber)
                .eq(Vehicle::getDeleted, NOT_DELETED);
        if (excludedId != null) {
            query.ne(Vehicle::getId, excludedId);
        }
        if (vehicleMapper.selectCount(query) > 0) {
            throw new BusinessException(ErrorCode.VEHICLE_PLATE_EXISTS);
        }
    }

    private void apply(Vehicle vehicle, VehicleCreateRequest request) {
        vehicle.setPlateNumber(request.plateNumber());
        vehicle.setBrand(request.brand());
        vehicle.setModel(request.model());
        vehicle.setColor(request.color());
        vehicle.setPurchaseDate(request.purchaseDate());
        vehicle.setRentStartDate(request.rentStartDate());
        vehicle.setRentEndDate(request.rentEndDate());
        vehicle.setStatus(request.status());
        vehicle.setRemark(request.remark());
    }

    private void apply(Vehicle vehicle, VehicleUpdateRequest request) {
        vehicle.setPlateNumber(request.plateNumber());
        vehicle.setBrand(request.brand());
        vehicle.setModel(request.model());
        vehicle.setColor(request.color());
        vehicle.setPurchaseDate(request.purchaseDate());
        vehicle.setRentStartDate(request.rentStartDate());
        vehicle.setRentEndDate(request.rentEndDate());
        vehicle.setStatus(request.status());
        vehicle.setRemark(request.remark());
    }
}
