-- 车辆状态：0停用->1空闲；2使用中语义改为已预约；3维保中保留
UPDATE `vehicle` SET `status` = 1 WHERE `status` = 0;
ALTER TABLE `vehicle`
    MODIFY COLUMN `status` TINYINT NOT NULL DEFAULT 1 COMMENT '车辆状态：1-空闲中 2-已预约 3-维保中';

-- 软删后可重用用户名/邮箱（仅未删除行唯一）
ALTER TABLE `sys_user` DROP INDEX `uk_username`;
ALTER TABLE `sys_user` DROP INDEX `uk_email`;
ALTER TABLE `sys_user`
    ADD COLUMN `username_active` VARCHAR(50)
        GENERATED ALWAYS AS (IF(`deleted` = 0, `username`, NULL)) STORED,
    ADD COLUMN `email_active` VARCHAR(100)
        GENERATED ALWAYS AS (IF(`deleted` = 0, `email`, NULL)) STORED,
    ADD UNIQUE KEY `uk_username_active` (`username_active`),
    ADD UNIQUE KEY `uk_email_active` (`email_active`);

-- 软删后可重用车牌
ALTER TABLE `vehicle` DROP INDEX `uk_plate_number`;
ALTER TABLE `vehicle`
    ADD COLUMN `plate_number_active` VARCHAR(20)
        GENERATED ALWAYS AS (IF(`deleted` = 0, `plate_number`, NULL)) STORED,
    ADD UNIQUE KEY `uk_plate_number_active` (`plate_number_active`);
