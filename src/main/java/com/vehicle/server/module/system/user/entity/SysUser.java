package com.vehicle.server.module.system.user.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@TableName("sys_user")
public class SysUser {

    @TableId(value = "id", type = IdType.INPUT)
    private Long id;

    private String username;

    private String password;

    private String email;

    private Integer role = 0;

    private Integer status = 1;

    private Integer deleted = 0;

    @TableField("last_login_time")
    private LocalDateTime lastLoginTime;

    @TableField("created_time")
    private LocalDateTime createdTime;

    @TableField("updated_time")
    private LocalDateTime updatedTime;
}
