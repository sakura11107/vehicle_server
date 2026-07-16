CREATE TABLE IF NOT EXISTS `vehicle_reservation` (
    `id` BIGINT NOT NULL COMMENT '雪花ID',
    `vehicle_id` BIGINT NOT NULL COMMENT '车辆ID',
    `user_id` BIGINT NOT NULL COMMENT '申请人ID',
    `start_time` DATETIME NOT NULL COMMENT '预计用车开始时间',
    `end_time` DATETIME NOT NULL COMMENT '预计还车时间',
    `purpose` VARCHAR(500) NOT NULL COMMENT '用车事由',
    `status` TINYINT NOT NULL DEFAULT 0 COMMENT '状态：0-申请中 1-已审核 2-已取消 3-使用中 4-已还车 5-已拒绝',
    `audit_user_id` BIGINT DEFAULT NULL COMMENT '审核人ID',
    `audit_time` DATETIME DEFAULT NULL COMMENT '审核时间',
    `audit_remark` VARCHAR(500) DEFAULT NULL COMMENT '审核备注',
    `pickup_time` DATETIME DEFAULT NULL COMMENT '实际取车时间',
    `pickup_mileage` INT DEFAULT NULL COMMENT '出车公里数',
    `pickup_fuel` DECIMAL(5,2) DEFAULT NULL COMMENT '出车油量(%)',
    `return_time` DATETIME DEFAULT NULL COMMENT '实际还车时间',
    `return_mileage` INT DEFAULT NULL COMMENT '还车公里数',
    `return_fuel` DECIMAL(5,2) DEFAULT NULL COMMENT '还车油量(%)',
    `return_remark` VARCHAR(1000) DEFAULT NULL COMMENT '还车备注',
    `parking_fee` DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '停车费',
    `fuel_fee` DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '加油费',
    `other_fee` DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '其他费用',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除 1-已删除',
    `created_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_vehicle_id` (`vehicle_id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_status` (`status`),
    KEY `idx_start_time` (`start_time`),
    KEY `idx_end_time` (`end_time`)
) ENGINE=InnoDB
DEFAULT CHARSET=utf8mb4
COLLATE=utf8mb4_unicode_ci
COMMENT='车辆预约记录表';
