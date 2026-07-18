-- 修复 message 表 created_time 精度：DATETIME 默认精度为0（截断到秒），
-- 改为 DATETIME(6) 保留微秒精度，与 Java LocalDateTime 精度匹配。
ALTER TABLE `message`
    MODIFY COLUMN `created_time` DATETIME(6) NOT NULL,
    MODIFY COLUMN `updated_time` DATETIME(6) NOT NULL;
