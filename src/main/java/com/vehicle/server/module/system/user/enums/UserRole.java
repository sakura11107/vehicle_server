package com.vehicle.server.module.system.user.enums;

/**
 * 系统用户角色，对应 sys_user.role 字段。
 */
public enum UserRole {

    USER(0, "ROLE_USER", "普通用户"),
    MANAGER(1, "ROLE_MANAGER", "车辆管理员"),
    ADMIN(2, "ROLE_ADMIN", "系统管理员");

    private final int code;

    private final String authority;

    private final String description;

    UserRole(int code, String authority, String description) {
        this.code = code;
        this.authority = authority;
        this.description = description;
    }

    public int getCode() {
        return code;
    }

    public String getAuthority() {
        return authority;
    }

    public String getDescription() {
        return description;
    }

    /**
     * 按数据库存储的 code 解析角色，未知或空值按普通用户处理。
     */
    public static UserRole fromCode(Integer code) {
        if (code != null) {
            for (UserRole role : values()) {
                if (role.code == code) {
                    return role;
                }
            }
        }
        return USER;
    }
}
