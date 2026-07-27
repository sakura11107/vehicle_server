package com.vehicle.server.module.vehicle.service;

import com.alibaba.excel.EasyExcel;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.vehicle.server.common.dto.PageRequest;
import com.vehicle.server.common.dto.PageResponse;
import com.vehicle.server.common.exception.BusinessException;
import com.vehicle.server.common.exception.ErrorCode;
import com.vehicle.server.common.id.SnowflakeIdGenerator;
import com.vehicle.server.module.reservation.service.ReservationService;
import com.vehicle.server.module.vehicle.dto.VehicleCreateRequest;
import com.vehicle.server.module.vehicle.dto.VehicleExcelDTO;
import com.vehicle.server.module.vehicle.dto.VehicleListRequest;
import com.vehicle.server.module.vehicle.dto.VehicleResponse;
import com.vehicle.server.module.vehicle.dto.VehicleUpdateRequest;
import com.vehicle.server.module.vehicle.entity.Vehicle;
import com.vehicle.server.module.vehicle.enums.VehicleStatus;
import com.vehicle.server.module.vehicle.mapper.VehicleMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class VehicleService {

    private final VehicleMapper vehicleMapper;
    private final SnowflakeIdGenerator idGenerator;
    private final ReservationService reservationService;

    public VehicleService(VehicleMapper vehicleMapper,
                          SnowflakeIdGenerator idGenerator,
                          @Lazy ReservationService reservationService) {
        this.vehicleMapper = vehicleMapper;
        this.idGenerator = idGenerator;
        this.reservationService = reservationService;
    }

    @Transactional
    public VehicleResponse create(VehicleCreateRequest request) {
        ensurePlateNumberUnique(request.plateNumber(), null);
        Vehicle vehicle = new Vehicle();
        vehicle.setId(idGenerator.nextId());
        apply(vehicle, request);
        if (vehicle.getStatus() == null) {
            vehicle.setStatus(VehicleStatus.IDLE);
        }
        vehicleMapper.insert(vehicle);
        return VehicleResponse.from(vehicle);
    }

    @Transactional(readOnly = true)
    public PageResponse<VehicleResponse> list(PageRequest pageRequest, VehicleListRequest query) {
        LambdaQueryWrapper<Vehicle> wrapper = new LambdaQueryWrapper<Vehicle>()
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
        return VehicleResponse.from(requireVehicle(id));
    }

    @Transactional(readOnly = true)
    public Vehicle requireVehicle(Long id) {
        Vehicle vehicle = vehicleMapper.selectById(id);
        if (vehicle == null) {
            throw new BusinessException(ErrorCode.VEHICLE_NOT_FOUND);
        }
        return vehicle;
    }

    @Transactional(readOnly = true)
    public Map<Long, Vehicle> mapByIds(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Map.of();
        }
        return vehicleMapper.selectByIds(ids).stream()
                .collect(Collectors.toMap(Vehicle::getId, v -> v));
    }

    /**
     * 根据是否仍有生效预约，同步车辆占用状态（维保中不自动改动）。
     */
    @Transactional
    public void syncOccupationStatus(Long vehicleId) {
        Vehicle vehicle = requireVehicle(vehicleId);
        if (vehicle.getStatus() == VehicleStatus.MAINTENANCE) {
            return;
        }
        boolean active = reservationService.hasActiveReservations(vehicleId);
        VehicleStatus target = active ? VehicleStatus.RESERVED : VehicleStatus.IDLE;
        if (vehicle.getStatus() != target) {
            vehicle.setStatus(target);
            vehicleMapper.updateById(vehicle);
        }
    }

    @Transactional
    public VehicleResponse update(Long id, VehicleUpdateRequest request) {
        Vehicle vehicle = requireVehicle(id);
        if (!vehicle.getPlateNumber().equals(request.plateNumber())) {
            ensurePlateNumberUnique(request.plateNumber(), id);
        }
        apply(vehicle, request);
        vehicleMapper.updateById(vehicle);
        return VehicleResponse.from(vehicle);
    }

    @Transactional
    public void delete(Long id) {
        requireVehicle(id);
        if (reservationService.hasActiveReservations(id)) {
            throw new BusinessException(ErrorCode.VEHICLE_HAS_ACTIVE_RESERVATIONS);
        }
        vehicleMapper.deleteById(id);
    }

    @Transactional
    public int importFromExcel(MultipartFile file) {
        List<VehicleExcelDTO> dataList;
        try {
            dataList = EasyExcel.read(file.getInputStream(), VehicleExcelDTO.class)
                    .sheet().doReadSync();
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.IMPORT_FORMAT_ERROR);
        }

        if (dataList.isEmpty()) {
            throw new BusinessException(ErrorCode.IMPORT_FORMAT_ERROR);
        }

        Set<String> plateNumbers = new HashSet<>();
        List<Vehicle> vehicles = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        for (int i = 0; i < dataList.size(); i++) {
            VehicleExcelDTO dto = dataList.get(i);
            int rowNum = i + 2;

            if (dto.getPlateNumber() == null || dto.getPlateNumber().isBlank()) {
                throw new BusinessException(ErrorCode.IMPORT_FORMAT_ERROR);
            }

            String plateNumber = dto.getPlateNumber().trim();
            if (!plateNumbers.add(plateNumber)) {
                throw new BusinessException(ErrorCode.IMPORT_DATA_DUPLICATE);
            }

            Vehicle vehicle = new Vehicle();
            vehicle.setId(idGenerator.nextId());
            vehicle.setPlateNumber(plateNumber);
            vehicle.setBrand(dto.getBrand());
            vehicle.setModel(dto.getModel());
            vehicle.setColor(dto.getColor());

            if (dto.getPurchaseDate() != null && !dto.getPurchaseDate().isBlank()) {
                try {
                    vehicle.setPurchaseDate(LocalDate.parse(dto.getPurchaseDate().trim(), formatter));
                } catch (DateTimeParseException e) {
                    throw new BusinessException(ErrorCode.IMPORT_FORMAT_ERROR);
                }
            }

            if (dto.getRentStartDate() != null && !dto.getRentStartDate().isBlank()) {
                try {
                    vehicle.setRentStartDate(LocalDate.parse(dto.getRentStartDate().trim(), formatter));
                } catch (DateTimeParseException e) {
                    throw new BusinessException(ErrorCode.IMPORT_FORMAT_ERROR);
                }
            }

            if (dto.getRentEndDate() != null && !dto.getRentEndDate().isBlank()) {
                try {
                    vehicle.setRentEndDate(LocalDate.parse(dto.getRentEndDate().trim(), formatter));
                } catch (DateTimeParseException e) {
                    throw new BusinessException(ErrorCode.IMPORT_FORMAT_ERROR);
                }
            }

            if (dto.getStatus() != null && !dto.getStatus().isBlank()) {
                vehicle.setStatus(parseStatus(dto.getStatus().trim()));
            } else {
                vehicle.setStatus(VehicleStatus.IDLE);
            }

            vehicle.setRemark(dto.getRemark());
            vehicles.add(vehicle);
        }

        for (Vehicle vehicle : vehicles) {
            ensurePlateNumberUnique(vehicle.getPlateNumber(), null);
        }

        for (Vehicle vehicle : vehicles) {
            vehicleMapper.insert(vehicle);
        }

        return vehicles.size();
    }

    private VehicleStatus parseStatus(String statusText) {
        return switch (statusText) {
            case "空闲中", "Idle", "idle", "IDLE" -> VehicleStatus.IDLE;
            case "已预约", "Reserved", "reserved", "RESERVED" -> VehicleStatus.RESERVED;
            case "维保中", "Maintenance", "maintenance", "MAINTENANCE" -> VehicleStatus.MAINTENANCE;
            default -> throw new BusinessException(ErrorCode.IMPORT_FORMAT_ERROR);
        };
    }

    public void downloadTemplate(HttpServletResponse response) {
        try {
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setCharacterEncoding("utf-8");
            String fileName = URLEncoder.encode("车辆导入模板", StandardCharsets.UTF_8).replaceAll("\\+", "%20");
            response.setHeader("Content-disposition", "attachment;filename=" + fileName + ".xlsx");

            List<VehicleExcelDTO> templateData = new ArrayList<>();
            VehicleExcelDTO example = new VehicleExcelDTO();
            example.setPlateNumber("京A12345");
            example.setBrand("丰田");
            example.setModel("卡罗拉");
            example.setColor("白色");
            example.setPurchaseDate("2024-01-01");
            example.setRentStartDate("2024-06-01");
            example.setRentEndDate("2025-06-01");
            example.setStatus("空闲中");
            example.setRemark("示例数据");
            templateData.add(example);

            EasyExcel.write(response.getOutputStream(), VehicleExcelDTO.class)
                    .sheet("车辆数据")
                    .doWrite(templateData);
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR);
        }
    }

    private void ensurePlateNumberUnique(String plateNumber, Long excludedId) {
        LambdaQueryWrapper<Vehicle> query = new LambdaQueryWrapper<Vehicle>()
                .eq(Vehicle::getPlateNumber, plateNumber);
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
