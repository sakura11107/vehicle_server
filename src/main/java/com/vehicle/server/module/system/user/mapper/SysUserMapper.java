package com.vehicle.server.module.system.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.vehicle.server.module.system.user.entity.SysUser;
import org.apache.ibatis.annotations.Mapper;

/**
 * 系统用户的数据访问层。
 *
 * <p>继承 MyBatis-Plus {@link BaseMapper}，提供 sys_user 的基础数据库操作。</p>
 */
@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {
}
