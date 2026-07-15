CREATE TABLE IF NOT EXISTS `vehicle` (
    `id` BIGINT NOT NULL COMMENT '雪花ID',
    `plate_number` VARCHAR(20) NOT NULL COMMENT '车牌号',
    `brand` VARCHAR(50) DEFAULT NULL COMMENT '品牌',
    `model` VARCHAR(50) DEFAULT NULL COMMENT '车型',
    `color` VARCHAR(20) DEFAULT NULL COMMENT '颜色',
    `purchase_date` DATE DEFAULT NULL COMMENT '购买日期',
    `rent_start_date` DATE DEFAULT NULL COMMENT '租用开始日期',
    `rent_end_date` DATE DEFAULT NULL COMMENT '租用结束日期',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '车辆状态：0-停用 1-空闲 2-使用中 3-维修中',
    `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除 1-已删除',
    `created_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_plate_number` (`plate_number`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB
DEFAULT CHARSET=utf8mb4
COLLATE=utf8mb4_unicode_ci
COMMENT='车辆基础信息表';
