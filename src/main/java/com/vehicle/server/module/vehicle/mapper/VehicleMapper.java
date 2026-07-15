package com.vehicle.server.module.vehicle.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.vehicle.server.module.vehicle.entity.Vehicle;
import org.apache.ibatis.annotations.Mapper;

/**
 * 车辆基础信息的数据访问层。
 *
 * <p>继承 MyBatis-Plus {@link BaseMapper}，提供 vehicle 表的基础数据库操作。</p>
 */
@Mapper
public interface VehicleMapper extends BaseMapper<Vehicle> {
}
