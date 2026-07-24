package com.vehicle.server.module.reservation.service;

import com.vehicle.server.common.exception.BusinessException;
import com.vehicle.server.common.id.SnowflakeIdGenerator;
import com.vehicle.server.module.reservation.dto.ReservationCreateRequest;
import com.vehicle.server.module.reservation.mapper.VehicleReservationMapper;
import com.vehicle.server.module.system.user.service.SysUserService;
import com.vehicle.server.module.vehicle.entity.Vehicle;
import com.vehicle.server.module.vehicle.enums.VehicleStatus;
import com.vehicle.server.module.vehicle.service.VehicleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.vehicle.server.module.message.service.MessageService;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReservationServiceTest {

    @Mock
    private VehicleReservationMapper reservationMapper;
    @Mock
    private VehicleService vehicleService;
    @Mock
    private SysUserService userService;
    @Mock
    private SnowflakeIdGenerator idGenerator;
    @Mock
    private MessageService messageService;

    @InjectMocks
    private ReservationService reservationService;

    private Vehicle testVehicle;
    private LocalDateTime startTime;
    private LocalDateTime endTime;

    @BeforeEach
    void setUp() {
        testVehicle = new Vehicle();
        testVehicle.setId(1L);
        testVehicle.setPlateNumber("京A12345");
        testVehicle.setBrand("丰田");
        testVehicle.setModel("凯美瑞");
        testVehicle.setStatus(VehicleStatus.IDLE);

        startTime = LocalDateTime.now().plusDays(1);
        endTime = LocalDateTime.now().plusDays(1).plusHours(2);
    }

    @Test
    void create_车辆不存在时应该抛出异常() {
        ReservationCreateRequest request = new ReservationCreateRequest(
                1L, startTime, endTime, "测试用车");
        when(vehicleService.requireVehicle(1L)).thenThrow(new BusinessException(
                com.vehicle.server.common.exception.ErrorCode.VEHICLE_NOT_FOUND));

        assertThrows(BusinessException.class,
                () -> reservationService.create(100L, request));
    }

    @Test
    void create_车辆维保中时应该抛出异常() {
        testVehicle.setStatus(VehicleStatus.MAINTENANCE);
        ReservationCreateRequest request = new ReservationCreateRequest(
                1L, startTime, endTime, "测试用车");
        when(vehicleService.requireVehicle(1L)).thenReturn(testVehicle);

        assertThrows(BusinessException.class,
                () -> reservationService.create(100L, request));
    }
}
