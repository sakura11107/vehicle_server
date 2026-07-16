package com.vehicle.server.module.reservation.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.vehicle.server.module.reservation.entity.VehicleReservation;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface VehicleReservationMapper extends BaseMapper<VehicleReservation> {
}
